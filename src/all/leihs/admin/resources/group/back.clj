(ns leihs.admin.resources.group.back
  (:refer-clojure :exclude [str keyword])
  (:require [leihs.core.core :refer [keyword str presence]])
  (:require
    [leihs.admin.paths :refer [path]]
    [leihs.core.sql :as sql]
    [clojure.set :refer [rename-keys]]
    [clojure.java.jdbc :as jdbc]
    [compojure.core :as cpj]



    [clj-logging-config.log4j :as logging-config]
    [clojure.tools.logging :as logging]
    [logbug.debug :as debug]
    [logbug.catcher :as catcher]
    )
  (:import
    [java.awt.image BufferedImage]
    [java.io ByteArrayInputStream ByteArrayOutputStream]
    [java.util Base64]
    [javax.imageio ImageIO]
    ))

;;; data keys ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def group-selects
  [:groups.id
   :name
   :description
   :org_id
   [(-> (sql/select :%count.*)
        (sql/from :groups_users)
        (sql/merge-where [:= :groups_users.group_id :groups.id]))
    :users_count]
   [(-> (sql/select :%count.*)
        (sql/from :group_access_rights)
        (sql/merge-where [:= :groups.id :group_access_rights.group_id]))
    :inventory_pools_roles_count]
   :created_at
   :updated_at])

(def group-write-keys
  [:name
   :description
   :org_id])

(def group-write-keymap
  {})


;;; group ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn group-query [group-id]
  (-> (apply sql/select group-selects)
      (sql/from :groups)
      (sql/merge-where [:= :id group-id])
      sql/format))

(defn group [{tx :tx {group-id :group-id} :route-params}]
  {:body
   (first (jdbc/query tx (group-query group-id)))})

;;; delete group ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn delete-group [{tx :tx {group-id :group-id} :route-params}]
  (assert group-id)
  (if (= [1] (jdbc/delete! tx :groups ["id = ?" group-id]))
    {:status 204}
    {:status 404 :body "Delete group failed without error."}))

;;; update group ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn prepare-write-data [data tx]
  (catcher/with-logging
    {}
    (-> data
        (select-keys group-write-keys)
        (rename-keys group-write-keymap))))

(defn patch-group
  ([{tx :tx data :body {group-id :group-id} :route-params}]
   (patch-group group-id (prepare-write-data data tx) tx))
  ([group-id data tx]
   (when (->> ["SELECT true AS exists FROM groups WHERE id = ?" group-id]
              (jdbc/query tx )
              first :exists)
     (jdbc/update! tx :groups data ["id = ?" group-id])
     {:status 204})))


;;; create group ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn create-group
  ([{tx :tx data :body}]
   (create-group (prepare-write-data data tx) tx))
  ([data tx]
   (if-let [group (first (jdbc/insert! tx :groups data))]
     {:body group}
     {:status 422
      :body "No group has been created."})))


;;; roles ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn group-inventory-pools-roles-query [group-id]
  (-> (sql/select :iprs.* [:inventory_pools.name :inventory_pool_name])
      (sql/from [:group_access_rights :iprs])
      (sql/merge-where [:= :group_id group-id])
      (sql/merge-join :inventory_pools [:= :iprs.inventory_pool_id :inventory_pools.id])
      sql/format))

(defn inventory-pools-roles [group-id tx]
  (->> group-id
       group-inventory-pools-roles-query
       (jdbc/query tx)))

(defn group-inventory-pools-roles
  [{tx :tx data :body {group-id :group-id} :route-params}]
  {:body
   {:inventory_pools_roles
    (inventory-pools-roles group-id tx)
    }})


;;; routes and paths ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def group-path (path :group {:group-id ":group-id"}))

(def group-transfer-path
  (path :group-transfer-data {:group-id ":group-id"
                             :target-group-id ":target-group-id"}))

(def routes
  (cpj/routes
    (cpj/GET group-path [] #'group)
    (cpj/GET (path :group-inventory-pools-roles {:group-id ":group-id"}) [] #'group-inventory-pools-roles)
    (cpj/PATCH group-path [] #'patch-group)
    (cpj/DELETE group-path [] #'delete-group)
    (cpj/POST (path :groups) [] #'create-group)))


;#### debug ###################################################################
;(logging-config/set-logger! :level :debug)
;(debug/wrap-with-log-debug #'data-url-img->buffered-image)
;(debug/wrap-with-log-debug #'buffered-image->data-url-img)
;(debug/wrap-with-log-debug #'resized-img)
;(logging-config/set-logger! :level :info)
;(debug/debug-ns *ns*)
