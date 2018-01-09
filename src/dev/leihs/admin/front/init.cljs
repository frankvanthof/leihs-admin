(ns ^:figwheel-no-load leihs.admin.front.init
  (:require
    [leihs.admin.front.main]
    [leihs.admin.front.html]
    [figwheel.client :as figwheel :include-macros true]))

(enable-console-print!)

(figwheel/watch-and-reload
  :websocket-url "ws://localhost:3212/figwheel-ws"
  :jsload-callback leihs.admin.front.html/mount)

(leihs.admin.front.main/init!)
