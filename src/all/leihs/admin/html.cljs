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

(defn sidebar-dropdown [title props children]
  (let [aprops (apply dissoc props [:active :icon :open])
        active? (:active props)
        icon (:icon props)
        open? (cond (contains? props :open) (:open props) :else true)
        ]
    [:li (merge aprops {:class (str "XXXc-sidebar-nav-dropdown " (when open? " XXXc-show ") (:class aprops))} )
     [:a (merge {:class (str "XXXc-sidebar-nav-link nav-dropdown-toggle " (when active? "XXXc-active"))})
      (when icon [:<> [:i {:class (str "XXXc-sidebar-nav-icon " icon)}] " "])
      title]
     (when (seq children)
       [:ul {:class "XXXc-sidebar-nav-dropdown-items"}
        children])]))
  

(defn sidebar-nav-item [props children]
  (let [nprops (apply dissoc props [:href :badge :icon :active])
        active? (:active props)
        href (:href props)
        icon (:icon props)
        badge (:badge props)]
    [:li (merge nprops {:class (str "XXXc-sidebar-nav-item " (:class nprops))})
     [:a (merge {:class (str "XXXc-sidebar-nav-link " (when active? " XXXc-active ")) :href href})
      (when icon [:<> [:i {:class (str "XXXc-sidebar-nav-icon " icon)}] " "])
      children
      (when badge badge)]]))

