(ns leihs.admin.resources.inventory-pools.inventory-pool.users.front
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go]])
  (:require
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.requests.core :as requests]
    [leihs.core.routing.front :as routing]

    [leihs.admin.front.breadcrumbs :as breadcrumbs]
    [leihs.admin.front.components :as components]
    [leihs.core.icons :as icons]
    [leihs.admin.front.shared :refer [humanize-datetime-component gravatar-url]]
    [leihs.admin.front.state :as state]
    [leihs.admin.paths :as paths :refer [path]]
    [leihs.admin.resources.inventory-pools.inventory-pool.front :as inventory-pool :refer [inventory-pool-id*]]
    [leihs.admin.resources.inventory-pools.inventory-pool.roles :refer [roles-hierarchy]]
    [leihs.admin.resources.users.front :as users]
    [leihs.admin.utils.regex :as regex]

    [clojure.contrib.inflect :refer [pluralize-noun]]
    [accountant.core :as accountant]
    [cljs.core.async :as async]
    [cljs.core.async :refer [timeout]]
    [cljs.pprint :refer [pprint]]
    [cljsjs.jimp]
    [cljsjs.moment]
    [clojure.contrib.inflect :refer [pluralize-noun]]
    [reagent.core :as reagent]))


(def inventory-pool-users-count*
  (reaction (-> @users/data*
                (get (:url @routing/state*) {})
                :inventory-pool_users_count)))

;### roles ####################################################################

(defn roles-th-component []
  [:th {:key :roles} " Roles "])

(defn roles-td-component [user]
  [:td {:key :roles}
   [:a
    {:href (path :inventory-pool-user
                 {:inventory-pool-id @inventory-pool-id* :user-id (:id user)})}
    (or (->> user :roles
             (into [])
             (filter second)
             (map first)
             (map str)
             (clojure.string/join ", ")
             presence)
        "none")]])


;### suspended ################################################################

(defn suspension-th-component []
  [:th " Suspension "])

(defn suspension-td-component [user]
  [:td
   [:a
    {:href (path :inventory-pool-user {:inventory-pool-id @inventory-pool-id* :user-id (:id user)})}

    (if-let [suspended_until (some-> user :suspension :suspended_until js/moment)]
      [:span.text-danger
       [:span
        (if (.isAfter suspended_until "2098-01-01")
          "forever"
          (.format suspended_until "YYYY-MM-DD"))]]
      [:span.text-success
       "unsuspended" ])]])


;### actions ##################################################################

(def colconfig
  (merge users/default-colconfig
         {:email false
          :org_id false
          :customcols [
                       {:key :roles
                        :th roles-th-component
                        :td roles-td-component}
                       {:key :suspension
                        :th suspension-th-component
                        :td suspension-td-component}]}))


;### filter ###################################################################


(defn form-role-filter []
  (let [role (or (-> @users/current-query-paramerters-normalized* :role presence) " ")]
    [:div.form-group.ml-2.mr-2.mt-2
     [:label.mr-1 {:for :users-filter-role} " Role "]
     [:select#users-filter-role.form-control
      {:value role
       :on-change (fn [e]
                    (let [val (or (-> e .-target .-value presence) "")]
                      (accountant/navigate! (users/page-path-for-query-params
                                              {:page 1
                                               :role val}))))}
      ;(console.log (clj->js roles-hierarchy))
      (doall (for [a (concat ["any" "none"] roles-hierarchy)]
               [:option {:key a :value a} a]))]]))


(defn form-suspension-filter []
  (let [suspended (-> @users/current-query-paramerters-normalized* :suspended presence boolean)]
    [:div.form-group.ml-2.mr-2.mt-2
     [:label.mr-1 {:for :users-suspended-filter} " Suspended "]
     [:input#users-suspended-filter
      {:type :checkbox
       :checked suspended
       :on-change
       #(accountant/navigate! (users/page-path-for-query-params
                                {:page 1
                                 :suspended (not suspended)}))}]]))

(defn filter-component []
  [:div.card.bg-light
   [:div.card-body
   [:div.form-inline
    [users/form-term-filter]
    [form-role-filter]
    [form-suspension-filter]
    [users/form-per-page]
    [users/form-reset]]]])


;### main #####################################################################

(defn debug-component []
  (when (:debug @state/global-state*)
    [:div
     [:div "@inventory-pool-users-count*"
      [:pre (with-out-str (pprint @inventory-pool-users-count*))]]]))

(defn main-page-component []
  [:div
   [routing/hidden-state-component
    {:did-mount users/escalate-query-paramas-update
     :did-update users/escalate-query-paramas-update}]
   [filter-component]
   [users/pagination-component]
   [users/users-table-component colconfig]
   [users/pagination-component]
   [debug-component]
   [users/debug-component]])

(defn index-page []
  [:div.inventory-pool-users
   [routing/hidden-state-component
    {:did-mount (fn [_] (inventory-pool/clean-and-fetch))}]
   (breadcrumbs/nav-component
     [(breadcrumbs/leihs-li)
      (breadcrumbs/admin-li)
      (breadcrumbs/inventory-pools-li)
      (breadcrumbs/inventory-pool-li @inventory-pool/inventory-pool-id*)
      (breadcrumbs/inventory-pool-users-li @inventory-pool/inventory-pool-id*)]
     [])
   [:div
    [:h1
     (let [c (or @inventory-pool-users-count* 0)]
       [:span c " " (pluralize-noun c "User")
        [:span " in Inventory-Pool "]
        [inventory-pool/inventory-pool-name-component]])]
    [main-page-component]]])
