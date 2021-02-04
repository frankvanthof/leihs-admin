(ns leihs.admin.html
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    )
  (:require
    [leihs.core.auth.core :as auth]
    [leihs.admin.resources.breadcrumbs :as breadcrumbs]
    [leihs.admin.resources.inventory-pools.breadcrumbs :as breadcrumbs-inventory-pools] 
    [leihs.core.anti-csrf.front :as anti-csrf]
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.requests.core :as requests]
    [leihs.core.requests.modal]
    [leihs.core.routing.front :as routing]
    [leihs.core.user.front :as core-user]
    [leihs.core.user.shared :refer [short-id]]
    [leihs.core.env :refer [use-global-navbar?]]

    [leihs.admin.common.http-client.modals]
    [leihs.admin.state :as state]
    [leihs.admin.paths :refer [path]]

    [clojure.pprint :refer [pprint]]
    [accountant.core :as accountant]
    [reagent.dom :as rdom]
    ;; ["/leihs-ui-client-side-external-react.js" :as UI]
    ;; ["@leihs/ui" :as UI]
    ["@leihs/ui/dist/components-external-react" :as UI]
    ))

#_(defn li-navitem [handler-key display-string]
  (let [active? (= (-> @routing/state* :handler-key) handler-key)]
    [:li.nav-item
     {:class (if active? "active" "")}
     [:a.nav-link {:href (path handler-key)} display-string]]))

