(ns leihs.admin.resources.inventory.front
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go]])
  (:require
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.icons :as icons]

    [leihs.admin.paths :as paths :refer [path paths]]
    [leihs.admin.front.breadcrumbs :as breadcrumbs]
    [leihs.admin.resources.inventory.breadcrumbs :as inventory-breadcrumbs]

    [clojure.string :as str]
    ))


(defn page []
  [:div.page.inventory-page
   [breadcrumbs/nav-component
    [[breadcrumbs/leihs-li]
     [breadcrumbs/admin-li]
     [inventory-breadcrumbs/inventory-li]]
    []]
   [:h1 "Inventory"]
   [:h2 "Export / Download Inventory"]
   [:ul
     [:li [:a {:href (path :inventory-csv) :target :_blank} [:i.fas.fa-download] " CSV"]]
     [:li [:a {:href (path :inventory-quick-csv) :target :_blank} [:i.fas.fa-download] " Quick-CSV"]]
     [:li [:a {:href (path :inventory-excel) :target :_blank} [:i.fas.fa-download] " Excel"]]
     [:li [:a {:href (path :inventory-quick-excel) :target :_blank} [:i.fas.fa-download] " Quick-Excel"]]]])
