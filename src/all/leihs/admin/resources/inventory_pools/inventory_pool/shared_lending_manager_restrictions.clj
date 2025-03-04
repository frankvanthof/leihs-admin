(ns leihs.admin.resources.inventory-pools.inventory-pool.shared-lending-manager-restrictions
  (:refer-clojure :exclude [str keyword])
  (:require [leihs.core.core :refer [keyword str presence]])
  (:require
    [leihs.admin.resources.inventory-pools.authorization :refer [pool-access-right-for-route]]
    [leihs.core.sql :as sql]
    [clojure.java.jdbc :as jdbc]
    [clj-logging-config.log4j :as logging-config]
    [clojure.tools.logging :as logging]
    [logbug.debug :as debug]))


(defn acts-as-lending-manger? [{authenticated-entity :authenticated-entity :as request}]
  (boolean (and (not (:scope_admin_write authenticated-entity))
                (= "lending_manager" (-> request pool-access-right-for-route :role)))))


(defn protect-inventory-manager-escalation-by-lending-manager!
  [{{roles :roles} :body :as request}]
  (when (and (:inventory_manager roles)
             (acts-as-lending-manger? request))
    (throw
      (ex-info
        "A lending_manager may not escalate roles to an inventory_manager"
        {:status 403}))))

(defn protect-inventory-manager-restriction-by-lending-manager!
  [access-rights-query
   {{inventory-pool-id :inventory-pool-id user-id :user-id group-id :group-id} :route-params
    tx :tx {roles :roles} :body :as request}]
  (when (and (not (:inventory_manager roles))
            (acts-as-lending-manger? request))
    (when-let [existing-access-right (->> (access-rights-query
                                            inventory-pool-id (or group-id  user-id))
                                          sql/format (jdbc/query tx) first)]
      (when (= (:role existing-access-right) "inventory_manager")
        (throw
          (ex-info
            "A lending_manager may not restrict the roles of an inventory_manager"
            {:status 403}))))))


;#### debug ###################################################################
;(debug/debug-ns *ns*)
;(logging-config/set-logger! :level :debug)
;(logging-config/set-logger! :level :info)
;(debug/wrap-with-log-debug #'filter-by-access-right)