(defn fake-lte-sidebar-content []
  [:nav {:class "mt-2"}
   [:ul {:class "nav nav-pills nav-sidebar nav-child-indent flex-column", :data-widget "treeview", :role "menu"}
    [:li {:class "nav-item"}
     [:a {:href "/docs/3.1//index.html", :class "nav-link"}
      [:i {:class "nav-icon fas fa-microchip"}]
      [:p "Installation"]]]
    [:li {:class "nav-item"}
     [:a {:href "/docs/3.1//dependencies.html", :class "nav-link"}
      [:i {:class "nav-icon fas fa-handshake"}]
      [:p "Dependencies & Plugins"]]]
    [:li {:class "nav-item"}
     [:a {:href "/docs/3.1//layout.html", :class "nav-link"}
      [:i {:class "nav-icon fas fa-copy"}]
      [:p "Layout"]]]
    [:li {:class "nav-item menu-is-opening menu-open"}
     [:a {:href "/docs/3.1//components", :class "nav-link active"}
      [:i {:class "nav-icon fas fa-th"}]
      [:p
       "Components"
       [:i {:class "right fas fa-angle-left"}]]]
     [:ul {:class "nav nav-treeview", :style {:display "block"}}
      [:li {:class "nav-item"}
       [:a {:href "/docs/3.1//components/main-header.html", :class "nav-link"}
        [:i {:class "far fa-circle nav-icon"}]
        [:p "Main Header"]]]
      [:li {:class "nav-item"}
       [:a {:href "/docs/3.1//components/main-sidebar.html", :class "nav-link active"}
        [:i {:class "far fa-circle nav-icon"}]
        [:p "Main Sidebar"]]]
      [:li {:class "nav-item"}
       [:a {:href "/docs/3.1//components/control-sidebar.html", :class "nav-link"}
        [:i {:class "far fa-circle nav-icon"}]
        [:p "Control Sidebar"]]]
      [:li {:class "nav-item"}
       [:a {:href "/docs/3.1//components/cards.html", :class "nav-link"}
        [:i {:class "far fa-circle nav-icon"}]
        [:p "Card"]]]
      [:li {:class "nav-item"}
       [:a {:href "/docs/3.1//components/boxes.html", :class "nav-link"}
        [:i {:class "far fa-circle nav-icon"}]
        [:p "Small-/ Info-Box"]]]
      [:li {:class "nav-item"}
       [:a {:href "/docs/3.1//components/direct-chat.html", :class "nav-link"}
        [:i {:class "far fa-circle nav-icon"}]
        [:p "Direct Chat"]]]
      [:li {:class "nav-item"}
       [:a {:href "/docs/3.1//components/timeline.html", :class "nav-link"}
        [:i {:class "far fa-circle nav-icon"}]
        [:p "Timeline"]]]
      [:li {:class "nav-item"}
       [:a {:href "/docs/3.1//components/ribbons.html", :class "nav-link"}
        [:i {:class "far fa-circle nav-icon"}]
        [:p "Ribbons"]]]
      [:li {:class "nav-item"}
       [:a {:href "/docs/3.1//components/miscellaneous.html", :class "nav-link"}
        [:i {:class "far fa-circle nav-icon"}]
        [:p "Miscellaneous"]]]
      [:li {:class "nav-item"}
       [:a {:href "/docs/3.1//components/plugins.html", :class "nav-link"}
        [:i {:class "far fa-circle nav-icon"}]
        [:p "Plugins"]]]]]
    [:li {:class "nav-item"}
     [:a {:href "/docs/3.1//javascript", :class "nav-link"}
      [:i {:class "nav-icon fas fa-code"}]
      [:p
       [:i {:class "right fas fa-angle-left"}]]]
     [:ul {:class "nav nav-treeview"}
      [:li {:class "nav-item"}
       [:a {:href "/docs/3.1//javascript/layout.html", :class "nav-link"}
        [:i {:class "far fa-circle nav-icon"}]
        [:p "Layout"]]]
      [:li {:class "nav-item"}
       [:a {:href "/docs/3.1//javascript/push-menu.html", :class "nav-link"}
        [:i {:class "far fa-circle nav-icon"}]
        [:p "Push Menu"]]]
      [:li {:class "nav-item"}
       [:a {:href "/docs/3.1//javascript/treeview.html", :class "nav-link"}
        [:i {:class "far fa-circle nav-icon"}]
        [:p "Treeview"]]]
      [:li {:class "nav-item"}
       [:a {:href "/docs/3.1//javascript/card-widget.html", :class "nav-link"}
        [:i {:class "far fa-circle nav-icon"}]
        [:p "Card Widget"]]]
      [:li {:class "nav-item"}
       [:a {:href "/docs/3.1//javascript/card-refresh.html", :class "nav-link"}
        [:i {:class "far fa-circle nav-icon"}]
        [:p "CardRefresh"]]]
      [:li {:class "nav-item"}
       [:a {:href "/docs/3.1//javascript/control-sidebar.html", :class "nav-link"}
        [:i {:class "far fa-circle nav-icon"}]
        [:p "Control Sidebar"]]]
      [:li {:class "nav-item"}
       [:a {:href "/docs/3.1//javascript/direct-chat.html", :class "nav-link"}
        [:i {:class "far fa-circle nav-icon"}]
        [:p "Direct Chat"]]]
      [:li {:class "nav-item"}
       [:a {:href "/docs/3.1//javascript/todo-list.html", :class "nav-link"}
        [:i {:class "far fa-circle nav-icon"}]
        [:p "Todo List"]]]
      [:li {:class "nav-item"}
       [:a {:href "/docs/3.1//javascript/toasts.html", :class "nav-link"}
        [:i {:class "far fa-circle nav-icon"}]
        [:p "Toasts"]]]
      [:li {:class "nav-item"}
       [:a {:href "/docs/3.1//javascript/sidebar-search.html", :class "nav-link"}
        [:i {:class "far fa-circle nav-icon"}]
        [:p "Sidebar Search"]]]
      [:li {:class "nav-item"}
       [:a {:href "/docs/3.1//javascript/expandable-tables.html", :class "nav-link"}
        [:i {:class "far fa-circle nav-icon"}]
        [:p "Expandable Tables"]]]
      [:li {:class "nav-item"}
       [:a {:href "/docs/3.1//javascript/iframe.html", :class "nav-link"}
        [:i {:class "far fa-circle nav-icon"}]
        [:p "IFrame"]]]]]
    [:li {:class "nav-item"}
     [:a {:href "/docs/3.1//browser-support.html", :class "nav-link"}
      [:i {:class "nav-icon fab fa-chrome"}]
      [:p "Browser Support\n                "]]]
    [:li {:class "nav-item"}
     [:a {:href "/docs/3.1//implementations.html", :class "nav-link"}
      [:i {:class "nav-icon fas fa-bookmark"}]
      [:p]]]
    [:li {:class "nav-item"}
     [:a {:href "/docs/3.1//additional-styles.html", :class "nav-link"}
      [:i {:class "nav-icon fas fa-mortar-pestle"}]
      [:p "Additional Styles\n                "]]]
    [:li {:class "nav-item"}
     [:a {:href "/docs/3.1//upgrade-guide.html", :class "nav-link"}
      [:i {:class "nav-icon fas fa-hand-point-up"}]
      [:p "Upgrade Guide\n                "]]]
    [:li {:class "nav-item"}
     [:a {:href "/docs/3.1//faq.html", :class "nav-link"}
      [:i {:class "nav-icon fas fa-question-circle"}]
      [:p]]]
    [:li {:class "nav-item"}
     [:a {:href "/docs/3.1//license.html", :class "nav-link"}
      [:i {:class "nav-icon fas fa-file-contract"}]
      [:p]]]]])

