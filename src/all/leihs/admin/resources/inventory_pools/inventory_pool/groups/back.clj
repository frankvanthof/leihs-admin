(ns leihs.admin.resources.inventory-pools.inventory-pool.groups.back
  (:refer-clojure :exclude [str keyword])
  (:require [leihs.core.core :refer [keyword str presence]])
  (:require
    [leihs.admin.paths :refer [path]]
    [leihs.admin.resources.inventory-pools.inventory-pool.roles :as roles]
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

(defn normalized-inventory-pool-id! [inventory-pool-id tx]
  "Get the id, i.e. the pkey, given either the id or the org_id and
  enforce some sanity checks like uniqueness and presence"
  (assert (presence inventory-pool-id) "inventory-pool-id must not be empty!")
  (let [inventory-pool-id (str inventory-pool-id)
        where-clause  (if (re-matches regex/uuid-pattern inventory-pool-id)
                        [:or
                         [:= :inventory-pools.id inventory-pool-id]
                         [:= :inventory-pools.name inventory-pool-id]])
        ids (->> (-> (sql/select :id)
                     (sql/from :inventory-pools)
                     (sql/merge-where where-clause)
                     sql/format)
                 (jdbc/query tx)
                 (map :id))]
    (assert (= 1 (count ids))
            "exactly one inventory-pool must match the given inventory-pool-id either name, or id")
    (first ids)))

;;; groups ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn filter-effective-role [query inventory-pool-id role]
  (-> query
      (sql/merge-join :group_access_rights
                      [:and
                       [:= :group_access_rights.group_id :groups.id]
                       [:= :group_access_rights.inventory_pool_id inventory-pool-id]])
      (sql/merge-where [:in :group_access_rights.role
                        (map str (roles/expand-to-hierarchy-up-and-include role))])))


(defn filter-for-none-role [query inventory-pool-id]
  (-> query
      (sql/merge-where
        [:not [:exists (-> (sql/select :true)
                           (sql/from :group_access_rights)
                           (sql/merge-where [:= :group_access_rights.inventory_pool_id inventory-pool-id])
                           (sql/merge-where [:= :group_access_rights.group_id :groups.id]))]])))

(defn filter-by-role [query inventory-pool-id {:as request}]
  (let [role (-> request :query-params :role presence str)]
    (case role
      "" query
      "any" query
      "none" (filter-for-none-role query inventory-pool-id)
      ("customer"
        "group_manager"
        "lending_manager"
        "inventory_manager") (filter-effective-role
                               query inventory-pool-id role))))


(defn groups-query [inventory-pool-id request]
  (-> request groups/groups-query
      (sql/merge-select [inventory-pool-id :inventory_pool_id])
      (filter-by-role inventory-pool-id request)))


(defn inventory-pool-groups-count-query [inventory-pool-id]
  (-> (sql/select :%count.*)
      (sql/from :group_access_rights)
      (sql/merge-where
        [:= :group_access_rights.inventory_pool_id inventory-pool-id])
      (sql/format)))

(defn groups-formated-query [inventory-pool-id request]
  (-> (groups-query inventory-pool-id request)
      sql/format))

(defn role-query [inventory-pool-id group-id]
  (-> (sql/select :role)
      (sql/from :group_access_rights)
      (sql/merge-where [:= :inventory_pool_id inventory-pool-id])
      (sql/merge-where [:= :group_id group-id])))

(defn group-roles [tx inventory-pool-id group-id]
  (let [role-kw (->> (role-query inventory-pool-id group-id)
                     sql/format
                     (jdbc/query tx)
                     first :role keyword)]
    (-> role-kw
        roles/expand-role-to-hierarchy
        roles/roles-to-map)))

(defn group-add-roles [tx inventory-pool-id group]
  (assoc group :roles (group-roles tx inventory-pool-id (:id  group))))

(defn groups [{{inventory-pool-id :inventory-pool-id} :route-params
               tx :tx :as request}]
  (let [inventory-pool-id (normalized-inventory-pool-id! inventory-pool-id tx)]
    {:body
     {:inventory-pool_groups_count (->> (inventory-pool-groups-count-query inventory-pool-id)
                                        (jdbc/query tx)
                                        first :count)
      :groups (->> (groups-formated-query inventory-pool-id request)
                   (jdbc/query tx)
                   (map (fn [group] (group-add-roles tx inventory-pool-id group)))
                   doall)}}))

;;; routes ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def inventory-pool-group-path
  (path :inventory-pool-group {:inventory-pool-id ":inventory-pool-id" :group-id ":group-id"}))

(def inventory-pool-groups-path
  (path :inventory-pool-groups {:inventory-pool-id ":inventory-pool-id"}))

(def routes
  (cpj/routes
    (cpj/GET inventory-pool-groups-path [] #'groups)))


;#### debug ###################################################################
;(logging-config/set-logger! :level :debug)
;(logging-config/set-logger! :level :info)
;(debug/wrap-with-log-debug #'filter-suspended)
;(debug/wrap-with-log-debug #'groups-formated-query)
;(debug/debug-ns *ns*)
