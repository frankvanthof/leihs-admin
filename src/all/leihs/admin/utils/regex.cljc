(ns leihs.admin.utils.regex)

(def uuid-pattern #"^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")

(def org-id-org-pattern #"^(.+)\|([A-Za-z0-9]+[A-Za-z0-9.-]+[A-Za-z0-9]+)$")

;(re-matches uuid-pattern "85cab111-4790-5b83-9c75-343e82037d41")

