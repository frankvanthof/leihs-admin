(ns leihs.admin.front.dom
  (:refer-clojure :exclude [str keyword])
  (:require
    [leihs.admin.utils.core :refer [keyword str presence]]
    [leihs.admin.utils.url.core :as url]

    [goog.dom :as dom]
    [goog.dom.dataset :as dataset]
    ))

(defn data-attribute
  "Retrieves JSON and urlencoded data attribute with attribute-name
  from the first element with element-name."
  [element-name attribute-name]
  (try (-> (.getElementsByTagName js/document element-name)
           (aget 0)
           (dataset/get attribute-name)
           url/decode
           (#(.parse js/JSON %))
           cljs.core/js->clj
           clojure.walk/keywordize-keys)
       (catch js/Object e
         (js/console.log e)
         nil)))

