(ns leihs.admin.resources.users.user.core
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go]])
  (:require
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.routing.front :as routing]
    [leihs.core.icons :as icons]
    [leihs.core.auth.core :as auth]

    [leihs.admin.common.http-client.core :as http-client]
    [leihs.admin.state :as state]
    [leihs.admin.paths :as paths :refer [path]]
    [leihs.admin.common.components :as components]
    [leihs.admin.resources.users.user.shared :as user-shared]

    [cljs.pprint :refer [pprint]]
    [cljs.core.async :as async :refer [timeout]]
    [clojure.string :refer [split trim]]
    [reagent.core :as reagent]
    [taoensso.timbre :as logging]
    ))


(defonce user-id*
  (reaction (or (-> @routing/state* :route-params :user-id)
                "00000000-0000-0000-0000-000000000000")))


;;; data ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defonce user-data* (reagent/atom nil))

(defn stringify-json [json]
  (.stringify js/JSON (clj->js json)))

(defn clean-and-fetch
  [& {:keys [path]
      :or {path (path :user {:user-id @user-id*})}}]
  (reset! user-data* nil)
  (go (reset! user-data*
              (-> {:chan (async/chan)
                   :url path}
                  http-client/request
                  :chan <! http-client/filter-success!  :body
                  (update-in [:extended_info] stringify-json)))))



;;; some display helpers ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn fullname [user]
  (-> (str (:firstname user) " " (:lastname user))
      trim
      presence))

(defn some-uid [user]
  (or (some-> user :email presence)
      (some-> user :login presence)
      (some-> user :org_id presence)
      (some-> user :id)))

(defn some-id-component [user]
  [:span.text-monospace
   (or (some-> user :email presence)
       (some-> user :login presence)
       (some-> user :org_id presence)
       (some-> user :id (split "-") first))])

(defn fullname-or-some-uid [user]
  (or (fullname user) (some-uid user)))

(defn fullname-some-uid-seq [user]
  (->> [(fullname user) (some-uid user)]
       (filter identity)))

(defn name-component [user]
  [:span
   (logging/info 'user user)
   (when-not user
     (logging/error "use name-link-component when you call wo argument and :user-id is in the routes"))
   (let [p (path :user {:user-id (:id user)})
         name-or-id (fullname-or-some-uid user)]
     [components/link [:em name-or-id] p])])

(defn name-link-component []
  [:span
   [routing/hidden-state-component
    {:did-change clean-and-fetch
     :did-mount clean-and-fetch}]
   (let [p (path :user {:user-id @user-id*})
         name-or-id (fullname-or-some-uid @user-data*)]
     [components/link [:em name-or-id] p])])

(defn img-avatar-component [user]
  [:div.mb-2
   [components/img-large-component user]])

(defn user-data-li-dl-component [dt dd]
  ^{:key dt}
  [:li {:key dt}
   [:dl.row.mb-0
    [:dt.col-sm-4 dt]
    [:dd.col-sm-8 dd]]])

(defn account-properties-component [user]
  [:ul.list-unstyled
   [user-data-li-dl-component "Enabled"
    (if (:account_enabled user)
      [:span.text-success "yes"]
      [:span.text-danger "no"])]
   [user-data-li-dl-component "Admin"
    (if (:is_admin user)
      [:span "yes"]
      [:span "no"])]
   [user-data-li-dl-component "System-admin"
    (if (:is_system_admin user)
      [:span "yes"]
      [:span "no"])]
   [user-data-li-dl-component "PW sign-in"
    (if (:password_sign_in_enabled user)
      [:span "yes"]
      [:span "no"])]
   [user-data-li-dl-component "Admin protected"
    (if (:admin_protected user)
      [:span "yes"]
      [:span "no"])]
   [user-data-li-dl-component "System-admin protected"
    (if (:system_admin_protected user)
      [:span "yes"]
      [:span "no"])]
   (when-let [badge-id (:badge_id user)]
     [user-data-li-dl-component "Badge ID" badge-id])
   (when-let [login (:login user)]
     [user-data-li-dl-component "Login" login])
   (when-let [org (:organization user)]
     [user-data-li-dl-component "Organization" org])
   (when-let [org-id (:org_id user)]
     [user-data-li-dl-component "Org ID" org-id])
   [user-data-li-dl-component "ID"
    [components/truncated-id-component (:id user)]]])

(defn email-component [email-addr]
  [:a {:href (str "mailto:" email-addr)}
   icons/email "\u00A0" [:span  email-addr ]])

(defn personal-properties-component [user]
  [:ul.list-unstyled
   [user-data-li-dl-component
    "Name" (fullname user)]
   [user-data-li-dl-component "Email"
    [:ul.list-unstyled
     (for [[idx email] (->> [(:email user)
                             (:secondary_email user)]
                            (map presence)
                            (filter identity)
                            (map-indexed vector))]
       [:li {:key idx} (email-component email)])]]
   (when-let [p (:phone user)]
     [user-data-li-dl-component "Phone" p])
   (let [addr (-> user :address presence)
         zip (-> user :zip presence)
         city (-> user :city presence)
         country (-> user :country presence)]
     (when (or addr zip city country)
       [user-data-li-dl-component "Address"
        [:ul.list-unstyled
         (for [[idx itm]  (some->> [addr (str zip  " "  city) country]
                                   (map clojure.string/trim)
                                   (map presence)
                                   (map identity)
                                   (map-indexed vector))]
           ^{:key idx} [:li itm])]]))
   (when-let [url (:url user)]
     [user-data-li-dl-component "URL"
       [:a {:href url}
        [:p
         {:style
          {:white-space :nowrap
           :overflow :hidden
           :text-overflow :ellipsis
           :max-width :15em}}
         url ]]])])


(defn debug-component []
  (when (:debug @state/global-state*)
    [:div.user-debug
     [:hr]
     [:div.user-data
      [:h3 "@user-data*"]
      [:pre (with-out-str (pprint @user-data*))]]]))




