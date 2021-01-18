(ns leihs.admin.html
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    )
  (:require
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
    ; TODO remove the following
    [leihs.admin.resources.main :as admin :refer [page] :rename {page home-page}]

    [clojure.pprint :refer [pprint]]
    [accountant.core :as accountant]
    [reagent.core :as reagent]
    ))

(defn li-navitem [handler-key display-string]
  (let [active? (= (-> @routing/state* :handler-key) handler-key)]
    [:li.nav-item
     {:class (if active? "active" "")}
     [:a.nav-link {:href (path handler-key)} display-string]]))

(defn li-admin-navitem []
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
    [:li (merge aprops {:class (str "c-sidebar-nav-dropdown " (when open? " c-show ") (:class aprops))} )
     [:a (merge {:class (str "c-sidebar-nav-link nav-dropdown-toggle " (when active? "c-active"))})
      (when icon [:<> [:i {:class (str "c-sidebar-nav-icon " icon)}] " "])
      title]
     (when (seq children)
       [:ul {:class "c-sidebar-nav-dropdown-items"}
        children])]))
  

(defn sidebar-nav-item [props children]
  (let [nprops (apply dissoc props [:href :badge :icon :active])
        active? (:active props)
        href (:href props)
        icon (:icon props)
        badge (:badge props)]
    [:li (merge nprops {:class (str "c-sidebar-nav-item " (:class nprops))})
     [:a (merge {:class (str "c-sidebar-nav-link " (when active? " c-active ")) :href href})
      (when icon [:<> [:i {:class (str "c-sidebar-nav-icon " icon)}] " "])
      children
      (when badge badge)]]))

(defn sidebar []
  (let [open? true]
    [:nav.c-sidebar {:class (when open? "c-sidebar-show")}
   [:div {:class "c-sidebar c-sidebar-light c-sidebar-show" #_:style #_{:background "#1b261a"}}

    [:ul {:class "c-sidebar-nav"}

     [:li {:class "c-sidebar-nav-title bg-light text-dark"} "Manage"]

     [sidebar-nav-item {:href "#", :active false, :icon "fas fa-cubes"} "Inventory-Pools"]
    ;  [sidebar-nav-item {:href "#", :class "" :active true, :icon "fas fa-cube"} ]
     [:li.c-sidebar-nav-dropdown.c-show
      [:a (merge {:class (str "c-sidebar-nav-link nav-dropdown-toggle c-active font-bold")})
       [:<> [:i {:class (str "c-sidebar-nav-icon fas fa-cube")}] " "]
       "ITZ-Ausstellungen"]
      [:div.pl-5
       [sidebar-nav-item {:href "#", :icon "fas fa-user-friends"} "Users"]
       [sidebar-nav-item {:href "#", :icon "fas fa-users"} "Groups"]
       [sidebar-nav-item {:href "#", :icon "fas fa-hands-helping"} "Delegations"]
       [sidebar-nav-item {:href "#", :icon "fas fa-hands"} "Entitlement-Groups"]
       [sidebar-nav-item {:href "#", :icon "fas fa-list"} "Mail Templates"]
       [sidebar-nav-item {:href "#", :icon "fas fa-list"} "Fields"]
       ]]]]

   [:div {:class "c-sidebar c-sidebar-dark c-sidebar-show" #_:style #_{:background "#1b261a"}}
    [:ul {:class "c-sidebar-nav"}

     [:li {:class "c-sidebar-nav-title"} "Reports"]

     [sidebar-nav-item {:href "#", :icon "fas fa-chart-line"} "Statistics"]
     [sidebar-nav-item {:href "#", :icon "fas fa-cube"} "Inventory"]
     [sidebar-nav-item {:href "#", :icon "fas fa-thermometer-half"} "Status Info"]
     [sidebar-dropdown "Audits" {:icon "fas fa-history" :open false}
      [:<>
       [sidebar-nav-item {:href "#", :icon "fas fa-history"} "Legacy"]
       [sidebar-nav-item {:href "#", :icon "fas fa-save"} "Audited Changes"]
       [sidebar-nav-item {:href "#", :icon "fas fa-exchange-alt"} "Audited Requests"]
       #_[sidebar-nav-item {:href "#"} " Three"]]]

     [:li {:class "c-sidebar-nav-title"} "Configuration"]

     [sidebar-nav-item {:href "#", :icon "fas fa-list"} "Fields"]
     [sidebar-nav-item {:href "#", :icon "fas fa-list"} "Buildings"]
     [sidebar-nav-item {:href "#", :icon "fas fa-list"} "Rooms"]
     [sidebar-nav-item {:href "#", :icon "fas fa-list"} "Suppliers"]
     [sidebar-nav-item {:href "#", :icon "fas fa-list"} "Languages"]
     [sidebar-nav-item {:href "#", :icon "fas fa-list"} "Mail Templates"]


     [:li {:class "c-sidebar-nav-title"} "Administration"]
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
    ;   [sidebar-nav-item {:href "#"} #_[:i {:class "c-sidebar-nav-icon cil-puzzle"}] " Three"]]]
     ]
    [:button {:class "c-sidebar-minimizer c-brand-minimizer", :type "button"}]]]))

(defn footer []
  [:nav.c-footer.navbar-dark.bg-secondary.mt-4
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
  [:div {:class "c-app"}
   [sidebar]
   [:div {:class "c-wrapper"}
    [leihs.core.requests.modal/modal-component]
    [leihs.admin.common.http-client.modals/modal-component]

    
     [:div.c-body
      [:main.c-main
       [:div.container-fluid 
        (if-let [page (:page @routing/state*)]
        [page]
        [:div.page
          [:h1.text-danger "Error 404 - There is no handler for the current path defined."]])]
     
    [state/debug-component]]]
    
   [footer]]])



(defn mount []
  (when-let [app (.getElementById js/document "app")]
    (reagent/render [current-page] app))
  (accountant/dispatch-current!))

