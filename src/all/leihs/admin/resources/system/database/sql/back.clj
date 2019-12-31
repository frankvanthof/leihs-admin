(ns leihs.admin.resources.system.database.sql.back
  (:refer-clojure :exclude [str keyword])
  (:require
    [leihs.core.core :refer [keyword str presence]]

    [leihs.admin.auth.back :as admin-auth]
    [leihs.admin.paths :refer [path]]

    [leihs.core.sql :as sql]
    [leihs.core.ds :as ds]

    [clojure.java.jdbc :as jdbc]
    [clojure.set]
    [compojure.core :as cpj]

    [clj-logging-config.log4j :as logging-config]
    [clojure.tools.logging :as logging]
    [logbug.debug :as debug]
    ))


(defn query [{tx :tx :as request}]
  )

(defn command [request]
  )

(def ^:private sql-path (path :sql {})) 

(def routes
  (cpj/routes 
    (-> (cpj/routes 
          (cpj/GET sql-path [] #'query)
          (cpj/POST sql-path [] #'command))
        (admin-auth/wrap-authorize 
          {:required-scopes {:scope_admin_read true
                             :scope_admin_write true 
                             :scope_system_admin_read true 
                             :scope_system_admin_write true}}))))


