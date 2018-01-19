(ns leihs.admin.resources.user.front
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
    [cljsjs.jimp]
    [cljsjs.moment]
    [clojure.contrib.inflect :refer [pluralize-noun]]
    [reagent.core :as reagent]
    ))

(declare fetcher-user)
(defonce user-id* (reaction (-> @state/routing-state* :route-params :user-id)))
(defonce user-data* (reagent/atom nil))


(defonce edit-mode?*
  (reaction
    (and (map? @user-data*)
         (boolean ((set '(:user-edit :user-new))
                   (:handler-key @state/routing-state*))))))

(def fetch-user-id* (reagent/atom nil))
(defn fetch-user []
  (let [resp-chan (async/chan)
        id (requests/send-off {:url (path :user (-> @state/routing-state* :route-params))
                               :method :get
                               :query-params {}}
                              {:modal false
                               :title "Fetch User"
                               :handler-key :user
                               :retry-fn #'fetch-user}
                              :chan resp-chan)]
    (reset! fetch-user-id* id)
    (go (let [resp (<! resp-chan)]
          (when (and (= (:status resp) 200)
                     (= id @fetch-user-id*))
            (reset! user-data* (:body resp)))))))


;;; reload logic ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn clean-and-fetch [_]
  (reset! user-data* nil)
  (fetch-user))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn field-component
  ([kw]
   (field-component kw {}))
  ([kw opts]
   (let [opts (merge {:type :text} opts)]
     [:div.form-group.row
      [:label.col.col-form-label.col-sm-2 {:for kw} kw]
      [:div.col
       [:input.form-control
        {:id kw
         :type (:type opts)
         :value (or (kw @user-data*) "")
         :on-change #(swap! user-data* assoc kw (-> % .-target .-value presence))
         :disabled (not @edit-mode?*)}]]])))

(defn checkbox-component [kw]
  [:div.form-check.form-check-inline
   [:label {:for kw}
    [:input
     {:id kw
      :type :checkbox
      :checked (kw @user-data*)
      :on-change #(swap! user-data* assoc kw (-> @user-data* kw boolean not))
      :disabled (not @edit-mode?*)}]
    [:span.ml-2 kw]]])

(defn debug-component []
  (when (:debug @state/global-state*)
    [:div.user-debug
     [:hr]
     [:div.edit-mode?*
      [:h3 "@edit-mode?*"]
      [:pre (with-out-str (pprint @edit-mode?*))]]
     [:div.user-data
      [:h3 "@user-data*"]
      [:pre (with-out-str (pprint @user-data*))]]]))


;;; image ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def img-processing* (reagent/atom {}))

(defn allow-drop [e] (.preventDefault e))

