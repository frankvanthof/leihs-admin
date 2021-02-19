(ns leihs.admin.resources.users.user.edit-main
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
    [leihs.admin.resources.users.user.breadcrumbs :as breadcrumbs]
    [leihs.admin.resources.users.user.core :as core :refer [user-id*]]
    [leihs.admin.resources.users.user.edit-core :as edit-core :refer [data*]]
    [leihs.admin.resources.users.user.edit-image :as edit-image]
    [leihs.admin.state :as state]
    [leihs.admin.utils.misc :as front-shared :refer [wait-component]]

    [accountant.core :as accountant]
    [cljs.core.async :as async :refer [timeout]]
    [cljs.pprint :refer [pprint]]
    [reagent.core :as reagent]
    [taoensso.timbre :as logging]
    ))


(defn patch [& args]
  (let [route (path :user {:user-id @user-id*})]
    (go (when (some->
                {:chan (async/chan)
                 :url route
                 :method :patch
                 :json-params  (-> @data*
                                   (update-in [:extended_info]
                                              (fn [s] (.parse js/JSON s))))}
                http-client/request :chan <!
                http-client/filter-success!)
          (accountant/navigate! route)))))

(defn patch-submit-component []
  [:div
   [:div.float-right
    [:button.btn.btn-warning
     icons/save
     " Save "]]
   [:div.clearfix]])


(defn inner-form-component []
  [:div
   [edit-core/essentials-form-component]
   [:div.image
    [:h3 "Image / Avatar"]
    [edit-image/image-component]]
   [edit-core/personal-and-contact-form-component]
   [edit-core/account-settings-form-component]])


(defn edit-form-component
  [& {:keys [patch]
      :or {patch patch}}]
  [:form.form
   {:auto-complete :off
    :on-submit (fn [e]
                 (.preventDefault e)
                 (patch))}
   [inner-form-component]
   [patch-submit-component]])

(defn page []
  [:div.user-edit
   [routing/hidden-state-component
    {:did-mount core/clean-and-fetch}]
   [breadcrumbs/nav-component
    (conj @breadcrumbs/left* [breadcrumbs/edit-li]) []]
   [:h1 "Edit User " (when @data* [core/name-component @data*])]
   (if (not @data*)
     [wait-component]
     [edit-form-component])
   [edit-core/debug-component]])
