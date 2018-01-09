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

    [clojure.tools.logging :as logging]
    [logbug.debug :as debug])
  (:import
    [java.util UUID]
    ))

(defn prepare-data [data]
  (-> data
      (select-keys [:firstname :lastname :login :email])
      (assoc :pw_hash (password-hash (:password data) @ds)
             :id (UUID/randomUUID))))

(defn insert-user [data tx]
  (first (jdbc/insert! tx :users data)))

(defn create-initial-admin
  ([{tx :tx data :body}]
   (create-initial-admin data tx))
  ([data tx]
   (if-let [user (-> data prepare-data (insert-user tx))]
     {:status 201
      :body (select-keys user [:id])}
     {:status 422})))

(def routes
  (cpj/routes
    (cpj/POST (path :initial-admin) [] create-initial-admin)))

;#### debug ###################################################################
;(logging-config/set-logger! :level :debug)
;(logging-config/set-logger! :level :info)
;(debug/debug-ns 'cider-ci.utils.shutdown)
;(debug/debug-ns *ns*)
