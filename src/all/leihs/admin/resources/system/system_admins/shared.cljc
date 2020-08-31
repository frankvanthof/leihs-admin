(ns leihs.admin.resources.system.system-admins.shared
  (:require
    [leihs.admin.defaults :as defaults]))

(def default-fields
  #{
    :enabled
    :id
    :name
    :priority
    :type
    })

(def available-fields
  #{
    :created_at
    :description
    :enabled
    :id
    :name
    :priority
    :type
    })

(def default-query-params {:page 1 :per-page defaults/PER-PAGE})

(defn normalized-query-parameters [query-params]
  (merge default-query-params query-params))

