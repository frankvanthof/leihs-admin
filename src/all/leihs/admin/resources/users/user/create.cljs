(ns leihs.admin.resources.users.user.create
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
    [leihs.admin.resources.users.breadcrumbs :as breadcrumbs]
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

(defn post [& args]
  (go (when-let
        [data (some->
                {:chan (async/chan)
                 :url (path :users)
                 :method :post
                 :json-params  (-> @data*
                                   (update-in [:extended_info]
                                              (fn [s] (.parse js/JSON s))))}
                http-client/request :chan <!
                http-client/filter-success! :body)]
        (reset! data* nil)
        (accountant/navigate!
          (path :user {:user-id (:id data)})))))

(defn clean [& _]
  (reset! data* {}))

(defn submit-component []
  [:div
   [:div.float-right
    [:button.btn.btn-primary
     icons/add
     " Create "]]
   [:div.clearfix]])

(defn edit-form-component
  ([]
   (edit-form-component (fn [e]
                          (.preventDefault e)
                          (post))))
  ([on-submit]
   [:form.form
    {:auto-complete :off
     :on-submit on-submit}
    [edit-main/inner-form-component]
    [submit-component]]))

(defn page []
  [:div.user-create
   [routing/hidden-state-component
    {:did-mount clean}]
   [breadcrumbs/nav-component
    (conj @breadcrumbs/left* [breadcrumbs/user-create-li])[]]
   [:h1 "Create User " ]
   [edit-form-component]
   [edit-core/debug-component]])
