(ns leihs.admin.resources.inventory-pools.inventory-pool.users.user.edit
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go]])
  (:require
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.routing.front :as routing]
    [leihs.core.icons :as icons]

    [leihs.admin.common.http-client.core :as http-client]
    [leihs.admin.paths :as paths :refer [path]]
    [leihs.admin.resources.inventory-pools.inventory-pool.core :as inventory-pool]
    [leihs.admin.resources.inventory-pools.inventory-pool.users.user.breadcrumbs :as breadcrumbs]
    [leihs.admin.resources.users.user.core :as core :refer [user-id*]]
    [leihs.admin.resources.users.user.edit-core :as edit-core :refer [data*]]
    [leihs.admin.resources.users.user.edit-main :as edit-main]
    [leihs.admin.state :as state]
    [leihs.admin.utils.misc :as front-shared :refer [wait-component]]

    [accountant.core :as accountant]
    [cljs.core.async :as async :refer [timeout]]
    [cljs.pprint :refer [pprint]]
    [reagent.core :as reagent]

    [taoensso.timbre :as logging]
    ))

(defn patch []
  (go (when
        (some->
          {:chan (async/chan)
           :url (path :user {:user-id @user-id*})
           :method :patch
           :json-params  (-> @data*
                             (update-in [:extended_info]
                                        (fn [s] (.parse js/JSON s))))}
          http-client/request :chan <!
          http-client/filter-success!)
        (accountant/navigate! (path :inventory-pool-user
                                    {:inventory-pool-id @inventory-pool/id*
                                     :user-id @user-id*})))))

(defn page []
  [:div.user-data
   [routing/hidden-state-component {:did-mount core/clean-and-fetch}]
   [breadcrumbs/nav-component
    (conj @breadcrumbs/left* [breadcrumbs/edit-li])[]]
   [:h1
    "Edit User "
    [core/name-component @data*]
    " in the Inventory-Pool "
    [inventory-pool/name-link-component]]
   (if (not @data*)
     [wait-component]
     [edit-main/edit-form-component :patch patch])
   [edit-core/debug-component]])
