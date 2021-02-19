(ns leihs.admin.resources.users.user.core
  (:refer-clojure :exclude [str keyword])
  (:require
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.sql :as sql]
    [leihs.admin.utils.regex :refer [uuid-pattern org-id-org-pattern]]
    [clojure.string :as string]
    ))

(defn sql-merge-unique-user [query uid]
  (cond

    ; case UUID must be the primary ID
    (re-matches uuid-pattern uid)
    (sql/merge-where query [:= :users.id uid])

    ; case ORG_ID and ORG
    (re-matches org-id-org-pattern uid)
    (let [[_ org org_id] (re-matches org-id-org-pattern uid)]
      (sql/merge-where query [:and
                              [:= :users.organization org]
                              [:= :users.org_id org_id]]))

    ; case emails, it suffices to test for @ since the org is out
    ; and login may not contain it
    (clojure.string/includes? uid "@" )
    (sql/merge-where query
                     [:= (sql/call :lower :users.email)
                      (sql/call :lower uid)])


    ; case login, could be anything but the above, for now it is restricted
    (and (not (clojure.string/includes? uid "@"))
         (not (clojure.string/includes? uid "|")))
    (sql/merge-where query
                     [:= :users.login uid])


    ; sure nothing matches else
    :else (sql/merge-where query [:= true false])

    ))