(defn get-img-data [dataTransfer array-buffer-handler]
  (js/console.log (clj->js ["TODO" 'get-img-data dataTransfer]))
  (let [url (.getData dataTransfer "text/plain")]
    (js/console.log (clj->js ["URL" url]))
    (js/console.log (clj->js ["DATA" (.getData dataTransfer "text/uri-list")]))
    (js/console.log (clj->js ["DATA" (.getData dataTransfer "text/html")]))
    (js/console.log (clj->js ["ITEMS" (.-items dataTransfer)]))
    (js/console.log (clj->js ["TYPES" (.-types dataTransfer)]))
    ))

(defn get-file-data [dataTransfer array-buffer-handler]
  (let [f (aget (.-files dataTransfer) 0)
        fname (.-name f)
        reader (js/FileReader.)]
    (set! (.-onload reader)
          (fn [e]
            (let [data (-> e .-target .-result)]
              (array-buffer-handler data))))
    (.readAsArrayBuffer reader f)))

(defn img-handler [data]
  (js/Jimp.read
    data (fn [err img]
           (when err
             (swap! img-processing* assoc :error err)
             (throw err))
           (doto img
             (.resize 512 512)
             (.quality 100))
           (.getBase64 img "image/jpeg"
                       (fn [err b64]
                         (if err
                           (swap! img-processing* assoc :error err)
                           (swap! user-data* assoc :img256_data_url b64)))))))

(defn handle-img-drop [evt]
  (reset! img-processing* {})
  (do
    (allow-drop evt)
    (.stopPropagation evt)
    (let [data-transfer (.. evt -dataTransfer)]
      (if (< 0 (-> data-transfer .-files .-length))
        (get-file-data data-transfer img-handler)
        (get-img-data data-transfer img-handler)))))


(defn handle-img-chosen [evt]
  (reset! img-processing* {})
  (js/console.log evt)
  (js/console.log (-> evt .-target .-files .-length))
  (get-file-data (-> evt .-target) img-handler))

(defn file-upload []
  [:div.box
   {:style {:position :relative}
    :on-drag-over #(allow-drop %)
    :on-drop #(handle-img-drop %)
    :on-drag-enter #(allow-drop %)}
   [:div.text-center
    {:style
     {:position :relative
      :width "256px"
      :height "256px"}}
    [:div.pt-2
     [:label.btn.btn-sm.btn-dark
      [:i.fas.fa-file-image]
      " Choose file "
      [:input#user-image.sr-only
       {:type :file
        :on-change handle-img-chosen}]]
     [:p "or drop file image here"]]
    [:div.text-center
     {:style {:position :absolute
              :bottom 0
              :width "100%"}}
     [:div
      (when (:img256_data_url @user-data*)
        [:p {:style {:margin-top "1em"}}
         [:button.btn.btn-sm.btn-dark
          {:on-click #(swap! user-data* assoc :img256_data_url nil :img32_data_url nil)}
          [:i.fas.fa-times] " Remove image "]])]]]
   (if-let [img-data (:img256_data_url @user-data*)]
           [:img {:src img-data
                  :style {:position :absolute
                          :left 0
                          :top 0
                          :width "256px"
                          :height "256px"
                          :opacity 0.3
                          :z-index -1}}]
           [:div.bg-light
            {:style {:position :absolute
                     :left 0
                     :top 0
                     :width "256px"
                     :height "256px"
                     :z-index -1 }}])])

(defn image-component []
  (if-not @edit-mode?*
    [:img.bg-light.user-image-256
     {:src (if-let [data (:img256_data_url @user-data*)]
             data
             (gravatar-url (:email @user-data*) 256))
      :width 256
      :height 256}]
    [file-upload]))


;; user components ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn basic-component []
  [:div.form.mt-2
   [checkbox-component :is_admin]
   [checkbox-component :sign_in_enabled]
   [checkbox-component :password_sign_in_enabled]
   [field-component :firstname]
   [field-component :lastname]
   [field-component :phone]
   [field-component :email]])

(defn address-component []
  [:div.form.mt-2
   [field-component :address]
   [field-component :zip]
   [field-component :city]
   [field-component :country]
   ])

(defn rest-component []
  [:div.form.mt-2
   [field-component :login]
   [field-component :org_id]
   [field-component :password {:type :password}]])

(defn additional-properties-component []
  [:div.additional-properties
   [:p [:span "The user has been created " [humanize-datetime-component (:created_at @user-data*)]
        ", and updated "[humanize-datetime-component (:updated_at @user-data*)]
        ". "]
    (let [c (:contracts_count @user-data*)]
      (if (< c 1)
        [:span "The user has no contracts."]
        [:span "The user has " c " " (pluralize-noun c "contract")  ". "]))]])

(defn user-component []
  [:div.user-component
   (if (nil?  @user-data*)
     [:div.text-center
      [:i.fas.fa-spinner.fa-spin.fa-5x]
      [:span.sr-only "Please wait"]]
     [:div
      [:div.row.mt-4
       [:div.col-lg-4 [image-component]]
       [:div.col-lg-8.mt-2 [basic-component]]]
      [:div.row.mt-4
       [:div.col-md [address-component]]
       [:div.col-md [rest-component]]]
      (when-not @edit-mode?*
        [additional-properties-component])])])

(defn user-name-component []
  (if-not @user-data*
    [:span {:style {:font-family "monospace"}} (short-id @user-id*)]
    [:em (str (:firstname @user-data*) " " (:lastname @user-data*))]))

(defn user-id-component []
  [:p "id: " [:span {:style {:font-family "monospace"}} (:id @user-data*)]])

;;; show ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn show-page []
  [:div.user
   [state/hidden-routing-state-component
    {:will-mount clean-and-fetch
     :did-change clean-and-fetch}]
   (breadcrumbs/nav-component
     [(breadcrumbs/leihs-li)
      (breadcrumbs/admin-li)
      (breadcrumbs/users-li)
      (breadcrumbs/user-li @user-id*)]
     [(breadcrumbs/api-tokens-li @user-id*)
      (breadcrumbs/user-delete-li @user-id*)
      (breadcrumbs/user-edit-li @user-id*)
      (breadcrumbs/email-li (:email @user-data*))])
   [:div.row
    [:div.col-lg
     [:h1
      [:span " User "]
      [user-name-component]]
     [user-id-component]]]
   [user-component]
   [debug-component]])


;;; edit ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn patch [_]
  (let [resp-chan (async/chan)
        id (requests/send-off {:url (path :user {:user-id @user-id*})
                               :method :patch
                               :json-params  @user-data*}
                              {:modal true
                               :title "Update User"
                               :handler-key :user-edit
                               :retry-fn #'patch}
                              :chan resp-chan)]
    (go (let [resp (<! resp-chan)]
          (when (= (:status resp) 204)
            (accountant/navigate!
              (path :user {:user-id @user-id*})))))))

(defn patch-submit-component []
  (if @edit-mode?*
    [:div
     [:div.float-right
      [:button.btn.btn-warning
       {:on-click patch}
       [:i.fas.fa-save]
       " Save "]]
     [:div.clearfix]]))

(defn edit-page []
  [:div.edit-user
   [state/hidden-routing-state-component
    {:will-mount clean-and-fetch
     :did-change clean-and-fetch}]
   (breadcrumbs/nav-component
     [(breadcrumbs/leihs-li)
      (breadcrumbs/admin-li)
      (breadcrumbs/users-li)
      (breadcrumbs/user-li @user-id*)
      (breadcrumbs/user-edit-li @user-id*)][])
   [:div.row
    [:div.col-lg
     [:h1
      [:span " Edit User "]
      [user-name-component]]
     [user-id-component]]]
   [user-component]
   [patch-submit-component]
   [debug-component]])


;;; new  ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn create [_]
  (let [resp-chan (async/chan)
        id (requests/send-off {:url (path :users)
                               :method :post
                               :json-params  @user-data*}
                              {:modal true
                               :title "Create User"
                               :handler-key :user-new
                               :retry-fn #'create}
                              :chan resp-chan)]
    (go (let [resp (<! resp-chan)]
          (when (= (:status resp) 200)
            (accountant/navigate!
              (path :user {:user-id (-> resp :body :id)})))))))

(defn create-submit-component []
  (if @edit-mode?*
    [:div
     [:div.float-right
      [:button.btn.btn-primary
       {:on-click create}
       " Create "]]
     [:div.clearfix]]))

(defn new-page []
  [:div.new-user
   [state/hidden-routing-state-component
    {:will-mount #(reset! user-data* {:sign_in_enabled true
                                      :password_sign_in_enabled true})}]
   (breadcrumbs/nav-component
     [(breadcrumbs/leihs-li)
      (breadcrumbs/admin-li)
      (breadcrumbs/users-li)
      (breadcrumbs/user-new-li)
      ][])
   [:div.row
    [:div.col-lg
     [:h1
      [:span " New User "]]]]
   [user-component]
   [create-submit-component]
   [debug-component]])


;;; delete ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def transfer-data* (reagent/atom {}))

