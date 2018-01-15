(ns leihs.admin.resources.auth.core
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go]])
  (:require
    [leihs.admin.anti-csrf.core :as anti-csrf]
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
    [cljsjs.jimp]
    [clojure.contrib.inflect :refer [pluralize-noun]]
    [reagent.core :as reagent]
    ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def password-sign-in* (reagent/atom {}))

(defn sign-in-form-component []
  [:div
   [:h2 "Sign in with password"]
   (when (-> @state/routing-state* :query-params :sign-in-warning)
     [:div.alert.alert-warning
      [:h3 "Attention"]
      [:p "Make sure that you use the "
       [:b "correct email-address "]
       "and "
       [:b "the correct password. "]]
      [:p "Reset your password or contact your leihs administrator if "
       "sign-in fails persistently." ]])
   [:form.form
    {:method :post
     :action (path :auth-password-sign-in)}
    [anti-csrf/hidden-form-group-token-component]
    [:div.form-group
     [:input
      {:name :url
       :type :hidden
       :value (-> @state/routing-state* :url)}]]
    [:div.form-group
     [:label {:for :email} "Email: "]
     [:div
      [:input.form-control
       {:id :email
        :name :email
        :type :email
        :value (:email @password-sign-in*)
        :on-change #(swap! password-sign-in* assoc :email (-> % .-target .-value presence))}]]]
    [:div.form-group
     [:label {:for :password} "Password: "]
     [:div
      [:input.form-control
       {:id :password
        :name :password
        :type :password
        :value (:password @password-sign-in*)
        :on-change #(swap! password-sign-in* assoc :password (-> % .-target .-value presence))}]]]
    [:div.form-group.float-right
     [:button.btn.btn-primary
      {:type :submit}
      [:i.fas.fa-sign-in-alt] " Sign in"]]]
   [:div.clearfix]])

(defn password-sign-in-page []
  (reagent/create-class
    {:component-did-mount #(reset! password-sign-in* {})
     :reagent-render
     (fn [_]
       [:div.password-sign-in
        (breadcrumbs/nav-component
          [(breadcrumbs/leihs-li)
           (breadcrumbs/admin-li)
           (breadcrumbs/auth-li)
           (breadcrumbs/auth-password-sign-in-li)
           ][])
        [sign-in-form-component]
        ])}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defonce auth-data* (reagent/atom nil))

(def fetch-auth-id* (reagent/atom nil))

(defn fetch-auth []
  (reset! auth-data* nil)
  (let [resp-chan (async/chan)
        id (requests/send-off {:url (path :auth)
                               :method :get
                               :query-params {}}
                              {:modal false
                               :title "Fetch Authentication"
                               :handler-key :auth
                               :retry-fn #'fetch-auth}
                              :chan resp-chan)]
    (reset! fetch-auth-id* id)
    (go (let [resp (<! resp-chan)]
          (when (and (= (:status resp) 200)
                     (= id @fetch-auth-id*))
            (reset! auth-data* (:body resp)))))))


(defn auth-page []
  (reagent/create-class
    {:component-did-mount #(fetch-auth)
     :reagent-render
     (fn [_]
       [:div.session
        (breadcrumbs/nav-component
          [(breadcrumbs/leihs-li)
           (breadcrumbs/admin-li)
           (breadcrumbs/auth-li)]
          [(breadcrumbs/auth-password-sign-in-li)])
        [:h1 "Authentication"]
        [:p "The data shown below is mostly of interest for exploring the API or for debugging."]
        (when-let [auth-data @auth-data*]
          [:pre.bg-light
           (with-out-str (pprint auth-data))])])}))


