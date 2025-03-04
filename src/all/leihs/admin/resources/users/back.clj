(ns leihs.admin.resources.users.back
  (:refer-clojure :exclude [str keyword])
  (:require [leihs.core.core :refer [keyword str presence]])
  (:require
    [leihs.admin.paths :refer [path]]
    [leihs.core.sql :as sql]
    [leihs.admin.resources.user.back :as user]
    [leihs.admin.resources.users.shared :as shared]

    [clojure.java.jdbc :as jdbc]
    [clojure.set]
    [compojure.core :as cpj]

    [clj-logging-config.log4j :as logging-config]
    [clojure.tools.logging :as logging]
    [logbug.debug :as debug]
    ))


(def users-base-query
  (-> (apply sql/select (map #(keyword (str "users." %)) shared/default-fields))
      (sql/from :users)
      (sql/order-by :lastname :firstname :id)
      (sql/merge-where [:= nil :delegator_user_id])))

(defn set-per-page-and-offset
  ([query {per-page :per-page page :page}]
   (when (or (-> per-page presence not)
             (-> per-page integer? not)
             (> per-page 1000)
             (< per-page 1))
     (throw (ex-info "The query parameter per-page must be present and set to an integer between 1 and 1000."
                     {:status 422})))
   (when (or (-> page presence not)
             (-> page integer? not)
             (< page 0))
     (throw (ex-info "The query parameter page must be present and set to a positive integer."
                     {:status 422})))
   (set-per-page-and-offset query per-page page))
  ([query per-page page]
   (-> query
       (sql/limit per-page)
       (sql/offset (* per-page (- page 1))))))

(defn match-term-with-emails [query term]
  (sql/merge-where
    query
    [:or
     [:= (sql/call :lower term) (sql/call :lower :users.email)]
     [:= (sql/call :lower term) (sql/call :lower :users.secondary_email)]]))

(defn match-term-fuzzy [query term]
  (sql/merge-where query [:or
                          ["%" (str term) :searchable]
                          ["~~*" :searchable (str "%" term "%")]]))

(defn term-filter [query request]
  (if-let [term (-> request :query-params-raw :term presence)]
    (if (clojure.string/includes? term "@" )
      (match-term-with-emails query term)
      (match-term-fuzzy query term))
    query))

(defn type-filter [query request]
  (case (-> request :query-params :type)
    (nil "any") query
    "org" (-> query
              (sql/merge-where [:<> nil :org_id]))
    "manual" (-> query
                 (sql/merge-where [:= nil :org_id]))))

(defn admins-filter [query request]
  (let [is-admin (-> request :query-params :is_admin)]
    (case is-admin
      ("true" true) (sql/merge-where query [:= :is_admin true])
      query)))

(defn select-fields [query request]
  (if-let [fields (some->> request :query-params :fields
                           (map keyword) set
                           (clojure.set/intersection shared/available-fields))]
    (apply sql/select query fields)
    query))

(defn users-query [request]
  (let [query-params (-> request :query-params
                         shared/normalized-query-parameters)]
    (-> users-base-query
        (set-per-page-and-offset query-params)
        (term-filter request)
        (type-filter request)
        (admins-filter request)
        (select-fields request))))

(defn users-formated-query [request]
  (-> request
      users-query
      sql/format))

(defn users [request]
  (when (= :json (-> request :accept :mime))
    {:body
     {:users
      (jdbc/query (:tx request) (users-formated-query request))}}))

(def routes
  (cpj/routes
    (cpj/GET (path :users) [] #'users)
    (cpj/POST (path :users) [] #'user/routes)))

;#### debug ###################################################################
(logging-config/set-logger! :level :debug)
;(debug/debug-ns *ns*)

;(logging-config/set-logger! :level :debug)
(debug/wrap-with-log-debug #'term-filter)
;(debug/wrap-with-log-debug #'users-formated-query)
