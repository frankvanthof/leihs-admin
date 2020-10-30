(ns leihs.admin.routes
  (:refer-clojure :exclude [str keyword])
  (:require [clj-logging-config.log4j :as logging-config]
            [leihs.core.core :refer [keyword str presence]])
  (:require
   [leihs.core.anti-csrf.back :as anti-csrf]
   [leihs.core.auth.core :as auth]
   [leihs.core.constants :as constants]
   [leihs.core.ds :as ds]
   [leihs.core.http-cache-buster2 :as cache-buster :refer [wrap-resource]]
   [leihs.core.json :as json]
   [leihs.core.json-protocol]
   [leihs.core.ring-audits :as ring-audits]
   [leihs.core.ring-exception :as ring-exception]
   [leihs.core.routes :as core-routes :refer [all-granted]]
   [leihs.core.routing.back :as routing]
   [leihs.core.routing.dispatch-content-type :as dispatch-content-type]

   [leihs.admin.auth.authorization :as authorization]
   [leihs.admin.back.html :as html]
   [leihs.admin.env :as env]
   [leihs.admin.paths :refer [path paths]]
   [leihs.admin.resources.system.authentication-system.back :as authentication-system]
   [leihs.admin.resources.system.authentication-system.groups.back :as authentication-system-groups]
   [leihs.admin.resources.system.authentication-system.users.back :as authentication-system-users]
   [leihs.admin.resources.system.authentication-systems.back :as authentication-systems]

   [leihs.admin.resources.delegations.delegation.back :as delegation]
   [leihs.admin.resources.delegations.delegation.users.back :as delegation-users]
   [leihs.admin.resources.delegations.delegation.groups.back :as delegation-groups]
   [leihs.admin.resources.delegations.back :as delegations]
   [leihs.admin.resources.group.back :as group]
   [leihs.admin.resources.group.users.back :as group-users]
   [leihs.admin.resources.groups.back :as groups]
   [leihs.admin.resources.inventory-pools.authorization :as inventory-pools-authorization :refer [http-safe-and-some-pools-lending-manger? pool-lending-manager? pool-inventory-manager?]]
   [leihs.admin.resources.inventory-pools.back :as inventory-pools]
   [leihs.admin.resources.inventory-pools.inventory-pool.entitlement-groups.back :as entitlement-groups]
   [leihs.admin.resources.inventory-pools.inventory-pool.entitlement-groups.entitlement-group.back :as entitlement-group]
   [leihs.admin.resources.inventory-pools.inventory-pool.entitlement-groups.entitlement-group.users.back :as entitlement-group-users]
   [leihs.admin.resources.inventory-pools.inventory-pool.entitlement_groups.entitlement_group.groups.back :as entitlement-group-groups]
   [leihs.admin.resources.inventory-pools.inventory-pool.groups.back :as inventory-pool-groups]
   [leihs.admin.resources.inventory-pools.inventory-pool.groups.group.roles.back :as inventory-pool-group-roles]
   [leihs.admin.resources.inventory-pools.inventory-pool.users.back :as inventory-pool-users]
   [leihs.admin.resources.inventory-pools.inventory-pool.users.user.direct-roles.back :as inventory-pool-user-direct-roles]
   [leihs.admin.resources.inventory-pools.inventory-pool.users.user.groups-roles.back :as inventory-pool-user-groups-roles]
   [leihs.admin.resources.inventory-pools.inventory-pool.users.user.roles.back :as inventory-pool-user-roles]
   [leihs.admin.resources.inventory-pools.inventory-pool.users.user.suspension.back :as inventory-pool-user-suspension]
   [leihs.admin.resources.settings.back :as settings]
   [leihs.admin.resources.status.back :as status]
   [leihs.admin.resources.system.database.audits.back :as audits]
   [leihs.admin.resources.system.system-admins.back :as system-admins]
   [leihs.admin.resources.system.system-admins.direct-users.back :as system-admin-direct-users]
   [leihs.admin.resources.system.system-admins.groups.back :as system-admin-groups]
   [leihs.admin.resources.user.back :as user]
   [leihs.admin.resources.users.back :as users]

   [bidi.bidi :as bidi]
   [bidi.ring :refer [make-handler]]
   [compojure.core :as cpj]
   [ring.middleware.accept]
   [ring.middleware.content-type :refer [wrap-content-type]]
   [ring.middleware.cookies]
   [ring.middleware.json]
   [ring.middleware.params]
   [ring.util.response :refer [redirect]]

   [clojure.tools.logging :as logging]
   [logbug.catcher :as catcher]
   [logbug.debug :as debug :refer [I>]]
   [logbug.ring :refer [wrap-handler-with-logging]]
   [logbug.thrown :as thrown]))

