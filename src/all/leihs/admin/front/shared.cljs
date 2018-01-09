(ns leihs.admin.front.shared
  (:refer-clojure :exclude [str keyword])
  (:require
    [leihs.admin.front.state :as state]
    [leihs.admin.utils.core :refer [keyword str presence]]
    [leihs.admin.utils.digest]

    [goog.string :as gstring]))

(defn humanize-datetime [ref_dt dt]
  (.to (js/moment) dt))

(defn humanize-datetime-component [dt]
  (if-let [dt (if (string? dt) (js/moment dt) dt)]
    [:span.datetime
     {:data-iso8601 (.format dt)}
     ;[:pre (with-out-str (pprint dt))]
     (humanize-datetime (:timestamp @state/global-state*) dt)]
    [:span "NULL"]))

(defn short-id [uuid]
  [:span {:style {:font-family :monospace}}
   (->> uuid (take 8) clojure.string/join)])

(defn gravatar-url
  ([email]
   (gravatar-url email 32))
  ([email size]
   (if-not (presence email)
     (gstring/format
       "https://www.gravatar.com/avatar/?s=%d&d=blank" size)
     (let [md5 (->> email
                    clojure.string/trim
                    clojure.string/lower-case
                    leihs.admin.utils.digest/md5-hex)]
       (gstring/format
         "https://www.gravatar.com/avatar/%s?s=%d&d=retro"
         md5 size)))))

