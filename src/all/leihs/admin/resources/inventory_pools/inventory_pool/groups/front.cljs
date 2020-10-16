(ns leihs.admin.resources.inventory-pools.inventory-pool.groups.front
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
    [leihs.admin.resources.inventory-pools.inventory-pool.roles :refer [roles-component roles-hierarchy]]
    [leihs.admin.resources.groups.front :as groups]
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




;### roles ####################################################################

(def roles-th-component [:th {:key :roles} " Roles "])

(defn roles-td-component [group]
  (let [path (path :inventory-pool-group-roles
                   {:inventory-pool-id @inventory-pool-id*
                    :group-id (:id group)})
        has-a-role? (some->> group :roles vals (reduce #(or %1 %2)))]
    [:td {:key :roles}
     [roles-component group false nil]
     [:a.btn.btn-outline-primary.btn-sm {:href path}
      (if has-a-role?
        [:span icons/edit " edit "]
        [:span icons/add " add "])]]))

;### actions ##################################################################


;### filter ###################################################################


(defn form-role-filter []
  (let [role (or (-> @groups/current-query-paramerters-normalized* :role presence) "")]
    [:div.form-group.ml-2.mr-2.mt-2
     [:label.mr-1 {:for :groups-filter-role} " Role "]
     [:select#groups-filter-role.form-control
      {:value role
       :on-change (fn [e]
                    (let [val (or (-> e .-target .-value presence) "")]
                      (accountant/navigate! (groups/page-path-for-query-params
                                              {:page 1
                                               :role val}))))}
      (for [a  (concat ["any" "none"] roles-hierarchy)]
        [:option {:key a :value a} a])]]))


(defn filter-component []
  [:div.card.bg-light
   [:div.card-body
   [:div.form-row
    [groups/form-term-filter]
    [groups/form-including-user-filter]
    [form-role-filter]
    [routing/form-per-page-component]
    [routing/form-reset-component]]]])


;### main #####################################################################

(defn debug-component []
  (when (:debug @state/global-state*)
    [:div ]))

(defn main-page-component []
  [:div
   [routing/hidden-state-component
    {:did-mount groups/escalate-query-paramas-update
     :did-update groups/escalate-query-paramas-update}]
   [filter-component]
   [routing/pagination-component]
   [groups/groups-table-component
    [roles-th-component]
    [roles-td-component]]
   [routing/pagination-component]
   [debug-component]
   [groups/debug-component]])

(defn index-page []
  [:div.inventory-pool-groups
   [routing/hidden-state-component
    {:did-mount (fn [_] (inventory-pool/clean-and-fetch))}]
   (breadcrumbs/nav-component
     [(breadcrumbs/leihs-li)
      (breadcrumbs/admin-li)
      (breadcrumbs/inventory-pools-li)
      (breadcrumbs/inventory-pool-li @inventory-pool-id*)
      (breadcrumbs/inventory-pool-groups-li @inventory-pool-id*)]
     [])
   [:div
    [:h1
     "Roles of groups "
     [:span " in the inventory-pool "]
     [:a {:href (path :inventory-pool {:inventory-pool-id @inventory-pool-id*})}
      [inventory-pool/name-component]]]
    [main-page-component] ]])
