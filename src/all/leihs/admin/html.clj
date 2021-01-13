(ns leihs.admin.html
  (:require [clj-http.client :as client]
            [leihs.admin.env :refer [env]]
            [hiccup.page :refer [html5 include-js]]
            [environ.core :refer [env] :rename {env system-env}]
            [leihs.admin.paths :refer [path]]
            [leihs.admin.utils.release-info :as release-info]
            [leihs.core
             [http-cache-buster2 :as cache-buster]
             [json :refer [to-json]]
             [ssr :as ssr]]
            [leihs.core.url.core :as url]


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
    "/admin/css/fontawesome-free-5.0.13/css/fontawesome-all.css"))

(defn head []
  [:head
   [:meta {:charset "utf-8"}]
   [:meta {:name "viewport"
           :content "width=device-width, initial-scale=1, shrink-to-fit=no"}]
   [:style "ol.breadcrumb.leihs-nav-right:empty {display: none}"]
   (include-site-css)
   (include-font-css)])

(defn body-attributes [request]
  {:data-user (some-> (:authenticated-entity request) to-json url/encode)
   :data-leihsadminversion (url/encode (to-json release-info/leihs-admin-version))
   :data-leihsversion (url/encode (to-json release-info/leihs-version))})

(defn not-found-handler [request]
  {:status 404
   :headers {"Content-Type" "text/html"}
   :body (html5
           (head)
           [:body
            (body-attributes request)
            [:div.container-fluid
             [:h1.text-danger "Error 404 - Not Found"]]])})

(defn html-handler [request]
  {:headers {"Content-Type" "text/html"}
   :body (html5
           (head)
           [:body (body-attributes request)
            [:div
             (if-not (:leihs-disable-global-navbar system-env)
               (ssr/render-navbar request {:admin false}))
             [:div#app
              [:div.alert.alert-warning
               [:h1 "Leihs Admin2"]
               [:p "This application requires Javascript."]]]]
            (hiccup.page/include-js (cache-buster/cache-busted-path
                                      "/admin/leihs-shared-bundle.js"))
            (hiccup.page/include-js
              (cache-buster/cache-busted-path "/admin/js/app.js"))])})


;#### debug ###################################################################
;(logging-config/set-logger! :level :debug)
;(logging-config/set-logger! :level :info)
;(debug/debug-ns *ns*)
