(ns leihs.admin.resources.inventory-pools.inventory-pool.users.back
  (:refer-clojure :exclude [str keyword])
  (:require [leihs.core.core :refer [keyword str presence]])
  (:require
    [leihs.admin.paths :refer [path]]
    [leihs.admin.resources.inventory-pools.inventory-pool.roles :as roles]
    [leihs.admin.resources.users.back :as users]
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

;;; users ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn filter-effective-role [query inventory-pool-id role]
  (-> query
      (sql/merge-join :access_rights
                      [:and
                       [:= :access_rights.user_id :users.id]
                       [:= :access_rights.inventory_pool_id inventory-pool-id]])
      (sql/merge-where [:in :access_rights.role
                        (map str (roles/expand-to-hierarchy-up-and-include role))])))

(defn filter-for-none-role [query inventory-pool-id]
  (-> query
      (sql/merge-where
        [:not [:exists (-> (sql/select :true)
                           (sql/from :access_rights)
                           (sql/merge-where [:= :access_rights.inventory_pool_id inventory-pool-id])
                           (sql/merge-where [:= :access_rights.user_id :users.id]))]])))

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

(defn filter-suspended [query inventory-pool-id {:as request}]
  (if-not (-> request :query-params :suspended)
    query
    (-> query
        (sql/merge-join :suspensions
                        [:and
                         [:= :suspensions.user_id :users.id]
                         [:= :suspensions.inventory_pool_id inventory-pool-id]])
        (sql/merge-where (sql/raw  "CURRENT_DATE <= suspensions.suspended_until")))))

(defn users-query [inventory-pool-id request]
  (-> request users/users-query
      (sql/merge-select [inventory-pool-id :inventory_pool_id])
      (filter-by-role inventory-pool-id request)
      (filter-suspended inventory-pool-id request)))


(defn inventory-pool-users-count-query [inventory-pool-id]
  (-> (sql/select :%count.*)
      (sql/from :access_rights)
      (sql/merge-where
        [:= :access_rights.inventory_pool_id inventory-pool-id])
      (sql/format)))

(defn users-formated-query [inventory-pool-id request]
  (-> (users-query inventory-pool-id request)
      sql/format))

(defn role-query [inventory-pool-id user-id]
  (-> (sql/select :role :origin_table)
      (sql/from :access_rights)
      (sql/merge-where [:= :inventory_pool_id inventory-pool-id])
      (sql/merge-where [:= :user_id user-id])))

(defn user-roles [tx inventory-pool-id user-id]
  (let [role-kw (->> (role-query inventory-pool-id user-id)
                     sql/format
                     (jdbc/query tx)
                     first :role keyword)]
    (-> role-kw
        roles/expand-role-to-hierarchy
        roles/roles-to-map)))

(defn user-add-roles [tx inventory-pool-id user]
  (assoc user :roles (user-roles tx inventory-pool-id (:id  user))))


(defn suspension-query [inventory-pool-id user-id]
  (-> (sql/select :*)
      (sql/from :suspensions)
      (sql/merge-where [:= :user_id user-id])
      (sql/merge-where [:= :inventory_pool_id inventory-pool-id])
      (sql/merge-where (sql/raw  "CURRENT_DATE <= suspended_until"))))

(defn user-suspension [tx inventory-pool-id user-id]
  (->> (suspension-query inventory-pool-id user-id)
       sql/format
       (jdbc/query tx)
       first))

(defn user-add-suspension [tx inventory-pool-id user]
  (assoc user :suspension (user-suspension tx inventory-pool-id (:id  user))))

(defn users [{{inventory-pool-id :inventory-pool-id} :route-params
              tx :tx :as request}]
  (let [inventory-pool-id (normalized-inventory-pool-id! inventory-pool-id tx)]
    {:body
     {:inventory-pool_users_count (->> (inventory-pool-users-count-query inventory-pool-id)
                                       (jdbc/query tx)
                                       first :count)
      :users (->> (users-formated-query inventory-pool-id request)
                  (jdbc/query tx)
                  (map (fn [user] (user-add-roles tx inventory-pool-id user)))
                  (map (fn [user] (user-add-suspension tx inventory-pool-id user)))
                  doall)}}))

;;; routes ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def inventory-pool-user-path
  (path :inventory-pool-user {:inventory-pool-id ":inventory-pool-id" :user-id ":user-id"}))

(def inventory-pool-users-path
  (path :inventory-pool-users {:inventory-pool-id ":inventory-pool-id"}))

(def routes
  (cpj/routes
    (cpj/GET inventory-pool-users-path [] #'users)))


;#### debug ###################################################################
;(logging-config/set-logger! :level :debug)
;(logging-config/set-logger! :level :info)
;(debug/wrap-with-log-debug #'filter-suspended)
;(debug/wrap-with-log-debug #'users-formated-query)
;(debug/debug-ns *ns*)
