(ns leihs.admin.resources.users.back
  (:refer-clojure :exclude [str keyword])
  (:require [leihs.admin.utils.core :refer [keyword str presence]])
  (:require
    [leihs.admin.paths :refer [path]]
    [leihs.admin.utils.sql :as sql]
    [leihs.admin.resources.user.back :as user]

    [clojure.java.jdbc :as jdbc]
    [compojure.core :as cpj]

    [clojure.tools.logging :as logging]
    [logbug.debug :as debug]
    ))


(def users-base-query
  (-> (sql/select :id, :login, :firstname, :lastname, :email, :img32_data_url, :org_id)
      (sql/from :users)
      (sql/order-by :lastname :firstname)
      (sql/merge-where [:= nil :delegator_user_id])
      ))

(defn set-per-page-and-offset
  ([query {{per-page :per-page page :page} :query-params}]
   (logging/info 'per-page per-page)
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

(defn term-fitler [query request]
  (if-let [term (-> request :query-params :term presence)]
    (-> query
        (sql/merge-where [:or
                          ["%" (str term) :searchable]
                          ["~~*" :searchable (str "%" term "%")]]))
    query))

(defn type-filter [query request]
  (case (-> request :query-params :type)
    (nil "any") query
    "org" (-> query
              (sql/merge-where [:<> nil :org_id]))
    "manual" (-> query
                 (sql/merge-where [:= nil :org_id]))))

(defn role-filter [query request]
  (let [role (-> request :query-params :role)]
    (case role
      (nil "any") query
      (-> query
          (sql/merge-where
            [:exists
             (-> (sql/select true)
                 (sql/from :access_rights)
                 (sql/merge-where [:= :access_rights.role role])
                 (sql/merge-where [:= :access_rights.user_id :users.id]))])))))

(defn admins-filter [query request]
  (let [is-admin (-> request :query-params :is_admin)]
    (case is-admin
      ("true" true) (sql/merge-where query [:= :is_admin true])
      query)))

(defn users-query [request]
  (-> users-base-query
      (set-per-page-and-offset request)
      (term-fitler request)
      (type-filter request)
      (role-filter request)
      (admins-filter request)
      sql/format))

(defn users [request]
  (when (= :json (-> request :accept :mime))
    {:body
     {:users
      (jdbc/query (:tx request) (users-query request))}}))


(def routes
  (cpj/routes
    (cpj/GET (path :users) [] #'users)
    (cpj/POST (path :users) [] #'user/routes)))

