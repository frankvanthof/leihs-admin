(ns leihs.admin.resources.group.users.front
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
    [leihs.admin.resources.group.front :as group :refer [group-id*]]
    [leihs.admin.resources.group.users.shared :refer [group-users-filter-value]]
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


(def group-users-count*
  (reaction (-> @users/data*
                (get (:url @routing/state*) {})
                :group_users_count)))

;### actions ##################################################################

(defn add-user [user-id]
  (let [resp-chan (async/chan)
        id (requests/send-off
             {:url (path :group-user {:group-id @group-id* :user-id user-id})
              :method :put
              :query-params {}}
             {:modal true
              :title "Add user"
              :handler-key :group-users
              :retry-fn #(add-user user-id)}
             :chan resp-chan)]
    (go (let [resp (<! resp-chan)]
          (when (and (= (:status resp) 204))
            (users/fetch-users))))))

(defn remove-user [user-id]
  (let [resp-chan (async/chan)
        id (requests/send-off
             {:url (path :group-user {:group-id @group-id* :user-id user-id})
              :method :delete
              :query-params {}}
             {:modal true
              :title "Remove user"
              :handler-key :group-users
              :retry-fn #(remove-user user-id)}
             :chan resp-chan)]
    (go (let [resp (<! resp-chan)]
          (when (and (= (:status resp) 204))
            (users/fetch-users))))))

(defn action-th-component []
  [:th "Add or remove from this group"])

(defn action-td-component [user]
  [:td
   (if (:group_id user)
     [:button.btn.btn-sm.btn-danger
      {:on-click (fn [_] (remove-user (:id user)))}
      icons/delete " Remove "]
     [:button.btn.btn-sm.btn-primary
      {:on-click (fn [_] (add-user (:id user)))}
      icons/add " Add "])])

(def colconfig
  (merge users/default-colconfig
         {:email false
          :customcols [{:key :action
                        :th action-th-component
                        :td action-td-component}]}))


;### filter ###################################################################

(defn group-users-filter-on-change [& args]
  (accountant/navigate!
    (users/page-path-for-query-params
      {:page 1
       :group-users-only (not (group-users-filter-value
                                     (:query-params @routing/state*)))})))

(defn group-users-filter []
  [:div.form-group.ml-2.mr-2.mt-2
   [:label
    [:span.pr-1 "Group users only"]
    [:input
     {:type :checkbox
      :on-change group-users-filter-on-change
      :checked (group-users-filter-value (:query-params @routing/state*))
      }]]])

(defn filter-component []
  [:div.card.bg-light
   [:div.card-body
   [:div.form-inline
    [group-users-filter]
    [users/form-term-filter]
    [users/form-admins-filter]
    [users/form-role-filter]
    [users/form-type-filter]
    [users/form-per-page]
    [users/form-reset]]]])


;### main #####################################################################

(defn debug-component []
  (when (:debug @state/global-state*)
    [:div
     [:div "@group-users-count*"
      [:pre (with-out-str (pprint @group-users-count*))]]]))

(defn main-page-component []
  [:div
   [routing/hidden-state-component
    {:will-mount users/escalate-query-paramas-update
     :did-update users/escalate-query-paramas-update}]
   [filter-component]
   [:p "To add users disable the \"Group users only\" filter."]
   [users/pagination-component]
   [users/users-table-component colconfig]
   [users/pagination-component]
   [debug-component]
   [users/debug-component]])

(defn index-page []
  [:div.group-users
   [routing/hidden-state-component
    {:will-mount (fn [_] (group/clean-and-fetch))}]
   (breadcrumbs/nav-component
     [(breadcrumbs/leihs-li)
      (breadcrumbs/admin-li)
      (breadcrumbs/groups-li)
      (breadcrumbs/group-li @group/group-id*)
      (breadcrumbs/group-users-li @group/group-id*)]
     [])
   [:div
    [:h1
     (let [c (or @group-users-count* 0)]
       [:span c " " (pluralize-noun c "User")
        [:span " in Group "]
        [group/group-name-component]])]
    [main-page-component]
    ]])
