(ns leihs.admin.resources.inventory-pools.shared
  (:refer-clojure :exclude [str keyword])
  (:require
    [leihs.core.core :refer [keyword str presence]]
    [leihs.admin.paths :refer [path]]
    [leihs.admin.defaults :as defaults]))

(def default-fields
  #{
    :is_active
    :id
    :name
    })

(def available-fields
  #{
    :address_id
    :automatic_access
    :automatic_suspension
    :automatic_suspension_reason
    :color
    :contact_details
    :contract_description
    :contract_url
    :default_contract_note
    :description
    :email
    :id
    :is_active
    :logo_url
    :name
    :opening_hours
    :print_contracts
    :required_purpose
    :shortname
    })


(def default-query-params
  {:page 1
   :per-page defaults/PER-PAGE
   :is-active "true"
   :order [["name" "asc"] ["id" "asc"]]
   :term nil})

(def inventory-pool-path (path :inventory-pool {:inventory-pool-id ":inventory-pool-id"}))

(defn normalized-query-parameters [query-params]
  (merge default-query-params query-params))
