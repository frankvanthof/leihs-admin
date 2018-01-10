(ns leihs.admin.back.html
  (:refer-clojure :exclude [str keyword])
  (:require [leihs.admin.utils.core :refer [keyword str presence]])
  (:require
    [leihs.admin.env :refer [env]]
    [leihs.admin.resources.user.back :as user]
    [leihs.admin.utils.http-resources-cache-buster :as cache-buster :refer [wrap-resource]]
    [leihs.admin.utils.json-protocol :refer [to-json]]
    [leihs.admin.utils.url.core :as url]

    [clojure.java.jdbc :as jdbc]
    [hiccup.page :refer [include-js html5]]

    [clojure.tools.logging :as logging]
    [logbug.catcher :as catcher]
    [logbug.debug :as debug :refer [I>]]
    [logbug.ring :refer [wrap-handler-with-logging]]
    [logbug.thrown :as thrown]
    ))

(defn include-site-css []
  (hiccup.page/include-css
    (cache-buster/cache-busted-path "/admin/css/site.css")))

(defn include-font-css []
  (hiccup.page/include-css
    "/admin/css/fontawesome-free-5.0.1//css/fontawesome-all.css"))

(defn head []
  [:head
   [:meta {:charset "utf-8"}]
   [:meta {:name "viewport"
           :content "width=device-width, initial-scale=1, shrink-to-fit=no"}]
   (include-site-css)
   (include-font-css)])

(defn user-data [request]
  (url/encode
    (to-json
      (when-let [user-id (-> request :authenticated-entity :user_id)]
        (->> (user/user-query user-id)
             (jdbc/query (:tx request)) first)))))

(defn not-found-handler [request]
  {:status 404
   :headers {"Content-Type" "text/html"}
   :body (html5
           (head)
           [:body
            {:data-user (user-data request)}
            [:div.container-fluid
             [:h1.text-danger "Error 404 - Not Found"]]])})

(defn html-handler [request]
  {:headers {"Content-Type" "text/html"}
   :body (html5
           (head)
           [:body
            {:data-user (user-data request)}
            [:div#app.container-fluid
             [:div.alert.alert-warning
              [:h1 "Leihs Admin2"]
              [:p "This application requires Javascript."]]]
            (hiccup.page/include-js
              (cache-buster/cache-busted-path "/admin/js/app.js"))])})
