(ns leihs.admin.front.init
  (:require [leihs.admin.front.main]))

;;ignore println statements in prod
(set! *print-fn* (fn [& _]))

(leihs.admin.front.main/init!)
