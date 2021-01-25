(ns leihs.admin.resources.inventory-pools.paths

  (:refer-clojure :exclude [str keyword])
  (:require
    [leihs.admin.resources.inventory-pools.inventory-pool.delegations.paths :as delegations]
    [leihs.core.core :refer [keyword str presence]]
    [bidi.verbose :refer [branch param leaf]]
    ))

(def entitlement-groups-paths
  (branch "/entitlement-groups/"
          (leaf "" :inventory-pool-entitlement-groups)
          (branch "" (param :entitlement-group-id)
                  (leaf "" :inventory-pool-entitlement-group)
                  (leaf "/users/" :inventory-pool-entitlement-group-users)
                  (branch "/direct-users/"
                          (param :user-id)
                          (leaf  "" :inventory-pool-entitlement-group-direct-user))
                  (branch "/groups/"
                          (leaf "" :inventory-pool-entitlement-group-groups)
                          (branch ""
                                  (param :group-id)
                                  (leaf "" :inventory-pool-entitlement-group-group))))))

(def groups-paths
  (branch "/groups/"
          (leaf "" :inventory-pool-groups)
          (branch ""
                  (param :group-id)
                  (leaf "" :inventory-pool-group)
                  (leaf "/roles" :inventory-pool-group-roles))))


(def users-paths
  (branch "/users/"
          (leaf "" :inventory-pool-users)
          (leaf "new" :inventory-pool-user-create)
          (branch ""
                  (param :user-id)
                  (leaf "" :inventory-pool-user)
                  (leaf "/edit" :inventory-pool-user-edit)
                  (leaf "/roles" :inventory-pool-user-roles)
                  (leaf "/direct-roles" :inventory-pool-user-direct-roles)
                  (leaf "/groups-roles/" :inventory-pool-user-groups-roles)
                  (leaf "/suspension" :inventory-pool-user-suspension))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def paths
  (branch "/inventory-pools/"
          (leaf "" :inventory-pools)
          (leaf "create" :inventory-pool-create)
          (branch ""
                  (param :inventory-pool-id)
                  (leaf "" :inventory-pool)
                  (leaf "/delete" :inventory-pool-delete)
                  (leaf "/edit" :inventory-pool-edit)
                  users-paths
                  delegations/paths
                  groups-paths
                  entitlement-groups-paths)))
