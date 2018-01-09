(ns leihs.admin.utils.url.query-params
  (:refer-clojure :exclude [str keyword encode])
  (:require
    [leihs.admin.utils.core :refer [keyword str presence]]

    [clojure.walk :refer [keywordize-keys]]
    [clojure.string :as string]

    #?(:clj [ring.util.codec])

    ))

(def decode #?(:cljs js/decodeURIComponent))

(def encode
  #?(
     :cljs js/encodeURIComponent
     :clj ring.util.codec/url-encode))

(defn try-parse-json [x]
  #?(:cljs
      (try (-> x js/JSON.parse js->clj)
           (catch js/Object _ x))))


(defn to-json [x]
  #?(:cljs
      (.stringify js/JSON x)))

(defn decode-query-params [query-string]
  (->> (if-not (presence query-string) [] (string/split query-string #"&"))
       (reduce
         (fn [m part]
           (let [[k v] (string/split part #"=" 2)]
             (assoc m (-> k decode keyword) (-> v decode try-parse-json))))
         {})
       keywordize-keys))

(defn encode-query-params [params]
  (->> params
       (map (fn [[k v]]
              (str (-> k str encode)
                   "="
                   (-> (if (coll? v) (to-json v) v)
                       str encode))))
       (clojure.string/join "&")))
