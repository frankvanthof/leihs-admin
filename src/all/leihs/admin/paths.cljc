(ns leihs.admin.paths
  (:refer-clojure :exclude [str keyword])
  (:require
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.paths]
    [leihs.core.url.query-params :as query-params]

    [leihs.admin.resources.settings.paths :as settings]
    [leihs.admin.resources.audits.paths :as audits]
    [leihs.admin.resources.inventory-pools.inventory-pool.delegations.paths :as delegations]
    [leihs.admin.resources.inventory-pools.paths :as inventory-pools]
    [leihs.admin.resources.inventory.paths :as inventory]
    [leihs.admin.resources.system.paths :as system]

    [bidi.verbose :refer [branch param leaf]]
    [bidi.bidi :refer [path-for match-route]]

    #?@(:clj
         [[uritemplate-clj.core :as uri-templ]
          [clojure.tools.logging :as logging]
          [logbug.catcher :as catcher]
          [logbug.debug :as debug]
          [logbug.thrown :as thrown]])
    #?@(:cljs
         [[taoensso.timbre :as logging]])))

(def external-handlers
  #{:admin-audits-legacy
    :admin-buildings
    :admin-fields
    :admin-languages
    :admin-mail-templates
    :admin-rooms
    :admin-settings
    :admin-suppliers
    :borrow
    :home
    :inventory-csv
    :inventory-excel
    :inventory-quick-csv
    :inventory-quick-excel
    :lending
    :my-user
    :procurement
    })

(def users-paths
  (branch "/users"
          (branch "/"
                  (leaf "" :users)
                  (branch "/"
                          (leaf "new" :user-create)
                          (leaf "choose" :users-choose)))
          (branch "/"
                  (param [#"[^/]+" :user-id])
                  (leaf "" :user)
                  (leaf "/delete" :user-delete)
                  (leaf "/edit" :user-edit)
                  (leaf "/inventory-pools/" :user-inventory-pools)
                  (branch "/transfer/"
                          (param [#"[^/]+" :target-user-uid])
                          (leaf "" :user-transfer-data)))))

(def groups-paths
  (branch "/groups"
          (branch "/"
                  (leaf "" :groups)
                  (leaf "create" :group-create))
          (branch "/"
                  (param :group-id)
                  (leaf "" :group)
                  (leaf "/delete" :group-delete)
                  (leaf "/edit" :group-edit)
                  (leaf "/inventory-pools-roles/" :group-inventory-pools-roles)
                  (branch "/users"
                          (leaf "/" :group-users)
                          (branch "/"
                                  (param :user-id)
                                  (leaf "" :group-user))))))

(def statistics-paths
  (branch "/statistics/"
          (leaf  "" :statistics)
          (leaf "basic/" :statistics-basic)))

(def paths
  (branch ""
          leihs.core.paths/core-paths
          (branch "/admin"
                  (leaf "/status" :status)
                  (leaf "/debug" :debug)
                  audits/paths
                  groups-paths
                  inventory-pools/paths
                  inventory/paths
                  settings/paths
                  system/paths
                  users-paths
                  statistics-paths
                  (leaf "/audits" :admin-audits-legacy)
                  (leaf "/buildings" :admin-buildings)
                  (leaf "/fields_editor" :admin-fields)
                  (leaf "/languages" :admin-languages)
                  (leaf "/mail_templates" :admin-mail-templates)
                  (leaf "/rooms" :admin-rooms)
                  (leaf "/settings" :admin-settings)
                  (leaf "/suppliers" :admin-suppliers)
                  )))


(reset! leihs.core.paths/paths* paths)

(defn path [& args]
  (try
    (apply leihs.core.paths/path args)
    #?(:cljs
       (catch :default e
         (logging/error e args)
         (throw e)))))

;(path :system-admins-direct-user {:user-id "foo"})
;(path :user-inventory-pools {:user-id "123"})
