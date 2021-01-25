(ns leihs.admin.routes
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    )
  (:require
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.routing.front :as routing]
    [leihs.core.url.query-params :as query-params]

    [leihs.admin.paths :as paths :refer [path paths]]

    [leihs.admin.resources.audits.main :as audits]
    [leihs.admin.resources.audits.changes.main :as audited-changes]
    [leihs.admin.resources.audits.changes.change.main :as audited-change]
    [leihs.admin.resources.audits.requests.main :as audited-requests]
    [leihs.admin.resources.audits.requests.request.main :as audited-request]

    [leihs.admin.resources.groups.group.create :as group-create]
    [leihs.admin.resources.groups.group.del :as group-delete]
    [leihs.admin.resources.groups.group.edit :as group-edit]
    [leihs.admin.resources.groups.group.show :as group-show]
    [leihs.admin.resources.groups.group.users.main :as group-users]
    [leihs.admin.resources.groups.main :as groups]
    [leihs.admin.resources.inventory-pools.inventory-pool.entitlement-groups.entitlement-group.main :as inventory-pool-entitlement-group]
    [leihs.admin.resources.inventory-pools.inventory-pool.entitlement-groups.entitlement-group.users.main :as inventory-pool-entitlement-group-users]
    [leihs.admin.resources.inventory-pools.inventory-pool.delegations.delegation.edit :as delegation-edit]
    [leihs.admin.resources.inventory-pools.inventory-pool.delegations.delegation.groups.main :as delegation-groups]
    [leihs.admin.resources.inventory-pools.inventory-pool.delegations.delegation.main :as delegation]
    [leihs.admin.resources.inventory-pools.inventory-pool.delegations.delegation.users.main :as delegation-users]
    [leihs.admin.resources.inventory-pools.inventory-pool.delegations.main :as delegations]
    [leihs.admin.resources.inventory-pools.inventory-pool.entitlement-groups.entitlement-group.groups.main :as inventory-pool-entitlement-group-groups]
    [leihs.admin.resources.inventory-pools.inventory-pool.entitlement-groups.main :as inventory-pool-entitlement-groups]
    [leihs.admin.resources.inventory-pools.inventory-pool.groups.group.roles.main :as inventory-pool-group-roles]
    [leihs.admin.resources.inventory-pools.inventory-pool.groups.main :as inventory-pool-groups]
    [leihs.admin.resources.inventory-pools.inventory-pool.main :as inventory-pool]
    [leihs.admin.resources.inventory-pools.inventory-pool.users.main :as inventory-pool-users]
    [leihs.admin.resources.inventory-pools.inventory-pool.users.user.direct-roles.main :as inventory-pool-user-direct-roles]
    [leihs.admin.resources.inventory-pools.inventory-pool.users.user.create :as inventory-pool-user-create]
    [leihs.admin.resources.inventory-pools.inventory-pool.users.user.edit :as inventory-pool-user-edit]
    [leihs.admin.resources.inventory-pools.inventory-pool.users.user.main :as inventory-pool-user]
    [leihs.admin.resources.inventory-pools.inventory-pool.users.user.roles.main :as inventory-pool-user-roles]
    [leihs.admin.resources.inventory-pools.inventory-pool.users.user.suspension.main :as inventory-pool-user-suspension]
    [leihs.admin.resources.inventory-pools.main :as inventory-pools]
    [leihs.admin.resources.inventory.main :as inventory]

    [leihs.admin.resources.leihs-root :as home]
    [leihs.admin.resources.main :as admin]

    [leihs.admin.resources.statistics.main :as statistics]
    [leihs.admin.resources.status.main :as status]

    [leihs.admin.resources.settings.main :as settings]
    [leihs.admin.resources.settings.languages.main :as languages-settings]
    [leihs.admin.resources.settings.misc.main :as misc-settings]
    [leihs.admin.resources.settings.smtp.main :as smtp-settings]
    [leihs.admin.resources.settings.syssec.main :as syssec-settings]

    [leihs.admin.resources.system.authentication-systems.authentication-system.groups.main :as authentication-system-groups]
    [leihs.admin.resources.system.authentication-systems.authentication-system.main :as authentication-system]
    [leihs.admin.resources.system.authentication-systems.authentication-system.users.main :as authentication-system-users]
    [leihs.admin.resources.system.authentication-systems.main :as authentication-systems]
    [leihs.admin.resources.system.main :as system]
    [leihs.admin.resources.system.system-admins.main :as system-admins]
    [leihs.admin.resources.users.choose-main :as users-choose]
    [leihs.admin.resources.users.main :as users]
    [leihs.admin.resources.users.user.create :as user-create]
    [leihs.admin.resources.users.user.delete-main :as user-delete]
    [leihs.admin.resources.users.user.edit-main :as user-edit]
    [leihs.admin.resources.users.user.show :as user-show]

    [accountant.core :as accountant]
    [bidi.bidi :as bidi]
    [clojure.pprint :refer [pprint]]
    [reagent.core :as reagent]
    ))

