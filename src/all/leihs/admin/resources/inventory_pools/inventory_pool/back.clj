(ns leihs.admin.resources.inventory-pools.inventory-pool.back
  (:refer-clojure :exclude [str keyword])
  (:require [leihs.core.core :refer [keyword str presence]])
  (:require
    [leihs.admin.paths :refer [path]]
    [leihs.core.sql :as sql]
    [leihs.admin.resources.inventory-pools.shared :as shared :refer [inventory-pool-path]]
    [leihs.admin.resources.mail-templates.back :as mail-templates]

    [clojure.set :refer [rename-keys]]
    [clojure.java.jdbc :as jdbc]
    [compojure.core :as cpj]

    [clj-logging-config.log4j :as logging-config]
    [clojure.tools.logging :as logging]
    [logbug.debug :as debug]
    [logbug.catcher :as catcher]
    ))

(def fields
  #{:description
    :id
    :is_active
    :email
    :name
    :shortname})

(defn inventory-pool
  [{{inventory-pool-id :inventory-pool-id} :route-params tx :tx :as request}]
  {:body (->> (-> (apply sql/select fields)
                  (sql/from :inventory-pools)
                  (sql/merge-where [:= :id inventory-pool-id])
                  sql/format)
              (jdbc/query tx)
              first )})

(defn create-inventory-pool [{tx :tx data :body :as request}]
  (if-let [inventory-pool (first (jdbc/insert!
                                   tx :inventory_pools
                                   (select-keys data fields)))]
    (do
      (jdbc/insert! tx :workdays
                    {:inventory_pool_id (:id inventory-pool)})
      (mail-templates/create-for-inventory-pool tx (:id inventory-pool))
      {:body inventory-pool})
    {:status 422
     :body "No inventory-pool has been created."}))

(defn patch-inventory-pool
  [{{inventory-pool-id :inventory-pool-id} :route-params
    tx :tx data :body :as request}]
  (when (->> ["SELECT true AS exists FROM inventory_pools WHERE id = ?" inventory-pool-id]
             (jdbc/query tx )
             first :exists)
    (jdbc/update! tx :inventory_pools
                  (select-keys data fields)
                  ["id = ?" inventory-pool-id])
    {:status 204}))

(defn delete-inventory-pool [{tx :tx {inventory-pool-id :inventory-pool-id} :route-params}]
  (assert inventory-pool-id)
  (if (= [1] (jdbc/delete! tx :inventory_pools ["id = ?" inventory-pool-id]))
    {:status 204}
    {:status 404 :body "Delete inventory-pool failed without error."}))

(def routes
  (cpj/routes
    (cpj/GET inventory-pool-path [] #'inventory-pool)
    (cpj/DELETE inventory-pool-path [] #'delete-inventory-pool)
    (cpj/PATCH inventory-pool-path [] #'patch-inventory-pool)
    (cpj/POST (path :inventory-pools) [] #'create-inventory-pool)))
