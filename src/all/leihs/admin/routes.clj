(ns leihs.admin.routes
  (:refer-clojure :exclude [str keyword])
  (:require [leihs.admin.utils.core :refer [keyword str presence]])
  (:require
    [leihs.admin.anti-csrf.core :as anti-csrf]
    [leihs.admin.back.html :as html]
    [leihs.admin.constants :as constants]
    [leihs.admin.env :as env]
    [leihs.admin.paths :refer [path paths]]
    [leihs.admin.resources.api-token.back :as api-token]
    [leihs.admin.resources.api-tokens.back :as api-tokens]
    [leihs.admin.resources.auth.core :as auth]
    [leihs.admin.resources.initial-admin.core :as initial-admin]
    [leihs.admin.resources.user.back :as user]
    [leihs.admin.resources.users.back :as users]
    [leihs.admin.utils.ds :as ds]
    [leihs.admin.utils.http-resources-cache-buster :as cache-buster :refer [wrap-resource]]
    [leihs.admin.utils.json-protocol]
    [leihs.admin.utils.ring-exception :as ring-exception]

    [bidi.bidi :as bidi]
    [bidi.ring :refer [make-handler]]
    [cheshire.core :as json]
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
    [logbug.thrown :as thrown]
    ))

(declare redirect-to-root-handler)

(def handler-resolve-table
  {
   :admin html/not-found-handler
   :api-token api-token/routes
   :api-token-delete html/html-handler
   :api-token-edit html/html-handler
   :api-token-new html/html-handler
   :api-tokens api-tokens/routes
   :auth auth/routes
   :auth-password-sign-in auth/routes
   :auth-sign-out auth/routes
   :borrow html/html-handler
   :debug html/html-handler
   :initial-admin initial-admin/routes
   :leihs html/html-handler
   :manage html/html-handler
   :not-found html/not-found-handler
   :procure html/html-handler
   :redirect-to-root redirect-to-root-handler
   :request html/html-handler
   :requests html/html-handler
   :user user/routes
   :user-delete html/html-handler
   :user-edit html/html-handler
   :user-new html/html-handler
   :user-transfer-data user/routes
   :users users/routes
   })

(def do-not-dispatch-to-std-frontend-handler-keys
  #{:redirect-to-root :not-found})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn redirect-to-root-handler [request]
  (redirect (path :root)))

(defn handler-resolver [handler-key]
  (get handler-resolve-table handler-key nil))

(defn dispatch-to-handler [request]
  (when-let [handler (:handler request)]
    (handler request)))

(defn wrap-assert-handler
  ([handler]
   (fn [request]
     (wrap-assert-handler handler request)))
  ([handler request]
   (if (:handler request)
     (handler request)
     (throw
       (ex-info "Implementation error: the handler for this request could not be resolved."
                (assoc request :status 500))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn wrap-dispatch-to-html
  ([handler]
   (fn [request]
     (wrap-dispatch-to-html handler request)))
  ([handler request]
   (if (and (not (do-not-dispatch-to-std-frontend-handler-keys
                   (:handler-key request)))
            (not (#{:post :put :patch :delete} (:request-method request)))
            (= (-> request :accept :mime) :html))
     (html/html-handler request)
     (handler request))))

(defn wrap-resolve-handler
  ([handler]
   (fn [request]
     (wrap-resolve-handler handler request)))
  ([handler request]
   (let [path (or (-> request :path-info presence)
                  (-> request :uri presence))
         {route-params :route-params
          handler-key :handler} (bidi/match-pair paths {:remainder path
                                                        :route paths})
         handler-fn (handler-resolver handler-key)]
     (handler (assoc request
                     :route-params route-params
                     :handler-key handler-key
                     :handler handler-fn)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn wrap-accept [handler]
  (ring.middleware.accept/wrap-accept
    handler
    {:mime
     ["application/json-roa+json" :qs 1 :as :json-roa
      "application/json" :qs 1 :as :json
      "text/html" :qs 1 :as :html ]}))

(defn canonicalize-params-map [params]
  (if-not (map? params)
    params
    (->> params
         (map (fn [[k v]]
                [(keyword k)
                 (try (json/parse-string v true)
                      (catch Exception _ v))]))
         (into {}))))

(defn wrap-canonicalize-params-maps [handler]
  (fn [request]
    (handler (-> request
                 (update-in [:params] canonicalize-params-map)
                 (update-in [:query-params] canonicalize-params-map)
                 (update-in [:form-params] canonicalize-params-map)))))

(defn wrap-empty [handler]
  (fn [request]
    (or (handler request)
        {:status 404})))

(defn wrap-secret-byte-array
  "Adds the secret into the request as a byte-array (to prevent
  visibility in logs etc) under the :secret-byte-array key."
  [handler secret]
  (fn [request]
    (handler (assoc request :secret-ba (.getBytes secret)))))

(defn init [secret]
  (I> wrap-handler-with-logging
      dispatch-to-handler
      wrap-dispatch-to-html
      wrap-assert-handler
      wrap-resolve-handler
      wrap-accept
      ring.middleware.json/wrap-json-response
      (ring.middleware.json/wrap-json-body {:keywords? true})
      anti-csrf/wrap
      auth/wrap-authenticate
      ring.middleware.cookies/wrap-cookies
      wrap-empty
      ring-exception/wrap
      (wrap-secret-byte-array secret)
      ds/wrap-tx
      wrap-canonicalize-params-maps
      ring.middleware.params/wrap-params
      (wrap-resource
        "public" {:allow-symlinks? true
                  :cache-bust-paths ["/admin/css/site.css"
                                     "/admin/css/site.min.css"
                                     "/admin/js/app.js"]
                  :never-expire-paths [#".*font-awesome-[^\/]*\d\.\d\.\d\/.*"
                                       #".+_[0-9a-f]{40}\..+"]
                  :enabled? (= env/env :prod)})
      wrap-content-type
      ring-exception/wrap))

;#### debug ###################################################################
;(logging-config/set-logger! :level :debug)
;(logging-config/set-logger! :level :info)
;(debug/debug-ns 'cider-ci.utils.shutdown)
;(debug/debug-ns *ns*)