(defn main-sidebar []
  (let [open? true]
    [:aside {:class "main-sidebar sidebar-dark-primary XXXelevation-4"}
     [:nav.sidebar {:class (str "sidebar " (when open? "XXXc-sidebar-show"))}

      #_[fake-lte-sidebar-content]
      
      [:nav {:class "mt-2"}
       [:ul {:class "nav nav-pills nav-sidebar nav-child-indent flex-column", :data-widget "treeview", :role "menu"}
        [:li {:class "nav-item menu-is-opening menu-open"}
         [:a {:href "/docs/3.1//components", :class "nav-link active"}
          [:i {:class "nav-icon fas fa-th"}]
          [:p "Components"
           [:i {:class "right fas fa-angle-left"}]]]
         [:ul {:class "nav nav-treeview", :style {:display "block"}}
          
          [breadcrumbs/li :admin-buildings " Buildings " {} {} :authorizers [auth/admin-scopes?]]
          
          [breadcrumbs-inventory-pools/inventory-pools-li]
          
          
          #_[:li {:class "nav-item"}
           [:a {:href "/docs/3.1//components/main-header.html", :class "nav-link"}
            [:i {:class "far fa-circle nav-icon"}]
            [:p "Main Header"]]]
          ]]]]

      #_[:div {:class "XXXsidebar" #_:style #_{:background "#1b261a"}}

         [:ul {:class "nav flex-column mb-2"}

          [:li {:class "XXXc-sidebar-nav-title bg-light text-dark"} "Manage"]

          [sidebar-nav-item {:href "#", :active false, :icon "fas fa-cubes"} "Inventory-Pools"]
    ;  [sidebar-nav-item {:href "#", :class "" :active true, :icon "fas fa-cube"} ]
          [:li.XXXc-sidebar-nav-dropdown.XXXc-show
           [:a (merge {:class (str "XXXc-sidebar-nav-link nav-dropdown-toggle XXXc-active font-bold")})
            [:<> [:i {:class (str "XXXc-sidebar-nav-icon fas fa-cube")}] " "]
            "ITZ-Ausstellungen"]
           [:div.pl-5
            [sidebar-nav-item {:href "#", :icon "fas fa-user-friends"} "Users"]
            [sidebar-nav-item {:href "#", :icon "fas fa-users"} "Groups"]
            [sidebar-nav-item {:href "#", :icon "fas fa-hands-helping"} "Delegations"]
            [sidebar-nav-item {:href "#", :icon "fas fa-hands"} "Entitlement-Groups"]
            [sidebar-nav-item {:href "#", :icon "fas fa-list"} "Mail Templates"]
            [sidebar-nav-item {:href "#", :icon "fas fa-list"} "Fields"]]]]]

      #_[:div {:class "XXXc-sidebar XXXc-sidebar-dark XXXc-sidebar-show" #_:style #_{:background "#1b261a"}}

         [:ul {:class "nav flex-column mb-2"}

          [:li {:class "XXXc-sidebar-nav-title"} "Reports"]

          [sidebar-nav-item {:href "#", :icon "fas fa-chart-line"} "Statistics"]
          [sidebar-nav-item {:href "#", :icon "fas fa-cube"} "Inventory"]
          [sidebar-nav-item {:href "#", :icon "fas fa-thermometer-half"} "Status Info"]
          [sidebar-dropdown "Audits" {:icon "fas fa-history" :open false}
           [:<>
            [sidebar-nav-item {:href "#", :icon "fas fa-history"} "Legacy"]
            [sidebar-nav-item {:href "#", :icon "fas fa-save"} "Audited Changes"]
            [sidebar-nav-item {:href "#", :icon "fas fa-exchange-alt"} "Audited Requests"]
            #_[sidebar-nav-item {:href "#"} " Three"]]]

          [:li {:class "XXXc-sidebar-nav-title"} "Configuration"]

          [sidebar-nav-item {:href "#", :icon "fas fa-list"} "Fields"]
          [sidebar-nav-item {:href "#", :icon "fas fa-list"} "Buildings"]
          [sidebar-nav-item {:href "#", :icon "fas fa-list"} "Rooms"]
          [sidebar-nav-item {:href "#", :icon "fas fa-list"} "Suppliers"]
          [sidebar-nav-item {:href "#", :icon "fas fa-list"} "Languages"]
          [sidebar-nav-item {:href "#", :icon "fas fa-list"} "Mail Templates"]


          [:li {:class "XXXc-sidebar-nav-title"} "Administration"]
          [sidebar-nav-item {:href "#", :icon "fas fa-user-friends"} "Users"]
          [sidebar-nav-item {:href "#", :icon "fas fa-users"} "Groups"]
          [sidebar-nav-item {:href "#", :icon "fas fa-user-astronaut"} "System-Admins"]
          [sidebar-nav-item {:href "#", :icon "fas fa-id-card"} "Authentication-Systems"]

          [sidebar-dropdown "Settings" {:open true}
           [:<>
            [sidebar-nav-item {:href "#", :icon "fas fa-globe"} "Languages"]
            [sidebar-nav-item {:href "#", :icon "fas fa-box-open"} "Miscellaneous"]
            [sidebar-nav-item {:href "#", :icon "fas fa-paper-plane"} "SMTP"]
            [sidebar-nav-item {:href "#", :icon "fas fa-shield-alt"} "System & Security"]
            #_[sidebar-nav-item {:href "#"} " Three"]]]

    ; [sidebar-dropdown "A Menu" {}
    ;  [:<>
    ;   [sidebar-nav-item {:href "#" :badge [:span {:class "badge badge-primary"} "YO"]} "One"]
    ;   [sidebar-nav-item {:href "#"} "Two"]
    ;   [sidebar-nav-item {:href "#"} #_[:i {:class "XXXc-sidebar-nav-icon cil-puzzle"}] " Three"]]]
          ]
         [:button {:class "XXXc-sidebar-minimizer XXXc-brand-minimizer", :type "button"}]]]]))

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

