(ns leihs.admin.resources.users.user.inventory-pools
  (:refer-clojure :exclude [str keyword])
  (:require [leihs.core.core :refer [keyword str presence]])
  (:require
    [leihs.core.sql :as sql]

    [leihs.admin.paths :refer [path]]
    [leihs.admin.resources.users.user.core :refer [sql-merge-unique-user]]

    [clojure.set :refer [rename-keys]]
    [clojure.java.jdbc :as jdbc]
    [compojure.core :as cpj]

    [clj-logging-config.log4j :as logging-config]
    [clojure.tools.logging :as logging]
    [logbug.debug :as debug]
    [logbug.catcher :as catcher]
    ))


(def contracts-count
  (-> (sql/select :%count.*)
      (sql/from :contracts)
      (sql/merge-where [:= :contracts.inventory_pool_id :inventory-pools.id])
      (sql/merge-where [:= :contracts.user_id :users.id])))

(def open-contracts-count
  (-> (sql/select :%count.*)
      (sql/from :contracts)
      (sql/merge-where [:= :contracts.user_id :users.id])
      (sql/merge-where [:= :contracts.inventory_pool_id :inventory-pools.id])
      (sql/merge-where [:= :contracts.state "open"])))


(defn reservations-count [& {:keys [stati]
                             :or {stati ["unsubmitted"
                                         "submitted"
                                         "approved"
                                         "rejected"
                                         "closed"
                                         "signed"]}}]
  (-> (sql/select :%count.*)
      (sql/from :reservations)
      (sql/merge-where [:= :reservations.inventory_pool_id :inventory-pools.id])
      (sql/merge-where [:= :reservations.user_id :users.id])
      (sql/merge-where [:in :reservations.status stati])))


(defn user-inventory-pools-query [uid]
  (-> (sql/select :access_rights.role
                  [:inventory_pools.name :inventory_pool_name]
                  [:inventory_pools.id :inventory_pool_id])
      (sql/from :users)
      (sql-merge-unique-user uid)
      (sql/merge-join :access_rights [:= :users.id :access_rights.user_id])
      (sql/merge-join :inventory_pools [:= :access_rights.inventory_pool_id :inventory_pools.id])
      (sql/merge-select [open-contracts-count :open_contracts_count])
      (sql/merge-select [contracts-count :contracts_count])
      (sql/merge-select [(reservations-count :stati ["submitted"]) :submitted_reservations_count])
      (sql/merge-select [(reservations-count :stati ["approved"]) :approved_reservations_count])
      (sql/merge-select [(reservations-count) :reservations_count])
      sql/format
      ))

(defn inventory-pools [uid tx]
  (->> uid
       user-inventory-pools-query
       (jdbc/query tx)))

(defn user-inventory-pools
  [{tx :tx data :body {uid :user-id} :route-params}]
  {:body
   {:user-inventory-pools
    (inventory-pools uid tx)}})


;;; create user ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def routes
  (cpj/routes
    (cpj/GET (path :user-inventory-pools {:user-id ":user-id"}) [] #'user-inventory-pools)))


;#### debug ###################################################################
;(logging-config/set-logger! :level :debug)
;(debug/wrap-with-log-debug #'data-url-img->buffered-image)
;(debug/wrap-with-log-debug #'buffered-image->data-url-img)
;(debug/wrap-with-log-debug #'resized-img)
;(logging-config/set-logger! :level :info)
;(debug/debug-ns *ns*)