(def resolve-table
  {:admin #'admin/page
   :audited-change #'audited-change/page
   :audited-changes #'audited-changes/page
   :audited-request #'audited-request/page
   :audited-requests #'audited-requests/page
   :audits #'audits/page
   :authentication-system #'authentication-system/show-page
   :authentication-system-create #'authentication-system/create-page
   :authentication-system-delete #'authentication-system/delete-page
   :authentication-system-edit #'authentication-system/edit-page
   :authentication-system-groups #'authentication-system-groups/page
   :authentication-system-users #'authentication-system-users/page
   :authentication-systems #'authentication-systems/page
   :group #'group-show/page
   :group-create #'group-create/page
   :group-delete #'group-delete/page
   :group-edit #'group-edit/page
   :group-users #'group-users/index-page
   :groups #'groups/page
   :home #'home/page
   :inventory #'inventory/page
   :inventory-pool #'inventory-pool/show-page
   :inventory-pool-create #'inventory-pool/create-page
   :inventory-pool-delegation #'delegation/show-page
   :inventory-pool-delegation-create #'delegation-edit/new-page
   :inventory-pool-delegation-edit #'delegation-edit/edit-page
   :inventory-pool-delegation-groups #'delegation-groups/page
   :inventory-pool-delegation-users #'delegation-users/index-page
   :inventory-pool-delegations #'delegations/page
   :inventory-pool-delete #'inventory-pool/delete-page
   :inventory-pool-edit #'inventory-pool/edit-page
   :inventory-pool-entitlement-group #'inventory-pool-entitlement-group/page
   :inventory-pool-entitlement-group-groups #'inventory-pool-entitlement-group-groups/page
   :inventory-pool-entitlement-group-users inventory-pool-entitlement-group-users/page
   :inventory-pool-entitlement-groups #'inventory-pool-entitlement-groups/index-page
   :inventory-pool-group-roles #'inventory-pool-group-roles/page
   :inventory-pool-groups #'inventory-pool-groups/index-page
   :inventory-pool-user #'inventory-pool-user/page
   :inventory-pool-user-create #'inventory-pool-user-create/page
   :inventory-pool-user-edit #'inventory-pool-user-edit/page
   :inventory-pool-user-direct-roles #'inventory-pool-user-direct-roles/page
   :inventory-pool-user-roles #'inventory-pool-user-roles/page
   :inventory-pool-user-suspension #'inventory-pool-user-suspension/page
   :inventory-pool-users #'inventory-pool-users/index-page
   :inventory-pools #'inventory-pools/page
   :languages-settings #'languages-settings/page
   :misc-settings #'misc-settings/page
   :settings #'settings/page
   :smtp-settings #'smtp-settings/page
   :statistics #'statistics/page
   :status #'status/info-page
   :syssec-settings #'syssec-settings/page
   :system #'system/page
   :system-admins #'system-admins/page
   :user #'user-show/page
   :user-create #'user-create/page
   :user-delete #'user-delete/page
   :user-edit #'user-edit/page
   :users #'users/page
   :users-choose #'users-choose/page })

(defn init []
  (routing/init paths resolve-table paths/external-handlers))
