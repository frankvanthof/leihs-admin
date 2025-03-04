(ns leihs.admin.auth.back.token
  (:refer-clojure :exclude [str keyword])
  (:require
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.constants :refer [USER_SESSION_COOKIE_NAME]]
    [leihs.core.ds :as ds]
    [leihs.core.ring-exception :as ring-exception]
    [leihs.core.sql :as sql]

    [clojure.java.jdbc :as jdbc]
    [clojure.walk :refer [keywordize-keys]]
    [clj-time.core :as time]

    [clojure.tools.logging :as logging]
    [logbug.catcher :as catcher]
    [logbug.debug :as debug]
    [logbug.thrown :as thrown]
    )
  (:import
    [java.util Base64]
    ))

(defn token-error-page [exception request]
  (-> {:status 401
       :body (str "Token authentication error: "
                  (.getMessage exception))}))

(defn token-matches-clause [token-secret]
  (sql/call
    := :api_tokens.token_hash
    (sql/call :crypt token-secret :api_tokens.token_hash)))

(defn user-with-valid-token-query [token-secret]
  (-> (sql/select
        :scope_read
        :scope_write
        :scope_admin_read
        :scope_admin_write
        :scope_system_admin_read
        :scope_system_admin_write
        [:users.id :user_id]
        :is_admin :account_enabled :firstname :lastname :email
        [:api_tokens.id :api_token_id]
        [:api_tokens.created_at :api_token_created_at])
      (sql/from :users)
      (sql/merge-join :api_tokens [:= :users.id :user_id])
      (sql/merge-where (token-matches-clause token-secret))
      (sql/merge-where [:= :account_enabled true])
      (sql/merge-where (sql/raw (str "now() < api_tokens.expires_at")))
      sql/format))

(defn server-admin-authenticated-entity-properties []
  {:scope_read true
   :scope_write true
   :scope_admin_read true
   :scope_admin_write true
   :scope_system_admin_read true
   :scope_system_admin_write true
   :user_id nil
   :is_admin true
   :account_enabled true
   :firstname ""
   :lastname "server-admin"
   :email "server-admin@leihs"
   :api_token_id nil
   :api_token_created_at (time/now)})

(defn user-auth-entity! [token-secret server-secret settings tx]
  (if-let [uae (or (->> (user-with-valid-token-query token-secret)
                        (jdbc/query (ds/get-ds)) first)
                   (when (and (= token-secret server-secret)
                              (-> settings :accept_server_secret_as_universal_password))
                     (server-admin-authenticated-entity-properties)))]
    (assoc uae
           :authentication-method :token
           :scope_admin_read (and (:scope_admin_read uae) (:is_admin uae))
           :scope_admin_write (and (:scope_admin_write uae) (:is_admin uae))
           :scope_system_admin_read (and (:scope_system_admin_read uae) (:is_system_admin uae))
           :scope_system_admin_write (and (:scope_system_admin_write uae) (:is_system_admin uae)))
    (throw (ex-info
             (str "No valid API-Token / User combination found! "
                  "Is the token present, not expired, and the user permitted to sign-in?"){}))))

(defn- decode-base64
  [^String string]
  (apply str (map char (.decode (Base64/getDecoder) (.getBytes string)))))

(defn extract-token-value [request]
  (when-let [auth-header (-> request :headers :authorization)]
    (or (some->> auth-header
                (re-find #"(?i)^token\s+(.*)$")
                last presence)
        (some->> auth-header
                 (re-find #"(?i)^basic\s+(.*)$")
                 last presence decode-base64
                 (#(clojure.string/split % #":" 2))
                 (map presence) (filter identity)
                 last))))

(defn authenticate [{tx :tx
                     sba :secret-ba
                     :as request}
                    _handler]
  (catcher/snatch
    {:level :warn
     :return-fn (fn [e] (token-error-page e request))}
    (let [handler (ring-exception/wrap _handler)]
      (if-let [token-secret (extract-token-value request)]
                 (let [user-auth-entity (user-auth-entity!
                                          token-secret (String. sba)
                                                           (:settings request) tx)]
          (handler (assoc request :authenticated-entity user-auth-entity)))
        (handler request)))))

(defn wrap [handler]
  (fn [request]
    (authenticate request handler)))

;#### debug ###################################################################
;(logging-config/set-logger! :level :debug)
;(logging-config/set-logger! :level :info)
;(debug/debug-ns *ns*)