(declare redirect-to-root-handler)

(def skip-authorization-handler-keys
  (clojure.set/union
    core-routes/skip-authorization-handler-keys
    #{:home}))

(def no-spa-handler-keys
  (clojure.set/union
    core-routes/no-spa-handler-keys
    #{:redirect-to-root
      :not-found}))


(def admin-scopes?
  (authorization/scope-authorizer
    {:scope_admin_read true
     :scope_admin_write true
     :scope_system_admin_read false
     :scope_system_admin_write false}))

(def system-admin-scopes?
  (authorization/scope-authorizer
    {:scope_admin_read true
     :scope_admin_write true
     :scope_system_admin_read true
     :scope_system_admin_write true}))


(def resolve-table
  (merge core-routes/resolve-table
         {:authentication-system {:handler authentication-system/routes :authorizers [system-admin-scopes?]}
          :authentication-system-group {:handler authentication-system-groups/routes  :authorizers [admin-scopes?]}
          :authentication-system-groups {:handler authentication-system-groups/routes :authorizers [admin-scopes?]}
          :authentication-system-user {:handler authentication-system-users/routes :authorizers [admin-scopes?]}
          :authentication-system-users {:handler authentication-system-users/routes :authorizers [admin-scopes?]}
          :authentication-systems {:handler authentication-systems/routes :authorizers [system-admin-scopes?]}
          :database-audits-before {:handler audits/routes :authorizers [system-admin-scopes?]}
          :database-audits-download {:handler audits/routes :authorizers [system-admin-scopes?]}
          :delegation {:handler delegation/routes :authorizers [admin-scopes?]}
          :delegation-add-choose-responsible-user {:handler delegation/routes :authorizers [admin-scopes?]}
          :delegation-edit-choose-responsible-user {:handler delegation/routes :authorizers [admin-scopes?]}
          :delegation-group {:handler delegation-groups/routes :authorizers [admin-scopes?]}
          :delegation-groups {:handler delegation-groups/routes :authorizers [admin-scopes?]}
          :delegation-user {:handler delegation-users/routes :authorizers [admin-scopes?]}
          :delegation-users {:handler delegation-users/routes :authorizers [admin-scopes?]}
          :delegations {:handler delegations/routes :authorizers [admin-scopes?]}
          :group {:handler group/routes :authorizers [admin-scopes? http-safe-and-some-pools-lending-manger?]}
          :group-inventory-pools-roles {:handler group/routes :authorizers [admin-scopes?]}
          :group-user {:handler group-users/routes :authorizers [admin-scopes?]}
          :group-users {:handler group-users/routes :authorizers [admin-scopes?]}
          :groups {:handler groups/routes :authorizers [admin-scopes? http-safe-and-some-pools-lending-manger?]}
          :inventory-pool {:handler inventory-pools/routes :authorizers [admin-scopes? pool-lending-manager?]}
          :inventory-pool-entitlement-group {:handler entitlement-group/routes :authorizers [admin-scopes? pool-inventory-manager?]}
          :inventory-pool-entitlement-group-direct-user {:handler entitlement-group-users/routes :authorizers [admin-scopes? pool-inventory-manager?]}
          :inventory-pool-entitlement-group-groups {:handler entitlement-group-groups/routes :authorizers [admin-scopes? pool-inventory-manager?]}
          :inventory-pool-entitlement-group-group {:handler entitlement-group-groups/routes :authorizers [admin-scopes? pool-inventory-manager?]}
          :inventory-pool-entitlement-group-users {:handler entitlement-group-users/routes :authorizers [admin-scopes? pool-inventory-manager?]}
          :inventory-pool-entitlement-groups {:handler entitlement-groups/routes :authorizers [admin-scopes? pool-inventory-manager?]}
          :inventory-pool-entitlement-groups-group {:handler entitlement-groups/routes :authorizers [admin-scopes? pool-inventory-manager?]}
          :inventory-pool-group-roles {:handler inventory-pool-group-roles/routes :authorizers [admin-scopes? pool-lending-manager?]}
          :inventory-pool-groups {:handler inventory-pool-groups/routes :authorizers [admin-scopes? pool-lending-manager?]}
          :inventory-pool-user-direct-roles {:handler inventory-pool-user-direct-roles/routes :authorizers [admin-scopes? pool-lending-manager?]}
          :inventory-pool-user-groups-roles {:handler inventory-pool-user-groups-roles/routes :authorizers [admin-scopes? pool-lending-manager?]}
          :inventory-pool-user-roles {:handler inventory-pool-user-roles/routes :authorizers [admin-scopes? pool-lending-manager?]}
          :inventory-pool-user-suspension {:handler inventory-pool-user-suspension/routes :authorizers [admin-scopes? pool-lending-manager?]}
          :inventory-pool-users {:handler inventory-pool-users/routes :authorizers [admin-scopes? pool-lending-manager?]}
          :inventory-pools {:handler inventory-pools/routes :authorizers [admin-scopes? http-safe-and-some-pools-lending-manger?]}
          :not-found {:handler html/not-found-handler :authorizers [all-granted]}
          :redirect-to-root {:handler redirect-to-root-handler :authorizers [all-granted]}
          :status {:handler status/routes :authorizers [all-granted]}
          :system-admin-direct-users {:handler system-admin-direct-users/routes :authorizers [admin-scopes?]}
          :system-admin-groups {:handler system-admin-groups/routes :authorizers [admin-scopes?]}
          :system-admins {:handler system-admins/routes :authorizers [admin-scopes?]}
          :system-admins-direct-user {:handler system-admin-direct-users/routes :authorizers [admin-scopes?]}
          :system-admins-group {:handler system-admin-groups/routes :authorizers [admin-scopes?]}
          :user {:handler user/routes :authorizers [admin-scopes? http-safe-and-some-pools-lending-manger?]}
          :user-inventory-pools-roles {:handler user/routes :authorizers [admin-scopes?]}
          :user-transfer-data {:handler user/routes :authorizers [admin-scopes?]}
          :users {:handler users/routes :authorizers [admin-scopes? http-safe-and-some-pools-lending-manger?]}
          }))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn redirect-to-root-handler [request]
  (redirect (path :root)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn browser-request-matches-javascript? [request]
  "Returns true if the accepted type is javascript or
  if the :uri ends with .js. Note that browsers do not
  use the proper accept type for javascript script tags."
  (boolean (or (= (-> request :accept :mime) :javascript)
               (re-find #".+\.js$" (or (-> request :uri presence) "")))))

(defn wrap-dispatch-content-type
  ([handler]
   (fn [request]
     (wrap-dispatch-content-type handler request)))
  ([handler request]
   (cond
     ; accept json always goes to the backend handlers, i.e. the normal routing
     (= (-> request :accept :mime) :json) (or (handler request)
                                              (throw (ex-info "This resource does not provide a json response."
                                                              {:status 406})))
     ; accept HTML and GET (or HEAD) wants allmost always the frontend
     (and (= (-> request :accept :mime) :html)
          (#{:get :head} (:request-method request))
          (not (no-spa-handler-keys (:handler-key request)))
          (not (browser-request-matches-javascript? request))
          ) (html/html-handler request)
     ; other request might need to go the backend and return frontend nevertheless
     :else (let [response (handler request)]
             (if (and (nil? response)
                      ; TODO we might not need the following after we check (?nil response)
                      (not (no-spa-handler-keys (:handler-key request)))
                      (not (#{:post :put :patch :delete} (:request-method request)))
                      (= (-> request :accept :mime) :html)
                      (not (browser-request-matches-javascript? request)))
               (html/html-handler request)
               response)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;



(defn canonicalize-params-map [params & {:keys [parse-json?]
                                         :or {parse-json? true}}]
  (if-not (map? params)
    params
    (->> params
         (map (fn [[k v]]
                [(keyword k)
                 (if parse-json? (json/try-parse-json v) v)]))
         (into {}))))


(defn wrap-empty [handler]
  (fn [request]
    (or (handler request)
        {:status 404})))

(defn init [secret]
  (routing/init paths resolve-table)
  (I> wrap-handler-with-logging
      routing/dispatch-to-handler
      (authorization/wrap resolve-table)
      wrap-dispatch-content-type
      ring-audits/wrap
      anti-csrf/wrap
      auth/wrap-authenticate
      ring.middleware.cookies/wrap-cookies
      wrap-empty
      settings/wrap
      ds/wrap-tx
      status/wrap
      ring.middleware.json/wrap-json-response
      (ring.middleware.json/wrap-json-body {:keywords? true})
      dispatch-content-type/wrap-accept
      routing/wrap-add-vary-header
      routing/wrap-resolve-handler
      routing/wrap-canonicalize-params-maps
      ring.middleware.params/wrap-params
      wrap-content-type
      (wrap-resource
        "public" {:allow-symlinks? true
                  :cache-bust-paths ["/admin/css/site.css"
                                     "/admin/css/site.min.css"
                                     "/admin/js/app.js"]
                  :never-expire-paths [#".*fontawesome-[^\/]*\d+\.\d+\.\d+\/.*"
                                       #".+_[0-9a-f]{40}\..+"]
                  :enabled? true})
      ring-exception/wrap))

;#### debug ###################################################################
;(logging-config/set-logger! :level :debug)
;(logging-config/set-logger! :level :info)
(debug/debug-ns *ns*)
