(ns leihs.admin.resources.shutdown.back
  (:refer-clojure :exclude [str keyword])
  (:require [leihs.admin.utils.core :refer [keyword str presence]])
  (:require
    [leihs.admin.paths :refer [path]]
    [leihs.admin.utils.sql :as sql]
    [leihs.admin.utils.ds :refer [ds]]
    [leihs.admin.resources.user.back :refer [password-hash]]

    [clojure.java.jdbc :as jdbc]
    [compojure.core :as cpj]
    [ring.util.response :refer [redirect]]

    [clojure.tools.logging :as logging]
    [logbug.debug :as debug])
  (:import
    [java.util UUID]
    ))

(defn- shutdown [request]
  (do (future (Thread/sleep 250)
              (System/exit 0))
      {:status 204}))

(def routes
  (cpj/routes
    (cpj/POST (path :shutdown) [] #'shutdown)))
