(ns leihs.admin.resources.initial-admin.core
  (:refer-clojure :exclude [str keyword])
  (:require [leihs.admin.utils.core :refer [keyword str presence]])
  (:require
    [leihs.admin.paths :refer [path]]
    [leihs.admin.utils.sql :as sql]
    [leihs.admin.utils.ds :refer [ds]]
    [leihs.admin.resources.user.back :refer [password-hash]]

    [clojure.java.jdbc :as jdbc]
    [compojure.core :as cpj]
    [ring.util.response :refer [redirect]]

    [clojure.tools.logging :as logging]
    [logbug.debug :as debug])
  (:import
    [java.util UUID]
    ))

(defn some-admin? [tx]
  (->> ["SELECT true AS has_admin FROM users WHERE is_admin = true"]
       (jdbc/query tx ) first :has_admin boolean))

(defn prepare-data [data]
  (-> data
      (select-keys [:email])
      (assoc :is_admin true
             :pw_hash (password-hash (:password data) @ds)
             :id (UUID/randomUUID))))

(defn insert-user [data tx]
  (first (jdbc/insert! tx :users data)))

(defn create-initial-admin
  ([{tx :tx form-params :form-params data :body}]
   (create-initial-admin (if (empty? form-params)
                           data form-params) tx))
  ([data tx]
   (if (some-admin? tx)
     {:status 403
      :body "A admin user already exists!"}
     (when-let [user (-> data prepare-data (insert-user tx))]
       (redirect (path :admin) :see-other)))))

(def routes
  (cpj/routes
    (cpj/POST (path :initial-admin) [] create-initial-admin)))

(defn wrap
  ([handler] (fn [request] (wrap handler request)))
  ([handler request]
   (if (or (not= (-> request :accept :mime) :html)
           (= (:handler-key request) :initial-admin)
           (some-admin? (:tx request)))
     (handler request)
     (redirect (path :initial-admin) :see-other))))

;#### debug ###################################################################
;(logging-config/set-logger! :level :debug)
;(logging-config/set-logger! :level :info)
;(debug/debug-ns 'cider-ci.utils.shutdown)
;(debug/debug-ns *ns*)
