(ns leihs.admin.paths
  (:refer-clojure :exclude [str keyword])
  (:require
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.paths]
    [leihs.core.url.query-params :as query-params]

    [leihs.admin.resources.system.paths :as system]
    [leihs.admin.resources.inventory-pools.paths :as inventory-pools]
    [leihs.admin.resources.inventory.paths :as inventory]

    [bidi.verbose :refer [branch param leaf]]
    [bidi.bidi :refer [path-for match-route]]

    #?@(:clj
         [[uritemplate-clj.core :as uri-templ]
          [clojure.tools.logging :as logging]
          [logbug.catcher :as catcher]
          [logbug.debug :as debug]
          [logbug.thrown :as thrown]
          ])))

(def external-handlers
  #{:admin-audits-legacy
    :admin-buildings
    :admin-fields
    :admin-languages
    :admin-mail-templates
    :admin-rooms
    :admin-settings
    :admin-statistics
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

(def delegation-paths
  (branch "/delegations"
          (leaf "/" :delegations)
          (branch "/add"
                  (leaf "" :delegation-add)
                  (leaf "/choose-responsible-user" :delegation-add-choose-responsible-user))
          (branch "/"
                  (param :delegation-id)
                  (leaf "" :delegation)
                  (leaf "/delete" :delegation-delete)
                  (branch "/edit"
                          (leaf "" :delegation-edit)
                          (leaf "/choose-responsible-user" :delegation-edit-choose-responsible-user))
                  (branch "/users"
                          (leaf "/" :delegation-users)
                          (leaf "/add" :delegation-users-add)
                          (branch "/"
                                  (param :user-id)
                                  (leaf "" :delegation-user))))))

(def users-paths
  (branch "/users"
          (branch "/"
                  (leaf "" :users)
                  (leaf "new" :user-new))
          (branch "/"
                  (param :user-id)
                  (leaf "" :user)
                  (leaf "/delete" :user-delete)
                  (leaf "/edit" :user-edit)
                  (leaf "/inventory-pools-roles/" :user-inventory-pools-roles)
                  (branch "/transfer/"
                          (param :target-user-id)
                          (leaf "" :user-transfer-data)))))

(def groups-paths
  (branch "/groups"
          (branch "/"
                  (leaf "" :groups)
                  (leaf "add" :group-add))
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


(def paths
  (branch ""
          leihs.core.paths/core-paths
          (branch "/admin"
                  (leaf "/status" :status)
                  (leaf "/debug" :debug)
                  delegation-paths
                  groups-paths
                  system/paths
                  inventory/paths
                  inventory-pools/paths
                  users-paths
                  (leaf "/audits" :admin-audits-legacy)
                  (leaf "/buildings" :admin-buildings)
                  (leaf "/fields_editor" :admin-fields)
                  (leaf "/languages" :admin-languages)
                  (leaf "/mail_templates" :admin-mail-templates)
                  (leaf "/rooms" :admin-rooms)
                  (leaf "/settings" :admin-settings)
                  (leaf "/statistics" :admin-statistics)
                  (leaf "/suppliers" :admin-suppliers)
                  )))


(reset! leihs.core.paths/paths* paths)

(def path leihs.core.paths/path)

;(path :system-admins-direct-user {:user-id "foo"})
;(path :user-inventory-pools-roles {:user-id "123"})