(defn delete-user [_]
  (let [resp-chan (async/chan)
        id (requests/send-off {:url (path :user (-> @state/routing-state* :route-params))
                               :method :delete
                               :query-params {}}
                              {:title "Delete User"
                               :handler-key :user-delete
                               :retry-fn #'delete-user}
                              :chan resp-chan)]
    (go (let [resp (<! resp-chan)]
          (when (= (:status resp) 204)
            (accountant/navigate!
              (path :users {}
                    (-> @state/global-state* :users-query-params))))))))

(defn delete-without-reasignment-component []
  [:div.card.m-3
   [:div.card-header.bg-warning
    [:h2 "Delete User"]]
   [:div.card-body
    [:p.text-warning
     "This variant is relatively save. "
     "If the to be deleted user is associated to contracts or other entities "
     "the delete operation will just fail without destroying data. "]
    [:div.float-right
     [:button.btn.btn-warning.btn-lg
      {:on-click delete-user}
      [:i.fas.fa-times] " Delete"]]]])

(defn transfer-data-and-delete-user [_]
  (let [resp-chan (async/chan)
        url (path :user-transfer-data
                  {:user-id @user-id*
                   :target-user-id (:target-user-id @transfer-data*)})
        id (requests/send-off
             {:url url
              :method :delete
              :query-params {}}
             {:title "Transfer Data and Delete User"
              :handler-key :user-delete
              :retry-fn #'transfer-data-and-delete-user}
             :chan resp-chan)]
    (go (let [resp (<! resp-chan)]
          (when (= (:status resp) 204)
            (accountant/navigate!
              (path :users {}
                    (-> @state/global-state* :users-query-params))))))))

(defn delete-with-transfer-component []
  [:div.card.m-3
   [:div.card-header.bg-danger
    [:h2 "Transfer Data and Delete User"]]
   [:div.card-body
    [:p.text-warning
     "Related data of the to be deleted user will "
     "transfered, respectively re-associated with the specified user. "
     "Leihs itself will be consistent after this operation. "]
    [:p.text-danger
     "Audits will still contain references to the removed user! "]
    [:p.text-danger
     "External data, such as open contracts printed on paper for example, "
     "will become inconsistent with the data in leihs!"]
    [:div.form
     [:div.form-group
      [:label {:for :user-transfer-id} "Id of user to transfer data to:" ]
      [:input#user-transfer-id.form-control
       {:type :text
        :value (or (-> @transfer-data* :target-user-id) "")
        :on-change #(swap! transfer-data* assoc
                           :target-user-id (-> % .-target .-value presence))}]]]
    [:div.float-right
     [:button.btn.btn-danger
      {:on-click transfer-data-and-delete-user}
      [:i.fas.fa-times] " Transfer and delete"]]]])

(defn delete-page []
  [:div.user-delete
   [state/hidden-routing-state-component
    {:will-mount clean-and-fetch
     :did-change clean-and-fetch}]
   [:div.row
    [:nav.col-lg {:aria-label :breadcrumb :role :navigation}
     [:ol.breadcrumb
      [breadcrumbs/leihs-li]
      [breadcrumbs/admin-li]
      [breadcrumbs/users-li]
      [breadcrumbs/user-li @user-id*]
      [breadcrumbs/user-delete-li @user-id*]]]
    [:nav.col-lg {:role :navigation}]]
   [:h1 "Delete User "
    [user-name-component]]
   [user-id-component]
   [:p.text-danger
    "Users should never be deleted! "
    "Instead it is recommended to " [:b " disable sign-in"]
    " via editing the user. "]
   [delete-without-reasignment-component]
   [delete-with-transfer-component]])
