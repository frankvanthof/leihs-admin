(ns leihs.admin.resources.user.front.shared
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go]])
  (:require
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.user.shared :refer [user-name-html]]
    [leihs.core.requests.core :as requests]
    [leihs.core.routing.front :as routing]

    [leihs.admin.front.breadcrumbs :as breadcrumbs]
    [leihs.admin.front.components :as components]
    [leihs.admin.front.shared :refer [humanize-datetime-component gravatar-url]]
    [leihs.admin.front.state :as state]
    [leihs.admin.paths :as paths :refer [path]]

    [accountant.core :as accountant]
    [cljs.core.async :as async]
    [cljs.core.async :refer [timeout]]
    [cljs.pprint :refer [pprint]]
    [cljsjs.jimp]
    [cljsjs.moment]
    [clojure.contrib.inflect :refer [pluralize-noun]]
    [clojure.string]
    [reagent.core :as reagent]
    ))


(defonce user-id* (reaction (-> @routing/state* :route-params :user-id)))
(defonce user-data* (reagent/atom nil))

(defonce edit-mode?*
  (reaction
    (and (map? @user-data*)
         (boolean ((set '(:user-edit :user-new :user-password))
                   (:handler-key @routing/state*))))))


(def fetch-user-id* (reagent/atom nil))
(defn fetch-user []
  (let [resp-chan (async/chan)
        id (requests/send-off {:url (path :user (-> @routing/state* :route-params))
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
            (reset! user-data*
                    (-> resp :body (update-in
                                     [:extended_info]
                                     (fn [json] (.stringify js/JSON (clj->js json)))))))))))

(defn clean-and-fetch [& args]
  (reset! user-data* nil)
  (fetch-user))


(defn field-component
  ([kw]
   (field-component kw {}))
  ([kw opts]
   (let [opts (merge {:type :text} opts)]
     (when (or @edit-mode?* (not= kw :password))
       [:div.form-group.row
        [:label.col.col-form-label.col-sm-2 {:for kw} kw]
        [:div.col.col-sm-10
         [:div.input-group
          (if @edit-mode?*
            [:input.form-control
             {:id kw
              :autoComplete (or (:autoComplete opts) :off)
              :type (:type opts)
              :value (or (kw @user-data*) "")
              :on-change (fn [e]
                           (swap! user-data* assoc kw (-> e .-target .-value presence))
                           (when-let [hook (:on-change opts)]
                             (apply hook [e])))
              :disabled (not @edit-mode?*)}]
            [:div
             (if-let [value (-> @user-data* kw presence)]
               [:span.form-control-plaintext.text-truncate
                {:style
                 {:max-width "20em"}}
                (case (:type opts)
                  :email [:a {:href (str "mailto:" value)}
                          [:i.fas.fa-envelope] " " value]
                  :url [:a {:href value} value]
                  value)])])]
         [:small (:remark opts)]]]))))

(defn json-component
  [kw opts]
  (let [expanded* (reagent/atom false)
        is-valid* (reaction
                    (try
                      (.parse js/JSON (get @user-data* kw))
                      true
                      (catch :default _ false)))
        opts (merge {} opts)]
    (fn []
      (when (or @edit-mode?* (not= kw :password))
        [:div.form-group.row
         [:label.col.col-form-label.col-sm-2 {:for kw} kw]
         [:div.col.col-sm-10
          [:div.input-group
           (if @edit-mode?*
             [:textarea.form-control
              {:id kw
               :class (clojure.string/join
                        " " [(if @is-valid* "is-valid" "is-invalid")])
               :autoComplete (or (:autoComplete opts) :off)
               :value (or (kw @user-data*) "")
               :on-change (fn [e]
                            (swap! user-data* assoc kw (-> e .-target .-value presence))
                            (when-let [hook (:on-change opts)]
                              (apply hook [e])))
               :disabled (not @edit-mode?*)}]
             [:div
              (if-let [value (-> @user-data* kw presence)]
                (if-not @expanded*
                  [:span [:span.form-control-plaintext.text-truncate
                          {:style {:max-width "20em"}} value]
                   [:button.btn-link
                    {:on-click #(reset! expanded* true)}
                    [:i.fa.fa-expand-arrows-alt]]]
                  [:pre.form-control-plaintext
                   (.stringify js/JSON
                               (.parse js/JSON value) nil 2)]))])]
          [:small (:remark opts)]]]))))


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

(defn update-img-digest [& args]
  "sets img_digest to the md5 hex of the concatenated img256_url and img32_url
  or to nil if both are empty; call this if either the fields :img256_url or :img32_url
  have been updated via the form; or via image drop or delete"
  (swap! user-data*
         (fn [user-data]
           (assoc user-data
                  :img_digest (some-> (str (:img256_url user-data)
                                           " "
                                           (:img32_url user-data))
                                      presence
                                      leihs.core.digest/md5-hex)))))

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
  (doseq [res [256 32]]
    (js/Jimp.read
      data (fn [err img]
             (when err
               (swap! img-processing* assoc :error err)
               (throw err))
             (doto img
               (.resize res res)
               (.quality 80))
             (.getBase64 img "image/jpeg"
                         (fn [err b64]
                           (if err
                             (swap! img-processing* assoc :error err)
                             (swap! user-data* assoc (keyword (str "img" res "_url")) b64)))))))
  (update-img-digest))

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
  [:div.box.mb-2
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
      (when (:img256_url @user-data*)
        [:p {:style {:margin-top "1em"}}
         [:a.btn.btn-sm.btn-dark
          {:href "#"
           :on-click #(swap! user-data* assoc :img256_url nil :img32_url nil :img_digest nil)}
          [:i.fas.fa-times] " Remove image "]])]]]
   [:div
    (if-let [img-data (:img256_url @user-data*)]
      [:img {:src img-data
             :style {
                     :position :absolute
                     :left 0
                     :top 0
                     :max-width "256px"
                     :max-height "256px"
                     :opacity 0.3
                     :z-index -1}}]
      [:div.bg-light
       {:style {:position :absolute
                :left 0
                :top 0
                :width "256px"
                :height "256px"
                :z-index -1 }}])]])

