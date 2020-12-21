(ns leihs.admin.resources.debug
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    )
  (:require
    [leihs.admin.paths :as paths]
    [leihs.admin.common.breadcrumbs :as breadcrumbs]

    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.url.query-params :as query-params]
    [clojure.pprint :refer [pprint]]
    ))


(defn page []
  [:div.debug

   [:div.row
    [:nav.col-lg {:aria-label :breadcrumb :role :navigation}
     [:ol.breadcrumb
      [breadcrumbs/leihs-li]
      [breadcrumbs/admin-li]
      [breadcrumbs/debug-li]]]
    [:nav.col-lg {:role :navigation}
     [:ol.breadcrumb.leihs-nav-right
      [breadcrumbs/requests-li]]]]


   [:h1 "Debug"]
   [:pre
    (with-out-str
      (pprint
        (.parse js/JSON
                (->  js/document .-body .-dataset .-user))
        ))]])
