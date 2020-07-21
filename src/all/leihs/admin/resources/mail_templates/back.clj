(ns leihs.admin.resources.mail-templates.back
  (:refer-clojure :exclude [str keyword])
  (:require [leihs.core.core :refer [keyword str presence]]
            [leihs.core.sql :as sql]
            [clojure.java.jdbc :as jdbc]))

(defn meta-templates [tx]
  (-> (sql/select :*)
      (sql/from :mail_templates)
      (sql/where [:= :is_template_template true])
      sql/format
      (->> (jdbc/query tx))))

(defn create-for-inventory-pool [tx pool-id]
  (doseq [tmpl (meta-templates tx)]
    (jdbc/insert! tx
                  :mail_templates
                  (-> tmpl
                      (dissoc :id)
                      (assoc :is_template_template false)
                      (assoc :inventory_pool_id pool-id)))))
