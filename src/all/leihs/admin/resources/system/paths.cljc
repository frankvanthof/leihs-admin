(ns leihs.admin.resources.system.paths
  (:refer-clojure :exclude [str keyword])
  (:require
    [leihs.core.core :refer [keyword str presence]]
    [leihs.admin.resources.system.system-admins.paths :as system-admins-paths]
    [leihs.admin.resources.system.authentication-systems.paths :as authentication-systems]

    [bidi.verbose :refer [branch param leaf]]

    ))

(def paths
  (branch "/system"
          (leaf "/" :system)
          (branch "/database"
                  (leaf "/" :database))
          authentication-systems/paths
          system-admins-paths/paths))
