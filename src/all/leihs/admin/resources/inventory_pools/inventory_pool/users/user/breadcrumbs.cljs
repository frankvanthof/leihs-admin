(ns leihs.admin.resources.inventory-pools.inventory-pool.users.user.breadcrumbs
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go]])
  (:require
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.auth.core :as auth]
    ;[leihs.core.breadcrumbs :as breadcrumbs-core :refer [li]]
    [leihs.core.icons :as icons]
    [leihs.core.routing.front :as routing]

    [leihs.admin.common.breadcrumbs :as breadcrumbs]
    [leihs.admin.resources.inventory-pools.inventory-pool.users.breadcrumbs :as breadcrumbs-parent]
    [leihs.admin.paths :as paths :refer [path]]
    [leihs.admin.resources.inventory-pools.authorization :as pool-auth]

    [cljs.pprint :refer [pprint]]
    [cljs.core.async :as async :refer [timeout]]
    [clojure.string :refer [split trim]]
    [reagent.core :as reagent]
    [taoensso.timbre :as logging]))


(def li breadcrumbs-parent/li)
(def nav-component breadcrumbs-parent/nav-component)

(def inventory-pool-id* breadcrumbs-parent/inventory-pool-id*)

(defonce user-id*
  (reaction (or (some-> @routing/state* :route-params :user-id)
                ":user-id")))

(def route-params-default* (reaction
                     {:inventory-pool-id @inventory-pool-id*
                      :user-id @user-id*}))

(def authorizers-default [auth/admin-scopes? pool-auth/pool-lending-manager?])

(def create-li breadcrumbs-parent/create-li)

(defn edit-li []
  [li :inventory-pool-user-edit [:span [:i.fas.fa-edit] " User-Data "]
   {:inventory-pool-id @inventory-pool-id*
    :user-id @user-id*} {}
   :button true
   :authorizers [auth/admin-scopes? pool-auth/pool-lending-manager?]])

(defn user-li []
  [li :inventory-pool-user [:span icons/user " User "]
   @route-params-default* {}
   :authorizers authorizers-default])

(defn roles-li []
  [li :inventory-pool-user-roles [:span icons/edit " Manage Roles "]
   @route-params-default* {}
   :authorizers authorizers-default])

(defn user-data-li []
  [li :inventory-pool-user-edit [:span icons/edit " User Data "]
   @route-params-default* {}
   :button true
   :authorizers authorizers-default])

(defn direct-roles-li []
  [li :inventory-pool-user-direct-roles [:span icons/edit " Direct Roles "]
   @route-params-default* {}
   :button true
   :authorizers authorizers-default])

(defn suspension-li []
  [li :inventory-pool-user-suspension [:span icons/edit " Suspension"]
   @route-params-default* {}
   :button true
   :authorizers authorizers-default])

(defonce left*
  (reaction
    (conj @breadcrumbs-parent/left*
          [user-li])))

