(ns leihs.admin.resources.users.front
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

(def current-query-paramerters* (reaction (-> @state/routing-state* :query-params)))

(def default-query-parameters {:is_admin nil
                               :role "any"
                               :page 1
                               :per-page 12
                               :term ""
                               :type "any" })

(def current-query-paramerters-normalized*
  (reaction (merge default-query-parameters
           @current-query-paramerters*)))

(def fetch-users-id* (reagent/atom nil))
(def users* (reagent/atom {}))
(def page-is-active?* (reaction (= (-> @state/routing-state* :handler-key) :users)))

(defn fetch-users []
  (let [query-paramerters @current-query-paramerters-normalized*]
    (go (<! (timeout 200))
        (when (= query-paramerters @current-query-paramerters-normalized*)
          (let [resp-chan (async/chan)
                id (requests/send-off {:url (path :users) :method :get
                                       :query-params query-paramerters}
                                      {:modal false
                                       :title "Fetch Users"
                                       :handler-key :users
                                       :retry-fn #'fetch-users}
                                      :chan resp-chan)]
            (reset! fetch-users-id* id)
            (go (let [resp (<! resp-chan)]
                  (when (and (= (:status resp) 200) ;success
                             (= id @fetch-users-id*) ;still the most recent request
                             (= query-paramerters @current-query-paramerters-normalized*)) ;query-params have not changed yet
                    ;(reset! effective-query-paramerters* current-query-paramerters)
                    (swap! users* assoc query-paramerters (->> (-> resp :body :users)
                                                               (map-indexed (fn [idx u]
                                                                              (assoc u :key (:id u)
                                                                                     :c idx)))
                                                               ))))))))))

(defn escalate-query-paramas-update [_]
  (fetch-users)
  (swap! state/global-state*
         assoc :users-query-params @current-query-paramerters-normalized*))

(defn current-query-params-component []
  (reagent/create-class
    {:component-did-mount escalate-query-paramas-update
     :component-did-update escalate-query-paramas-update
     :reagent-render
     (fn [_] [:div.current-query-parameters
              {:style {:display (if @state/debug?* :block :none)}}
              [:h3 "@current-query-paramerters-normalized*"]
              [components/pre-component @current-query-paramerters-normalized*]])}))


;;; helpers ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;;; Filter ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn form-term-filter []
  [:div.form-group.ml-2.mr-2.mt-2
   [:label.sr-only {:for :users-fiter-term} "Term"]
   [:input#users-filter-term.form-control.mb-1.mr-sm-1.mb-sm-0
    {:type :text
     :placeholder "Search term ..."
     :value (or (-> @current-query-paramerters-normalized* :term presence) "")
     :on-change (fn [e]
                  (let [val (or (-> e .-target .-value presence) "")]
                    (accountant/navigate! (path :users {}
                                                (merge @current-query-paramerters-normalized*
                                                       {:page 1
                                                        :term val})))))}]])

(defn form-admins-filter []
  [:div.form-group.ml-2.mr-2.mt-2
   [:label
    [:span.pr-1 "Admins only"]
    [:input
     {:type :checkbox
      :on-change #(let [new-state (case (-> @current-query-paramerters-normalized*
                                            :is_admin presence)
                                    ("true" true) nil
                                    true)]
                    (js/console.log (with-out-str (pprint new-state)))
                    (accountant/navigate! (path :users {}
                                                (merge @current-query-paramerters-normalized*
                                                       {:page 1
                                                        :is_admin new-state}))))
      :checked (case (-> @current-query-paramerters-normalized*
                         :is_admin presence)
                 (nil false "false") false
                 ("true" true) true)}]]])

(defn form-type-filter []
  (let [type (or (-> @current-query-paramerters-normalized* :type presence) "any")]
    [:div.form-group.ml-2.mr-2.mt-2
     [:label.mr-1 {:for :users-filter-type} "Type"]
     [:select#users-filter-type.form-control
      {:value type
       :on-change (fn [e]
                    (let [val (or (-> e .-target .-value presence) "")]
                      (accountant/navigate! (path :users {}
                                                  (merge @current-query-paramerters-normalized*
                                                         {:page 1
                                                          :type val})))))}
      (for [t ["any" "org" "manual"]]
        [:option {:key t :value t} t])]]))

