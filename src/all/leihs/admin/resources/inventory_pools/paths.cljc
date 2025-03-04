(ns leihs.admin.resources.inventory-pools.paths

  (:refer-clojure :exclude [str keyword])
  (:require
    [leihs.core.core :refer [keyword str presence]]
    [bidi.verbose :refer [branch param leaf]]
    ))

(def paths
  (branch "/inventory-pools/"
          (leaf "" :inventory-pools)
          (leaf "add" :inventory-pool-add)
          (branch ""
                  (param :inventory-pool-id)
                  (leaf "" :inventory-pool)
                  (leaf "/delete" :inventory-pool-delete)
                  (leaf "/edit" :inventory-pool-edit)
                  (branch "/users/"
                          (leaf "" :inventory-pool-users)
                          (branch ""
                                  (param :user-id)
                                  (leaf "" :inventory-pool-user)
                                  (leaf "/roles" :inventory-pool-user-roles)
                                  (leaf "/direct-roles" :inventory-pool-user-direct-roles)
                                  (leaf "/groups-roles/" :inventory-pool-user-groups-roles)
                                  (leaf "/suspension" :inventory-pool-user-suspension)))
                  (branch "/groups/"
                          (leaf "" :inventory-pool-groups)
                          (branch ""
                                  (param :group-id)
                                  (leaf "" :inventory-pool-group)
                                  (leaf "/roles" :inventory-pool-group-roles))))))
