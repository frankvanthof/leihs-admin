(ns leihs.admin.resources.users.user.edit-main
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go]])
  (:require
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.requests.core :as requests]
    [leihs.core.routing.front :as routing]
    [leihs.core.icons :as icons]

    [leihs.admin.resources.users.user.breadcrumbs :as breadcrumbs]
    [leihs.admin.utils.misc :as front-shared :refer [wait-component]]
    [leihs.admin.state :as state]
    [leihs.admin.paths :as paths :refer [path]]
    [leihs.admin.resources.users.user.core :as core :refer [user-id*]]
    [leihs.admin.resources.users.user.edit-core :as edit-core :refer [data*]]
    [leihs.admin.resources.users.user.edit-image :as edit-image]

    [accountant.core :as accountant]
    [cljs.core.async :as async :refer [timeout]]
    [cljs.pprint :refer [pprint]]
    [reagent.core :as reagent]
    [taoensso.timbre :as logging]
    ))

(defn fetch []
  (def fetch-id* (reagent/atom nil))
  (let [resp-chan (async/chan)
        id (requests/send-off {:url (path :user {:user-id @user-id*})
                               :method :get
                               :query-params {}}
                              {:modal false
                               :title "Fetch User"}
                              :chan resp-chan)]
    (reset! fetch-id* id)
    (go (let [resp (<! resp-chan)]
          (when (and (= (:status resp) 200)
                     (= id @fetch-id*))
            (reset! data*
                    (-> resp :body (update-in
                                     [:extended_info]
                                     (fn [json] (.stringify js/JSON (clj->js json)))))))))))

(defn patch
  ([] (patch (path :user {:user-id @user-id*})))
  ([redirect-to]
   (let [resp-chan (async/chan)
         id (requests/send-off {:url (path :user {:user-id @user-id*})
                                :method :patch
                                :json-params  (-> @data*
                                                  (update-in [:extended_info]
                                                             (fn [s] (.parse js/JSON s))))}
                               {:modal true
                                :title "Update User"
                                :handler-key :user-edit
                                :retry-fn #'patch}
                               :chan resp-chan)]
     (go (let [resp (<! resp-chan)]
           (when (= (:status resp) 200)
             (accountant/navigate! redirect-to)))))))

(defn clean-and-fetch [& _]
  (reset! data* nil)
  (fetch))

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
   [edit-core/account-settings-form-component]
   ])


(defn edit-form-component
  ([]
   (edit-form-component (fn [e]
                          (.preventDefault e)
                          (patch))))
  ([on-submit]
   [:form.form
    {:auto-complete :off
     :on-submit on-submit}
    [inner-form-component]
    [patch-submit-component]]))

(defn page []
  [:div.user-edit
   [routing/hidden-state-component
    {:did-mount clean-and-fetch}]
   [breadcrumbs/nav-component
    (conj @breadcrumbs/left* [breadcrumbs/edit-li]) []]
   [:h1 "Edit User " (when @data* [core/name-component @data*])]
   (if (not @data*)
     [wait-component]
     [edit-form-component])
   [edit-core/debug-component]])
