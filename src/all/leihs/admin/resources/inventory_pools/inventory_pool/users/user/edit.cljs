(ns leihs.admin.resources.inventory-pools.inventory-pool.users.user.edit
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go]])
  (:require
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.requests.core :as requests]
    [leihs.core.routing.front :as routing]
    [leihs.core.icons :as icons]

    [leihs.admin.utils.misc :as front-shared :refer [wait-component]]
    [leihs.admin.state :as state]
    [leihs.admin.paths :as paths :refer [path]]
    [leihs.admin.resources.users.user.core :as core :refer [user-id*]]
    [leihs.admin.resources.users.user.edit-core :as edit-core :refer [data*]]
    [leihs.admin.resources.users.user.edit-main :as edit-main]
    [leihs.admin.resources.inventory-pools.inventory-pool.core :as inventory-pool]
    [leihs.admin.resources.inventory-pools.inventory-pool.users.user.breadcrumbs :as breadcrumbs]

    [accountant.core :as accountant]
    [cljs.core.async :as async :refer [timeout]]
    [cljs.pprint :refer [pprint]]
    [reagent.core :as reagent]

    [taoensso.timbre :as logging]
    ))

(defn page []
  [:div.user-data
   [routing/hidden-state-component {:did-mount edit-main/clean-and-fetch}]
   [breadcrumbs/nav-component
    (conj @breadcrumbs/left* [breadcrumbs/edit-li])[]]
   [:h1
    "Edit User "
    [core/name-component @data*]
    " in the Inventory-Pool "
    [inventory-pool/name-link-component]]
   (if (not @data*)
     [wait-component]
     [edit-main/edit-form-component (fn [e]
                                      (.preventDefault e)
                                      (edit-main/patch
                                        (path :inventory-pool-user
                                              {:inventory-pool-id @inventory-pool/id*
                                               :user-id @user-id*})))])
   [edit-core/debug-component]])
