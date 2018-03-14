(ns leihs.admin.resources.settings.back
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


;#### settings ################################################################

(defn settings [{tx :tx :as request}]
  (when (= :json (-> request :accept :mime))
    {:body (->> ["SELECT * FROM settings"] (jdbc/query tx) first)}))

(def routes
  (cpj/routes
    (cpj/GET (path :settings) [] #'settings)))


;#### create initial settings #################################################

(defn settings! [tx]
  (or (->> ["SELECT * FROM settings"] (jdbc/query tx) first)
      (first (jdbc/insert! tx :settings {:id 0}))
      (throw (IllegalStateException. "No settings here!"))))

(defn system_settings! [tx]
  (or (->> ["SELECT * FROM system_settings"] (jdbc/query tx) first)
      (first (jdbc/insert! tx :system_settings {:id 0}))
      (throw (IllegalStateException. "No system_settings here!"))))

(defn wrap
  ([handler]
   (fn [request]
     (wrap handler request)))
  ([handler request]
   (handler (assoc request
                   :settings (settings! (:tx request))
                   :system_settings (system_settings! (:tx request))
                   ))))


;#### debug ###################################################################
;(logging-config/set-logger! :level :debug)
;(logging-config/set-logger! :level :info)
;(debug/debug-ns 'cider-ci.utils.shutdown)
(debug/debug-ns *ns*)
