(ns leihs.admin.resources.settings.front
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


;;; helpers ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def settings* (atom nil))

(def fetch-settings-id* (reagent/atom nil))

(defn fetch-settings [& args]
  (let [resp-chan (async/chan)
        id (requests/send-off {:url (path :settings)
                               :method :get}
                              {:modal false
                               :title "Fetch Settings"
                               :handler-key :settings
                               :retry-fn #'fetch-settings}
                              :chan resp-chan)]
    (reset! fetch-settings-id* id)
    (go (let [resp (<! resp-chan)]
          (when (and (= (:status resp) 200) ;success
                     (= id @fetch-settings-id*) ;still the most recent request
                     (reset! settings* (->> resp :body))))))))

(defn debug-component []
  (when (:debug @state/global-state*)
    [:div.debug
     [:hr]
     [:div
      [:h3 "@settings*"]
      [:pre (with-out-str (pprint @settings*))]]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn auth-page []
  [:div.auth-settings
   [state/hidden-routing-state-component
    {:will-mount (fn [] (reset! settings* nil))
     :did-change (fn [old diff new]
                   (js/console.log (with-out-str (pprint  diff)))
                   (fetch-settings))
     :did-mount fetch-settings}]
   (breadcrumbs/nav-component
     [(breadcrumbs/leihs-li)
      (breadcrumbs/admin-li)
      (breadcrumbs/settings-li)
      (breadcrumbs/settings-auth-li)]
     [])
   [:h1 "Authentication-System Settings"]

   [:h2 "Session Settings"]

   [debug-component]
   ])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn email-page []
  [:div.email-settings
   (breadcrumbs/nav-component
     [(breadcrumbs/leihs-li)
      (breadcrumbs/admin-li)
      (breadcrumbs/settings-li)
      (breadcrumbs/settings-email-li)]
     [])
   [:h1 "Email-System Settings"]
   [:div.alert.alert-danger {:role :alert}
    [:p "After saving "
     [:b "changes require a restart "]
     "of the leihs-legacy service to take effect! "
     "You can also restart your whole leihs-server."]]])


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn page []
  [:div.settings
   (breadcrumbs/nav-component
     [(breadcrumbs/leihs-li)
      (breadcrumbs/admin-li)
      (breadcrumbs/settings-li)]
     [(breadcrumbs/settings-auth-li)
      (breadcrumbs/settings-email-li)])
   [:h1 "Settings"]
   [:div.alert.alert-warning {:role :alert}
    [:p "Some settings can also be altered from the leihs deployment system. "
     "Settings defined via deployment will override settings performed manually here." ]]])
