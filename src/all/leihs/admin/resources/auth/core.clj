(ns leihs.admin.resources.auth.core
  (:refer-clojure :exclude [str keyword])
  (:require [leihs.admin.utils.core :refer [keyword str presence]])
  (:require
    [leihs.admin.constants :refer [USER_SESSION_COOKIE_NAME]]
    [leihs.admin.paths :refer [path]]
    [leihs.admin.utils.sql :as sql]
    [leihs.admin.resources.auth.back.session :as session]
    [leihs.admin.resources.auth.back.token :as token]

    [cider-ci.open-session.encryptor :as encryptor]
    [clojure.java.jdbc :as jdbc]
    [clojure.set :refer [rename-keys]]
    [clojure.walk]
    [compojure.core :as cpj]
    [pandect.core]
    [ring.util.response :refer [redirect]]

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
      (sql/merge-join :system_settings [:= :system_settings.id 0])
      (sql/merge-where [:or
                        (pw-matches-clause password)
                        [:and
                         [:= :system_settings.accept_server_secret_as_universal_password true]
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

(defn shib-params->user-params [m]
  (let [m (clojure.walk/keywordize-keys m)]
    {:email (:mail m)
     :firstname (:givenname m)
     :lastname (:surname m)
     :org_id (:uniqueid m)}))

(defn validate-user-params! [params]
  (doseq [p [:email :firstname :lastname :org_id]]
    (when-not (presence (get params p nil))
      (throw (ex-info (str "The parameter " p " is required to sign in!")
                      {:status 412 :params params})))))

(defn create-or-update-user [params tx]
  (if (->> ["SELECT true AS exists FROM users WHERE lower(email) = lower(?)" (:email params)]
           (jdbc/query tx ) first :exists)
    (jdbc/update! tx :users params ["lower(email) = lower(?)" (:email params)])
    (jdbc/insert! tx :users params))
  (first (jdbc/query tx ["SELECT *  FROM users WHERE lower(email) = lower(?)" (:email params)])))

(defn shib-sign-in
  ([{headers :headers tx :tx sba :secret-ba}]
   (shib-sign-in headers (String. sba) tx))
  ([headers secret tx]
   (let [user-params (shib-params->user-params headers)
         _ (validate-user-params! user-params)
         user (create-or-update-user user-params tx)]
     (assert user)
     (session/create-user-session
       user secret (redirect (path :admin) :see-other) tx))))

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
  (when (= :json (-> request :accept :mime))
    (when-let [auth-ent (:authenticated-entity request)]
      {:body auth-ent})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def routes
  (cpj/routes
    (cpj/GET (path :auth) [] #'get-auth)
    (cpj/GET (path :auth-shib-sign-in) [] #'shib-sign-in)
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
(debug/debug-ns *ns*)
