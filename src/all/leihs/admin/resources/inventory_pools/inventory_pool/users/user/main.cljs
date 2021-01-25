(ns leihs.admin.resources.inventory-pools.inventory-pool.users.user.main
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go]])
  (:require
     [leihs.core.core :refer [keyword str presence]]
     [leihs.core.requests.core :as requests]
     [leihs.core.routing.front :as routing]
     [leihs.core.icons :as icons]

     [leihs.admin.common.components :as components]
     [leihs.admin.state :as state]
     [leihs.admin.paths :as paths :refer [path]]
     [leihs.admin.resources.inventory-pools.inventory-pool.core :as inventory-pool]
     [leihs.admin.resources.inventory-pools.inventory-pool.roles :refer []]
     [leihs.admin.resources.inventory-pools.inventory-pool.users.user.breadcrumbs :as breadcrumbs]
     [leihs.admin.resources.inventory-pools.inventory-pool.users.user.direct-roles.main :as direct-roles]
     [leihs.admin.resources.inventory-pools.inventory-pool.users.user.groups-roles.main :as groups-roles]
     [leihs.admin.resources.inventory-pools.inventory-pool.users.user.roles.main :as user-roles]
     [leihs.admin.resources.inventory-pools.inventory-pool.users.user.suspension.main :as suspension]
     [leihs.admin.resources.users.user.core :as user :refer [user-id* user-data*]]
     [leihs.admin.resources.users.user.shared :as user-shared]
     [leihs.admin.utils.regex :as regex]

     [accountant.core :as accountant]
     [cljs.core.async :as async]
     [cljs.pprint :refer [pprint]]
     [reagent.core :as reagent]))

(defonce inventory-pool-user-data* (reagent/atom nil))

(defn debug-component []
  (when (:debug @state/global-state*)
    [:section.debug
     [:hr]
     [:h2 "Page Debug"]
     [:div
      [:h3 "@user-data*"]
      [:pre (with-out-str (pprint @user-data*))]]
     [:div
      [:h3 "@suspension/data*"]
      [:pre (with-out-str (pprint @suspension/data*))]]]))

(defn name-component []
  [:span
   [routing/hidden-state-component {:did-mount user/clean-and-fetch}]
   (let [p (path :inventory-pool-user {:inventory-pool-id @inventory-pool/id*
                                       :user-id @user-id*})
         name-or-id (user/fullname-or-some-uid @user-data*)]
     [components/link [:em name-or-id] p])])


;;; suspension ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn suspension-component []
  [:div#suspension
   [:h2
    " Suspension "
    [:a.btn.btn-outline-primary
     {:href (path :inventory-pool-user-suspension
                  {:inventory-pool-id @inventory-pool/id*
                   :user-id @user-id*})}
     icons/edit (if @suspension/suspended?* " Edit " " Suspend ")]
    (when @suspension/suspended?*
      [:button.btn.btn-warning.btn.mx-2
       {:on-click #(suspension/cancel {:id @user-id*} suspension/clean-and-fetch)}
       icons/delete " Cancel suspension "])]
   [suspension/suspension-component]])


;;; roles ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn effective-roles-component []
  [:div.effective-roles
   [routing/hidden-state-component
    {:did-mount user-roles/clean-and-fetch
     :did-change user-roles/clean-and-fetch}]
   [:h2 "Roles"]
   [:p "This section shows the effective roles. This is an aggregate computed from "
    "direct roles, and roles via groups. "]
   [user-roles/roles-component]])

(defn direct-roles-component []
  [:div.direct-roles
   [routing/hidden-state-component
    {:did-mount direct-roles/clean-and-fetch
     :did-change direct-roles/clean-and-fetch}]
   [:h3
    [:span " Direct roles "]
    [:a.btn.btn-outline-primary {:href (path :inventory-pool-user-direct-roles
                                             {:inventory-pool-id @inventory-pool/id*
                                              :user-id @user-id*})}
     [:span icons/edit "Edit"]]]
   [direct-roles/roles-component]])

(defn roles-via-groups-component [user]
  [:div.roles-via-groups
   [:h3 "Roles via groups "
    [:a.btn.btn-outline-primary
     {:href (path :inventory-pool-groups
                  {:inventory-pool-id @inventory-pool/id*}
                  {:including-user (or (-> user :email presence) (:id user))}
                  )}

     icons/add " Add group role "]]
   [groups-roles/groups-roles-component]])

(defn roles-component []
  [:div
   [effective-roles-component]
   [:div.mt-3]
   [direct-roles-component]
   [:div.mt-3]
   [roles-via-groups-component @user-data*]])


;;; overview ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn pool-data-li-dl-component [dt dd]
  ^{:key dt}
  [:li {:key dt}
   [:dl.row.mb-0
    [:dt.col-sm-5 dt]
    [:dd.col-sm-7 dd]]])

(defn not-zero-or-nil [n]
  (when-not (= n 0) n))

(defn not-zero-count-pool-data-li-dl-component [user dt dd-key]
  (when-let [c (-> user
                   dd-key
                   not-zero-or-nil)]
    [pool-data-li-dl-component dt c]))

(defn overview-pool-data-component [user]
  [:div
   [:ul.list-unstyled
    [pool-data-li-dl-component "Suspended"
     (if @suspension/suspended?*
       [:span.text-danger "yes"]
       [:span.text-success "no"])]
    [pool-data-li-dl-component "Roles"
     [user-roles/roles-component]]
    [not-zero-count-pool-data-li-dl-component user
     "Submitted reservations" :reservations_submitted_count ]
    [not-zero-count-pool-data-li-dl-component user
     "Approved reservations" :reservations_approved_count]
    [not-zero-count-pool-data-li-dl-component user
     "Open contracts" :contracts_open_count]
    [not-zero-count-pool-data-li-dl-component user
     "Closed contracts" :contracts_closed_count]]])

(defn overview-component []
  [:div.overview
   [:h2 "Overview"]
   [:div.row
    [:div.col-md
     [:h3 " Image / Avatar "]
     [user/img-avatar-component @user-data*]]
    [:div.col-md
     [:h3 "Personal Properties"]
     [user/personal-properties-component @user-data*]]
    [:div.col-md
     [:h3 "Account Properties"]
     [user/account-properties-component @user-data*] ]
    [:div.col-md
     [:h3 "Pool Related Properties"]
     [overview-pool-data-component @user-data*]]]])


;;; page ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn page-title-component []
  [:h1 "User "
   [name-component]
   " in the Inventory-Pool "
   [inventory-pool/name-link-component]])

(defn page []
  [:div.user-roles
   [breadcrumbs/nav-component
    @breadcrumbs/left*
    [[breadcrumbs/user-data-li]
     [breadcrumbs/direct-roles-li]
     [breadcrumbs/suspension-li]]]
   [page-title-component]
   [:hr]
   [overview-component]
   [:hr]
   [suspension-component]
   [:hr]
   [roles-component]
   [:hr]
   [:div
    [:h2 "Extended User Info"]
    (when-let [ext-info (-> @user-data* :extended_info presence)]
      [:pre (.stringify js/JSON (.parse js/JSON ext-info) nil 2)])]

   [debug-component]])
