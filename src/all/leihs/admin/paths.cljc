(ns leihs.admin.paths
  (:refer-clojure :exclude [str keyword])
  (:require
    [leihs.admin.utils.core :refer [keyword str presence]]
    [bidi.verbose :refer [branch param leaf]]
    [bidi.bidi :refer [path-for match-route]]
    [leihs.admin.utils.url.query-params :refer [encode-query-params]]

    #?@(:clj
         [[uritemplate-clj.core :as uri-templ]
          [clojure.tools.logging :as logging]
          [logbug.catcher :as catcher]
          [logbug.debug :as debug]
          [logbug.thrown :as thrown]
          ])))


(def paths
  (branch ""
          (leaf "/" :leihs)
          (branch "/auth"
                  (leaf "" :auth)
                  (leaf "/shib-sign-in" :auth-shib-sign-in)
                  (leaf "/password-sign-in" :auth-password-sign-in)
                  (leaf "/sign-out" :auth-sign-out))
          (leaf "/procure" :procure)
          (leaf "/manage" :lend)
          (leaf "/borrow" :borrow)
          (branch "/admin"
                  (leaf "/status" :status)
                  (leaf "/shutdown" :shutdown)
                  (leaf "/initial-admin" :initial-admin)
                  (branch "/debug"
                          (leaf "" :debug)
                          (branch "/requests"
                                  (leaf "/" :requests)
                                  (branch "/" (param :id)
                                          (leaf "" :request))))
                  (leaf "/" :admin)
                  (leaf "/users/new" :user-new)
                  (leaf "/users/" :users)
                  (branch "/users/"
                          (param :user-id)
                          (leaf "" :user)
                          (leaf "/delete" :user-delete)
                          (leaf "/edit" :user-edit)
                          (branch "/api-tokens/"
                                  (leaf "" :api-tokens)
                                  (leaf "new" :api-token-new)
                                  (branch ""
                                          (param :api-token-id)
                                          (leaf "" :api-token)
                                          (leaf "/delete" :api-token-delete)
                                          (leaf "/edit" :api-token-edit)))
                          (branch "/transfer/"
                                  (param :target-user-id)
                                  (leaf "" :user-transfer-data))))
          (leaf "/" :redirect-to-root)))

;(path-for (paths) :user :user-id "{user-id}")
;(match-route (paths) "/users/512")
;(match-route (paths) "/?x=5#7")

(defn path
  ([kw]
   (path-for paths kw))
  ([kw route-params]
   (apply (partial path-for paths kw)
          (->> route-params (into []) flatten)))
  ([kw route-params query-params]
   (str (path kw route-params) "?"
        (encode-query-params query-params))))

