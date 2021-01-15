(ns leihs.admin.resources.inventory-pools.main
  (:refer-clojure :exclude [str keyword])
  (:require
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.sql :as sql]
    [leihs.core.routing.back :as routing :refer [set-per-page-and-offset wrap-mixin-default-query-params]]

    [leihs.admin.paths :refer [path]]
    [leihs.admin.resources.inventory-pools.inventory-pool.main :as inventory-pool]
    [leihs.admin.resources.inventory-pools.shared :as shared :refer [inventory-pool-path]]

    [clojure.java.jdbc :as jdbc]
    [compojure.core :as cpj]

    [clj-logging-config.log4j :as logging-config]
    [clojure.tools.logging :as logging]
    [logbug.debug :as debug]
    [logbug.catcher :as catcher]
    ))

(def users-count-sub
  (-> (sql/select :%count.*)
      (sql/from :users)
      (sql/merge-where [:= nil :delegator_user_id])
      (sql/merge-join :access_rights [:= :access_rights.user_id :users.id])
      (sql/merge-where [:= :access_rights.inventory_pool_id :inventory_pools.id])))

(def delegations-count-sub
  (-> (sql/select :%count.*)
      (sql/from [:users :delegations])
      (sql/merge-where [:<> nil :delegator_user_id])
      (sql/merge-join :access_rights [:= :access_rights.user_id :delegations.id])
      (sql/merge-where [:= :access_rights.inventory_pool_id :inventory_pools.id])))

(def inventory-pools-base-query
  (-> (apply sql/select (map #(keyword (str "inventory-pools." %)) shared/default-fields))
      (sql/merge-select [users-count-sub :users_count])
      (sql/merge-select [delegations-count-sub :delegations_count])
      (sql/from :inventory_pools)))

(defn set-order [query {query-params :query-params :as request}]
  (let [order (some-> query-params :order seq vec
                      (->> (map (fn [[f o]] [(keyword f) (keyword o)]))))]
    (case order
      ([[:name :asc][:id :asc]]
       [[:users_count :desc] [:id :asc]]
       [[:delegations_count :desc][:id :asc]]) (apply sql/order-by query order)
      (apply sql/order-by query [[:name :asc][:id :asc]]))))

(defn term-fitler [query request]
  (if-let [term (-> request :query-params-raw :term presence)]
    (-> query
        (sql/merge-where [:or
                          ["%" (str term) :name]
                          ["~~*" :name (str "%" term "%")]]))
    query))

(defn activity-filter [query request]
  (case (-> request :query-params-raw :active)
    "true" (sql/merge-where query [:= :inventory_pools.is_active true])
    "false" (sql/merge-where query [:= :inventory_pools.is_active false])
    query))

(defn inventory-pools-query [{:as request}]
  (-> inventory-pools-base-query
      (set-per-page-and-offset request)
      (set-order request)
      (activity-filter request)
      (term-fitler request)))

(defn inventory-pools [{tx :tx :as request}]
  {:body {:inventory-pools
          (-> request
              inventory-pools-query
              sql/format
              (->> (jdbc/query tx))

              )}})

(def routes
  (-> (cpj/routes
        (cpj/GET (path :inventory-pools) [] #'inventory-pools)
        (cpj/POST (path :inventory-pools) [] inventory-pool/routes)
        (cpj/ANY inventory-pool-path [] inventory-pool/routes))
      (wrap-mixin-default-query-params shared/default-query-params)))


;#### debug ###################################################################
;(logging-config/set-logger! :level :debug)
;(debug/wrap-with-log-debug #'activity-filter)
;(debug/wrap-with-log-debug #'set-order)
;(debug/wrap-with-log-debug #'inventory-pools-query)
;(debug/wrap-with-log-debug #'inventory-pools-formated-query)
;(logging-config/set-logger! :level :info)
;(debug/debug-ns *ns*)
