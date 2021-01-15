(ns leihs.admin.resources.audits.changes.main
  (:refer-clojure :exclude [str keyword])
  (:require [leihs.core.core :refer [keyword str presence]])
  (:require
    [leihs.core.sql :as sql]
    [leihs.core.auth.core :as auth]
    [leihs.core.routing.back :as routing :refer [set-per-page-and-offset wrap-mixin-default-query-params]]

    [leihs.admin.paths :refer [path]]
    [leihs.admin.resources.audits.changes.shared :refer [default-query-params]]

    [clojure.java.jdbc :as jdbc]
    [compojure.core :as cpj]

    [clj-logging-config.log4j :as logging-config]
    [clojure.tools.logging :as logging]
    [logbug.debug :as debug]
    [logbug.catcher :as catcher]
    ))

(defn audited-changes-meta
  [{tx :tx :as request}]
  {:body
   {:tables
    (->> ["SELECT DISTINCT table_name FROM audited_changes"]
         (jdbc/query tx)
         (map :table_name))}})


(def request-sub
  (-> (sql/select :true)
      (sql/from :audited_requests)
      (sql/merge-where
        [:= :audited_changes.txid :audited_requests.txid])))

(def selects
  [:audited-changes.id
   :audited-changes.txid
   :audited-changes.tg_op
   :audited-changes.table_name
   :audited-changes.created_at
   :audited-changes.pkey
   [request-sub :has_request]])

(def changes-base-query
  (-> (apply sql/select selects)
      (sql/merge-select
        [(sql/call
           :array_to_json
           (sql/call
             :ARRAY
             (sql/select
               (sql/call :jsonb_object_keys :audited_changes.changed))))
         :changed_attributes])
      (sql/from :audited_changes)
      (sql/order-by [:audited_changes.created_at :desc]
                    [:audited_changes.table_name :asc]
                    [:audited_changes.pkey :asc])))

(defn filter-by-search-term
  [query {{term :term} :query-params :as request}]
  (if-let [term (presence term)]
    (-> query
        (sql/merge-where ["@@" :audited_changes.changed term]))
    query))

(defn filter-by-table
  [query {{table-name :table} :query-params :as request}]
  (if-not (presence table-name)
    query
    (-> query
        (sql/merge-where
          [:= :audited_changes.table_name table-name]))))

(defn filter-by-txid [query {{txid :txid} :query-params}]
  (if-let [txid (presence txid)]
    (sql/merge-where query [:= :audited_changes.txid (str txid)])
    query))


(defn filter-by-pkey [query {{pkey :pkey} :query-params}]
  (if-let [pkey (presence pkey)]
    (sql/merge-where query [:= :audited_changes.pkey (str pkey)])
    query))


(defn filter-by-tg-op [query {{tg-op :tg-op} :query-params}]
  (if-let [tg-op (presence tg-op)]
    (sql/merge-where query [:= :audited_changes.tg_op tg-op])
    query))

(defn audited-changes [{tx :tx :as request}]
  {:body
   {:changes
    (->> (-> changes-base-query
             (set-per-page-and-offset request)
             (filter-by-search-term request)
             (filter-by-txid request)
             (filter-by-pkey request)
             (filter-by-tg-op request)
             (filter-by-table request)
             sql/format)
         (jdbc/query tx))}})

(def routes
  (-> (cpj/routes
        (cpj/GET (path :audited-changes-meta {}) [] #'audited-changes-meta)
        (cpj/GET (path :audited-changes {}) [] #'audited-changes))
      (wrap-mixin-default-query-params default-query-params)))

;#### debug ###################################################################
;(logging-config/set-logger! :level :debug)
;(debug/debug-ns *ns*)
;(debug/wrap-with-log-debug #'users-formated-query)
;(debug/wrap-with-log-debug #'users-formated-query)