(defn image-component []
  [:div.clearfix
   [:h3 "User-Image"]
   (if-not @edit-mode?*
     [:div
      {:style {:width 256 :height 256}}
      [:img.bg-light.user-image-256.mb-2
       {:src (if-let [data (:img256_url @user-data*)]
               data
               (gravatar-url (:email @user-data*) 256))
        :style {:max-width 256
                :max-height 256}}]]
     [file-upload])
   ; custom handler here?
   [field-component :img256_url {:type :url :on-change update-img-digest}]
   [field-component :img32_url {:type :url :on-change update-img-digest}]
   [field-component :img_digest
    {:type :text
     :remark [:strong
              "Proceed judiciously when overriding this field manually!"
              " Set this value explicitly when using the API! "]}]])


;; user components ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn basic-component []
  [:div.form.mt-2
   [:h3 "Basic properties"]
   [checkbox-component :is_admin]
   [checkbox-component :account_enabled]
   [checkbox-component :password_sign_in_enabled]
   [field-component :firstname]
   [field-component :lastname]
   [field-component :phone]
   [field-component :email {:type :email}]])

(defn address-component []
  [:div.form.mt-2
   [:h3 "Address"]
   [field-component :address]
   [field-component :zip]
   [field-component :city]
   [field-component :country]
   ])

(defn rest-component []
  [:div.form.mt-2
   [:h3 "Other"]
   [field-component :secondary_email {:type :email}]
   [field-component :url {:type :url}]
   [field-component :org_id]
   [field-component :login]
   [field-component :badge_id]
   [json-component :extended_info {:type :json}]
   ])

(defn additional-properties-component []
  [:div.additional-properties
   [:p [:span "The user has been created " [humanize-datetime-component (:created_at @user-data*)]
        ", and updated "[humanize-datetime-component (:updated_at @user-data*)]
        ". "]
    (let [c (:contracts_count @user-data*)]
      (if (< c 1)
        [:span "The user has no contracts."]
        [:span "The user has " c " " (pluralize-noun c "contract")  ". "]))
    (let [c (:inventory_pool_roles_count @user-data*)]
      (if (< c 1)
        [:span "The user has no inventory pool roles."]
        [:span "The user has "
         [:a {:href (path :user-inventory-pools-roles {:user-id @user-id*})}
          c " inventory pool " (pluralize-noun c "role")]  ". "]))]])

(defn user-component [submit-component submit-handler]
  [:div.user-component
   (if (nil?  @user-data*)
     [:div.text-center
      [:i.fas.fa-spinner.fa-spin.fa-5x]
      [:span.sr-only "Please wait"]]
     [(if @edit-mode?* :form :div)
      {:on-submit (fn [e]
                    (.preventDefault e)
                    (submit-handler))}
      [basic-component]
      [image-component]
      [address-component]
      [rest-component]
      (if @edit-mode?*
        [submit-component]
        [additional-properties-component])])])

(defn user-name-component []
  (let [user-id @user-id*
        user-data @user-data*]
    (user-name-html user-id user-data)))

(defn user-id-component []
  [:p "id: " [:span {:style {:font-family "monospace"}} (:id @user-data*)]])
