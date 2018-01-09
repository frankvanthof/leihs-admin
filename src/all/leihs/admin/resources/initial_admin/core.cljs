(ns leihs.admin.resources.initial-admin.core
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go]])
  (:require
    [leihs.admin.front.breadcrumbs :as breadcrumbs]
    [leihs.admin.front.components :as components]
    [leihs.admin.front.requests.core :as requests]
    [leihs.admin.front.shared :refer [humanize-datetime-component short-id gravatar-url]]
    [leihs.admin.front.state :as state]
    [leihs.admin.paths :as paths :refer [path]]
    [leihs.admin.utils.core :refer [keyword str presence]]

    [accountant.core :as accountant]
    [cljs.core.async :as async]
    [cljs.core.async :refer [timeout]]
    [cljs.pprint :refer [pprint]]
    [reagent.core :as reagent]
    ))


(defonce form-data* (reagent/atom {}))
(def firstname-valid?* (reaction (-> @form-data* :firstname presence boolean not)))

(defn post-create-admin []
  (let [resp-chan (async/chan)
        id (requests/send-off {:url (path :initial-admin)
                               :method :post
                               :json-params  @form-data*}
                              {:modal true
                               :title "Create Initial Admin"
                               :handler-key :initial-admin
                               :retry-fn #'post-create-admin}
                              :chan resp-chan)]
    (go (let [resp (<! resp-chan)]))))


(defn text-input-component
  ([kw]
   (text-input-component kw {}))
  ([kw opts]
   (let [opts (merge {:type :text
                      :valid* (reaction (-> @form-data* kw presence))}
                     opts)]
     [:div.form-group
      [:label {:for :firstname} kw]
      [:input.form-control
       {:type (:type opts)
        :class (if @(:valid* opts) "" "is-invalid")
        :value (or (-> @form-data* kw) "")
        :on-change #(swap! form-data* assoc kw (-> % .-target .-value presence))
        }]])))

(defn debug-component []
  (when (:debug @state/global-state*)
    [:div.debug
     [:hr]
     [:h3 "@form-data*"]
     [:pre (with-out-str (pprint @form-data*))]]))

(defn page []
  [:div.initial-admin
   [:div.row
    [:nav.col-lg {:aria-label :breadcrumb :role :navigation}
     [:ol.breadcrumb
      [breadcrumbs/leihs-li]
      [breadcrumbs/admin-li]
      [breadcrumbs/initial-admin-li]]]
    [:nav.col-lg {:role :navigation}]]
   [:div
    [:h1 "Initial Admin"]
    [:p "An initial administrator account is required to sign in and further configure this instance of leihs."]
    [:div.form
     [text-input-component :firstname]
     [text-input-component :lastname]
     [text-input-component :email]
     [text-input-component :password {:type :password}]
     [:div.float-right
      [:button.btn.btn-primary
       {:on-click post-create-admin }
       "Submit"]]
     [:div.clearfix]
     [debug-component]]]])
