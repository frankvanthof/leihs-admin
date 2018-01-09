(ns leihs.admin.front.pages.leihs
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    )
  (:require
    [leihs.admin.paths :as paths]

    [leihs.admin.front.breadcrumbs :as breadcrumbs]
    [leihs.admin.utils.core :refer [keyword str presence]]

    [leihs.admin.front.breadcrumbs :as breadcrumbs]

    ))

(defn page []
  [:div.root
   [:div.row
    [:nav.col-lg {:aria-label :breadcrumb :role :navigation}
     [:ol.breadcrumb
      [breadcrumbs/leihs-li]
      ]]
    [:nav.col-lg {:role :navigation}
     [:ol.breadcrumb.leihs-nav-right
      [breadcrumbs/borrow-li]
      [breadcrumbs/lend-li]
      [breadcrumbs/procure-li]
      [breadcrumbs/admin-li]]]]

   [:h1 "leihs - Equipment Booking and Inventory Management System"]
   [:p  "Manage inventory, place reservations on items and pick them up."]])
