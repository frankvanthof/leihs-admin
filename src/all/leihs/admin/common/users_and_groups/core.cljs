(ns leihs.admin.common.users-and-groups.core
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go]])
  (:require
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.routing.front :as routing]

    [leihs.admin.common.components :as components]
    [leihs.admin.common.form-components :refer [checkbox-component input-component]]
    [leihs.admin.common.http-client.core :as http]
    [leihs.admin.defaults :as defaults]
    [leihs.admin.paths :as paths :refer [path]]
    [leihs.admin.state :as state]
    [leihs.admin.utils.misc :as front-shared :refer [wait-component]]
    [leihs.admin.utils.seq :as seq]
    [leihs.core.user.front :as current-user]

    [clojure.string :as str]
    [accountant.core :as accountant]
    [cljs.core.async :as async :refer [timeout]]
    [cljs.pprint :refer [pprint]]
    [reagent.core :as reagent]))


(defn organizations [data*]
  (merge {"" "(any)"}
         (some-> @routing/state* :query-params-raw :organization presence
                 (#(do {% %})))
         (some-> @data* (get (:url @routing/state*))
                 :meta :organizations
                 (->> (map presence)
                      (filter identity)
                      (map #(do [% %]))
                      (into {})))))

(defn form-org-filter [data*]
  [routing/select-component
   :label "Organization"
   :query-params-key :organization
   :options (organizations data*)])

(defn form-org-id-filter []
  [routing/delayed-query-params-input-component
   :label "Organization ID"
   :query-params-key :org_id
   :input-options
   {:placeholder "id within the org"}])

(defn protected-filter []
  [routing/select-component
   :label "Protected by"
   :query-params-key :protected
   :options {"" "(any or none)"
             :none "(none)"
             :admin :admin
             :system-admin :system-admin}])


(defn protect-form-fiels-row-component [data*]
  [:div.form-row
   [:div.col-md-6
    [checkbox-component data* [:admin_protected]
     :disabled (not @current-user/admin?*)
     :label "Leihs admin protected"
     :hint [:span "An admin protected entity can only be modifed by admins and in particular not by inventory-pool staff. "
            "This is often used for entities which are automatically managed via the API. "]]]
   [:div.col-md-6
    [checkbox-component data* [:system_admin_protected]
     :disabled (not @current-user/system-admin?*)
     :label "System admin protected"
     :hint [:span "This entity can only be modifed by system-admins. "]]]])

(defn org-form-fields-row-component [data*]
  [:div.form-row
   [:div.col-md
    [input-component data* [:organization]
     :label "Organization"
     :disabled (not @current-user/admin?*)
     :hint [:span "Is is recommended to set this to "
            [:strong  [:em "leihs-local"]] " for locally managed entities "
            "or to the corresponding " [:strong " domain name "] " for automatically managed entitie. "
            "This value must " [:strong  "not be empty"] ". "
            "The characters " [:strong " a-z, 0-9, hyphens, and dots are allowed. "]
            "The organization name " [:strong [:em "leihs-core"] " is reserved "] " and may not be used."]]]
   [:div.col-md
    [input-component data* [:org_id]
     :label "Organizational ID"
     :disabled (not @current-user/admin?*)
     :hint [:span
            "This field may " [:strong " not contain a " [:span.text-monospace "@"] " sign."]
            "The " [:strong " combination of organization and " [:span.text-monospace "org_id"]]
            " must be " [:strong " unique accross all entities"] "."]]]])




