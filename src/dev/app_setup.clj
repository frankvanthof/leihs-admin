(ns app-setup
  (:require [leihs.admin.back.main :as main]
            [leihs.core.http-server :as http-server]))

(defn stop [] (http-server/stop))

(defn run [& args] (apply main/-main "run" args))
