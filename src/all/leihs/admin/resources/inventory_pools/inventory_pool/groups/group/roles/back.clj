(ns leihs.admin.resources.inventory-pools.inventory-pool.groups.group.roles.back
  (:refer-clojure :exclude [str keyword])
  (:require [leihs.core.core :refer [keyword str presence]])
  (:require
    [leihs.admin.paths :refer [path]]
    [leihs.admin.resources.inventory-pools.inventory-pool.groups.back :refer [group-roles]]
    [leihs.admin.resources.inventory-pools.inventory-pool.roles :refer [expand-role-to-hierarchy allowed-roles-states]]
    [leihs.admin.resources.inventory-pools.inventory-pool.shared-lending-manager-restrictions :refer [protect-inventory-manager-escalation-by-lending-manager! protect-inventory-manager-restriction-by-lending-manager!]]
    [leihs.admin.resources.groups.back :as groups]
    [leihs.admin.utils.regex :as regex]
    [leihs.core.sql :as sql]
    [leihs.admin.utils.jdbc :as utils.jdbc]

    [clojure.java.jdbc :as jdbc]
    [compojure.core :as cpj]
    [clojure.set :as set]

    [clj-logging-config.log4j :as logging-config]
    [clojure.tools.logging :as logging]
    [logbug.debug :as debug]
    ))

;;; roles ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn role-query [inventory-pool-id group-id]
  (-> (sql/select :role)
      (sql/from :group_access_rights)
      (sql/merge-where [:= :inventory_pool_id inventory-pool-id])
      (sql/merge-where [:= :group_id group-id])))

(defn roles
  [{{inventory-pool-id :inventory-pool-id group-id :group-id} :route-params
    tx :tx :as request}]
  {:body {:roles (group-roles tx inventory-pool-id group-id)}})


(defn set-roles
  [{{inventory-pool-id :inventory-pool-id group-id :group-id} :route-params
    tx :tx data :body :as request}]
  (protect-inventory-manager-escalation-by-lending-manager! request)
  (protect-inventory-manager-restriction-by-lending-manager! role-query request)
  (let [roles (:roles data)]
    (logging/debug 'roles roles)
    (if-let [allowed-role-key (some->> allowed-roles-states
                                       (into [])
                                       (filter #(= roles (second %)))
                                       first first)]
      (do (jdbc/delete! tx :group_access_rights ["inventory_pool_id = ? AND group_id =? " inventory-pool-id group-id])
          (when (not= allowed-role-key :none)
            (jdbc/insert! tx :group_access_rights {:inventory_pool_id inventory-pool-id
                                                   :group_id group-id
                                                   :role (str allowed-role-key)}))
          {:status 204})
      {:status 422 :data {:message "Submitted combination of roles is not allowed!"}})))
;;; routes ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def inventory-pool-group-roles-path
  (path :inventory-pool-group-roles {:inventory-pool-id ":inventory-pool-id" :group-id ":group-id"}))

(def routes
  (cpj/routes
    (cpj/GET inventory-pool-group-roles-path [] #'roles)
    (cpj/PUT inventory-pool-group-roles-path [] #'set-roles)))


;#### debug ###################################################################
;(logging-config/set-logger! :level :debug)
;(logging-config/set-logger! :level :info)
;(debug/wrap-with-log-debug #'filter-by-access-right)
;(debug/wrap-with-log-debug #'groups-formated-query)
;(debug/debug-ns *ns*)