#_(defn li-admin-navitem []
  (let [active? (boolean
                  (when-let [current-path (-> @routing/state* :path)]
                    (re-matches #"^/admin.*$" current-path)))]
    [:li.nav-item
     {:class (if active? "active" "")}
     [:a.nav-link {:href (path :admin)} "Admin"]]))

(defn sign-out-nav-component []
  [:form.form-inline.ml-2
   {:action (path :auth-sign-out {} {:target (-> @routing/state* :url)})
    :method :post}
   [:div.form-group
    [:input
     {:name :url
      :type :hidden
      :value (-> @routing/state* :url)}]]
   [anti-csrf/hidden-form-group-token-component]
   [:div.form-group
    [:label.sr-only
     {:for :sign-out}
     "Sign out"]
    [:button#sign-out.btn.btn-dark.form-group
     {:type :submit
      :style {:padding-top "0.2rem"
              :padding-bottom "0.2rem"}}
     [:span
      [:span " Sign out "]
      [:i.fas.fa-sign-out-alt]]]]])

(defn version-component []
  [:span.navbar-text "Version "
   (let [major (:version_major @state/leihs-admin-version*)
         minor (:version_minor @state/leihs-admin-version*)
         patch (:version_patch @state/leihs-admin-version*)
         pre (:version_pre @state/leihs-admin-version*)
         build (:version_build @state/leihs-admin-version*)]
     [:span
      [:span.major major]
      "." [:span.minor minor]
      "." [:span.patch patch]
      (when pre
        [:span "-"
         [:span.pre pre]])
      (when build
        [:span "+"
         [:span.build build]])])])


(defn fake-sidebar-menu-tree []
  ;; `:id` is for react keys and such
  ;; `:active` means we are on or inside this route
  ;; :link is an object of props for `<a>` - needs at least `:href` of course
  [{:id "manage"
    :label "Manage"
    :submenu
    [{:id "pools"
      :label "Inventory-Pool"
      :link {:href "/admin/inventory-pools/"}}
     {:id "4a1ba40c-467e-5efe-8cf1-e8d3dbb59f04"
      :label "ITZ-Ausstellungen"
      :icon UI/Components.Icon.AdminPool
      :active true
      :link {:href "/admin/inventory-pools/4a1ba40c-467e-5efe-8cf1-e8d3dbb59f04"}
      :submenu
      [{:id "users"
        :label "Users"
        :icon UI/Components.Icon.AdminUsers}
       {:id "groups"
        :label "Groups"
        :icon UI/Components.Icon.AdminGroups
        :active true}
       {:id "delegations"
        :label "Delegations"
        :icon UI/Components.Icon.AdminDelegations}
       {:id "entitlement-groups"
        :label "Entitlement-Groups"
        :icon UI/Components.Icon.AdminEntitlementGroups}
       {:id "mail-templates"
        :label "Mail Templates"
        :icon UI/Components.Icon.AdminMenuItemSettings}
       {:id "fields"
        :label "Fields"
        :icon UI/Components.Icon.AdminMenuItemSettings}]}]}
   {:id "reports"
    :label "reports"
    :submenu
    [{:id "statistics"
      :label "Statistics"
      :icon UI/Components.Icon.AdminStatistics}
     {:id "inventory"
      :label "Inventory Export"
      :icon UI/Components.Icon.AdminExportInventory}
     {:id "status-info"
      :label "Status Info"
      :icon UI/Components.Icon.AdminStatusInfo}
     {:id "audits"
      :label "Audits"
      :icon UI/Components.Icon.AdminAudits
      :submenu
      [{:id "legacy"
        :label "Legacy"
        :icon UI/Components.Icon.AdminAuditsLegacy}
       {:id "audited-changes"
        :label "Audited Changes"
        :icon UI/Components.Icon.AdminAuditedChanges}
       {:id "audited-requests"
        :label "Audited Requests"
        :icon UI/Components.Icon.AdminAuditedRequests}]}]}
   {:id "configuration"
    :label "Configuration"
    :submenu
    [{:label "Fields"
      :icon UI/Components.Icon.AdminMenuItemSettings}
     {:label "Buildings"
      :icon UI/Components.Icon.AdminMenuItemSettings}
     {:label "Rooms"
      :icon UI/Components.Icon.AdminMenuItemSettings}
     {:label "Suppliers"
      :icon UI/Components.Icon.AdminMenuItemSettings}
     {:label "Languages"
      :icon UI/Components.Icon.AdminMenuItemSettings}
     {:label "Mail Templates"
      :icon UI/Components.Icon.AdminMenuItemSettings}]}
   {:id "administration"
    :label "Administration"
    :submenu
    [{:label "Users", :icon UI/Components.Icon.AdminUsers}
     {:label "Groups", :icon UI/Components.Icon.AdminGroups}
     {:label "System-Admins"
      :icon UI/Components.Icon.AdminSystemAdmins}
     {:label "Authentication-Systems"
      :icon UI/Components.Icon.AdminAuthSystems}
     {:id "settings"
      :label "Settings"
      :icon UI/Components.Icon.AdminMenuItemSettings
      :submenu
      [{:label "Languages"
        :icon UI/Components.Icon.AdminLanguages}
       {:label "Miscellaneous"
        :icon UI/Components.Icon.AdminSettingsMisc}
       {:label "SMTP"
        :icon UI/Components.Icon.AdminSettingsSMTP}
       {:label "System & Security"
        :icon UI/Components.Icon.AdminSettingsSystemSecurity}]}]}]
)

(defn main-sidebar []
  [:> UI/Components.DebugProps "hoi"]
  [:> UI/Components.Admin.Sidebar {:menuTree (clj->js (fake-sidebar-menu-tree))}]
  )
(js/console.log UI)


(defn footer []
  [:nav.XXXc-footer.navbar-dark.bg-secondary.mt-4
   [:div.col
    [:a.navbar-brand {:href (path :admin {})} "leihs-admin"]
    [version-component]]
   [:div.col
    [:a.navbar-text
     {:href (path :status)} "Admin-Status-Info"]]
   [state/debug-toggle-navbar-component]
   [:form.form-inline {:style {:margin-left "0.5em"
                               :margin-right "0.5em"}}
    [:label.navbar-text
     [:a {:href (path :requests)}
      [requests/icon-component]
      " Requests "]]]])

(defn current-page []
  [:div {:class "ui-body sidebar-mini layout-fixed layout-navbar-fixed"}
   [:div {:class "wrapper"}
    [main-sidebar]
    [:main {:class "content-wrapper px-4 py-2"}
     [leihs.core.requests.modal/modal-component]
     [leihs.admin.common.http-client.modals/modal-component]

     [:div.container-fluid
      (if-let [page (:page @routing/state*)]
        [page]
        [:div.page
         [:h1.text-danger "Error 404 - There is no handler for the current path defined."]])]

     [state/debug-component]

     [footer]]]])



(defn mount []
  (when-let [app (.getElementById js/document "app")]
    (rdom/render [current-page] app))
  (accountant/dispatch-current!))

