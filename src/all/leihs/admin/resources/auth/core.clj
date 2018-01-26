(ns leihs.admin.resources.auth.core
  (:refer-clojure :exclude [str keyword])
  (:require [leihs.admin.utils.core :refer [keyword str presence]])
  (:require
    [leihs.admin.constants :refer [USER_SESSION_COOKIE_NAME]]
    [leihs.admin.paths :refer [path]]
    [leihs.admin.utils.sql :as sql]
    [leihs.admin.resources.auth.back.session :as session]
    [leihs.admin.resources.auth.back.token :as token]

    [ring.util.response :refer [redirect]]
    [cider-ci.open-session.encryptor :as encryptor]
    [clojure.java.jdbc :as jdbc]
    [clojure.set :refer [rename-keys]]
    [compojure.core :as cpj]
    [pandect.core]

    [clj-logging-config.log4j :as logging-config]
    [clojure.tools.logging :as logging]
    [logbug.debug :as debug]
    [logbug.catcher :as catcher]
    )
  )

(defn pw-matches-clause [pw]
  (sql/call
    := :users.pw_hash
    (sql/call :crypt pw :users.pw_hash)))

(defn password-sign-in-query [email password secret]
  (-> (sql/select :users.id :is_admin :sign_in_enabled :firstname :lastname :email)
      (sql/from :users)
      (sql/merge-join :settings [:= :settings.id 0])
      (sql/merge-where [:or
                        (pw-matches-clause password)
                        [:and
                         [:= :settings.accept_server_secret_as_universal_password true]
                         [:= password secret] ]])
      (sql/merge-where [:= (sql/call :lower :users.email) (sql/call :lower email)])
      (sql/merge-where [:= :users.sign_in_enabled true])
      (sql/merge-where [:= :users.password_sign_in_enabled true])
      sql/format))

(defn password-sign-in
  ([{{email :email password :password url :url} :form-params tx :tx sba :secret-ba}]
   (password-sign-in email password url (String. sba) tx))
  ([email password url secret tx]
   (if-let [user (->> (password-sign-in-query email password secret)
                      (jdbc/query tx) first)]
     (session/create-user-session
       user secret (redirect (or (-> url presence)
                                 (path :admin)) :see-other) tx)
     (redirect (path :admin {} {:sign-in-warning true})
               :see-other))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn sign-out [request]
  (-> (redirect (or (-> request :form-params :url presence)
                    (path :admin)) :see-other)
      (assoc-in [:cookies (str USER_SESSION_COOKIE_NAME)]
                {:value ""
                 :http-only true
                 :max-age -1
                 :path "/"
                 :secure false})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn get-auth [request]
  (when-let [auth-ent (:authenticated-entity request)]
    {:body auth-ent}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def routes
  (cpj/routes
    (cpj/GET (path :auth) [] #'get-auth)
    (cpj/POST (path :auth-password-sign-in) [] #'password-sign-in)
    (cpj/POST (path :auth-sign-out) [] #'sign-out)))

(defn wrap-authenticate [handler]
  (-> handler
      session/wrap
      token/wrap))

;#### debug ###################################################################
;(logging-config/set-logger! :level :debug)
;(logging-config/set-logger! :level :info)
;(debug/debug-ns 'cider-ci.utils.shutdown)
;(debug/debug-ns *ns*)
