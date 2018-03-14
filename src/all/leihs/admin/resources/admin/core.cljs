(ns leihs.admin.resources.admin.core
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    )
  (:require
    [leihs.admin.front.state :as state :refer [routing-state*]]
    [leihs.admin.resources.auth.core :as auth]
    [leihs.admin.front.breadcrumbs :as breadcrumbs]
    [leihs.admin.paths :as paths]
    [leihs.admin.utils.core :refer [keyword str presence]]
    ))

(defn page []
  [:div.root

   (when-let [user @state/user*]
     (breadcrumbs/nav-component
       [(breadcrumbs/leihs-li)
        (breadcrumbs/admin-li)]
       [(breadcrumbs/auth-li)
        (breadcrumbs/settings-li)
        (breadcrumbs/users-li)]))

   [:div
    [:h1 "leihs administriers"]
    [:p "The application with programming interface to "
     "administrate this instance of "
     [:em " leihs"]"."]]

   (when-not @state/user*
     [auth/sign-in-form-component])

   ])
