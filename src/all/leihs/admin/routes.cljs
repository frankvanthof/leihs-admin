(ns leihs.admin.routes
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    )
  (:require
    [leihs.admin.front.pages.debug]
    [leihs.admin.front.pages.leihs]
    [leihs.admin.front.requests.pages.request]
    [leihs.admin.front.requests.pages.requests]
    [leihs.admin.front.state :refer [routing-state*]]
    [leihs.admin.paths :refer [path paths]]
    [leihs.admin.resources.api-tokens.front :as api-tokens]
    [leihs.admin.resources.api-token.front :as api-token]
    [leihs.admin.resources.admin.core :as admin]
    [leihs.admin.resources.auth.core :as auth]
    [leihs.admin.resources.initial-admin.core :as initial-admin]
    [leihs.admin.resources.user.front :as user]
    [leihs.admin.resources.users.front :as users]
    [leihs.admin.utils.core :refer [keyword str presence]]
    [leihs.admin.utils.url.query-params :refer [decode-query-params]]

    [accountant.core :as accountant]
    [bidi.bidi :as bidi]
    [cljsjs.js-yaml]
    [clojure.pprint :refer [pprint]]
    [reagent.core :as reagent]
    ))

(def resolve-table
  {
   :admin #'admin/page
   :api-token-new #'api-token/new-page
   :api-token #'api-token/show-page
   :api-token-edit #'api-token/edit-page
   :api-token-delete #'api-token/delete-page
   :api-tokens #'api-tokens/page
   :auth #'auth/auth-page
   :auth-password-sign-in #'auth/password-sign-in-page
   :debug #'leihs.admin.front.pages.debug/page
   :initial-admin #'initial-admin/page
   :leihs #'leihs.admin.front.pages.leihs/page
   :request #'leihs.admin.front.requests.pages.request/page
   :requests #'leihs.admin.front.requests.pages.requests/page
   :user #'user/show-page
   :user-delete #'user/delete-page
   :user-edit #'user/edit-page
   :user-new #'user/new-page
   :users #'users/page
   })

(defn resolve-page [k]
  (get resolve-table k nil))

(defn match-path [path]
  (bidi/match-route paths path))

(defn init-navigation []
  (accountant/configure-navigation!
    {:nav-handler (fn [path]
                    (let [{route-params :route-params
                           handler-key :handler} (match-path path)
                          location-href (-> js/window .-location .-href)
                          location-url (goog.Uri. location-href)]
                      (swap! routing-state* assoc
                             :route-params route-params
                             :handler-key handler-key
                             :page (resolve-page handler-key)
                             :url location-href
                             :path (.getPath location-url)
                             :query-params (-> location-url .getQuery decode-query-params))
                      ;(js/console.log (with-out-str (pprint [handler-key route-params])))
                      ))
     :path-exists? (fn [path]
                     (boolean (match-path path)))}))

(defn init []
  (init-navigation)
  (accountant/dispatch-current!))