(defn form-role-filter []
  (let [role (or (-> @current-query-paramerters-normalized* :role presence) "any")]
    [:div.form-group.ml-2.mr-2.mt-2
     [:label.mr-1 {:for :users-filter-role} "Role"]
     [:select#users-filter-role.form-control
      {:value role
       :on-change (fn [e]
                    (let [val (or (-> e .-target .-value presence) "")]
                      (accountant/navigate! (path :users {}
                                                  (merge @current-query-paramerters-normalized*
                                                         {:page 1
                                                          :role val})))))}
      (for [a [ "any" "customer" "group_manager" "inventory_manager" "lending_manager"]]
        [:option {:key a :value a} a])]]))

(defn form-per-page []
  (let [per-page (or (-> @current-query-paramerters-normalized* :per-page presence) "12")]
    [:div.form-group.ml-2.mr-2.mt-2
     [:label.mr-1 {:for :users-filter-per-page} "Per page"]
     [:select#users-filter-per-page.form-control
      {:value per-page
       :on-change (fn [e]
                    (let [val (or (-> e .-target .-value presence) "12")]
                      (accountant/navigate! (path :users {}
                                                  (merge @current-query-paramerters-normalized*
                                                         {:page 1
                                                          :per-page val})))))}
      (for [p [12 25 50 100 250 500 1000]]
        [:option {:key p :value p} p])]]))

(defn form-reset []
  [:div.form-group.mt-2
   [:label.sr-only {:for :users-filter-reset} "Reset"]
   [:a#users-filter-reset.btn.btn-warning
    {:href (path :users {} default-query-parameters)}
    [:i.fas.fa-times]
    " Reset "]])

(defn filter-form []
  [:div.card.bg-light
   [:div.card-body
   [:div.form-inline
    [form-term-filter]
    [form-admins-filter]
    [form-role-filter]
    [form-type-filter]
    [form-per-page]
    [form-reset]]]])

;;; Table ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn users-thead-component []
  [:thead
   [:tr
    [:th]
    [:th]
    [:th "Id"]
    [:th "Org id"]
    [:th "Firstname"]
    [:th "Lastname"]
    [:th "Email"]]])

(defn user-row-component [user]
  [:tr {:key (:key user)}
   [:td (:count user)]
   [:td [:a {:href (path :user {:user-id (:id user)})}
         [:img {:src (or (:img32_data_url user)
                         (gravatar-url (:email user)))}]]]
   [:td [:a {:href (path :user {:user-id (:id user)})}
         (short-id (:id user))]]
   [:td {:style {:font-family "monospace"}} (:org_id user)]
   [:td (:firstname user)]
   [:td (:lastname user)]
   [:td [:a {:href (str "mailto:" (:email user))}
         [:i.fas.fa-envelope] " " (:email user)]]])

(defn users-table-component []
  (if-not (contains? @users* @current-query-paramerters-normalized*)
    [:div.text-center
     [:i.fas.fa-spinner.fa-spin.fa-5x]]
    (if-let [users (-> @users* (get  @current-query-paramerters-normalized* []) seq)]
      [:table.table.table-striped.table-sm
       [users-thead-component]
       [:tbody
        (let [page (:page @current-query-paramerters-normalized*)
              per-page (:per-page @current-query-paramerters-normalized*)]
          (doall (for [user users]
                   (user-row-component
                     (assoc user :count (+ 1 (:c user) (* per-page (- page 1))))))))]]
      [:div.alert.alert-warning.text-center "No (more) users found."])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn pagination-component []
  [:div.clearfix.mt-2.mb-2
   (let [page (dec (:page @current-query-paramerters-normalized*))]
     [:div.float-left
      [:a.btn.btn-primary.btn-sm
       {:class (when (< page 1) "disabled")
        :href (path :users {} (assoc @current-query-paramerters-normalized*
                                     :page page))}
       [:i.fas.fa-arrow-circle-left] " Previous " ]])
   [:div.float-right
    [:a.btn.btn-primary.btn-sm
     {:href (path :users {} (assoc @current-query-paramerters-normalized*
                                   :page (inc (:page @current-query-paramerters-normalized*))))}
     " Next " [:i.fas.fa-arrow-circle-right]]]])

(defn debug-component []
  (when (:debug @state/global-state*)
    [:section.debug
     [:hr]
     [:h2 "Page Debug"]
     [:div.users
      [:h3 "@users*"]
      [:pre (with-out-str (pprint @users*))]]]))

(defn page []
  [:div.users
   (breadcrumbs/nav-component
     [(breadcrumbs/leihs-li)
      (breadcrumbs/admin-li)
      (breadcrumbs/users-li)]
     [(breadcrumbs/user-new-li)])
   [current-query-params-component]
   [:h1 "Users"]
   [filter-form]
   [pagination-component]
   [users-table-component]
   [pagination-component]
   [debug-component]])
