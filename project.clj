(defproject leihs-admin "0.0.0-beta"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies
  [
   [aleph "0.4.4"]
   [bidi "2.1.3"]
   [camel-snake-kebab "0.4.0"]
   [cheshire "5.8.0"]
   [cider-ci/open-session "2.0.0-beta.1"]
   [clj-http "3.7.0"]
   [cljs-http "0.1.44"]
   [cljsjs/jimp "0.2.27"]
   [cljsjs/js-yaml "3.3.1-0"]
   [cljsjs/moment "2.17.1-1"]
   [clojure-humanize "0.2.2"]
   [com.lucasbradstreet/cljs-uuid-utils "1.0.2"]
   [com.mchange/c3p0 "0.9.5.2"]
   [compojure "1.6.0"]
   [environ "1.1.0"]
   [hiccup "1.0.5"]
   [hiccup "1.0.5"]
   [hickory "0.7.1"]
   [honeysql "0.9.1"]
   [inflections "0.13.0"]
   [io.forward/yaml "1.0.6"]
   [log4j/log4j "1.2.17" :exclusions [javax.mail/mail javax.jms/jms com.sun.jdmk/jmxtools com.sun.jmx/jmxri]]
   [logbug "4.2.2"]
   [org.clojure/clojure "1.9.0"]
   [org.clojure/clojurescript "1.9.946" :scope "provided"]
   [org.clojure/java.jdbc "0.7.4"]
   [org.clojure/tools.cli "0.3.5"]
   [org.clojure/tools.logging "0.4.0"]
   [org.clojure/tools.nrepl "0.2.13"]
   [org.slf4j/slf4j-log4j12 "1.7.25"]
   [org.slf4j/slf4j-log4j12 "1.7.25"]
   [pg-types "2.3.0"]
   [reagent "0.7.0"]
   [ring "1.6.3"]
   [ring-middleware-accept "2.0.3"]
   [ring/ring-json "0.4.0"]
   [timothypratley/patchin "0.3.5"]
   [uritemplate-clj "1.1.1"]
   [venantius/accountant "0.2.3"]

   ; force transitive dependency resolution
   [ring/ring-core "1.6.3"]
   ]

  :jvm-opts ["--add-modules" "java.xml.bind"]
  :javac-options ["-target" "1.8" "-source" "1.8" "-Xlint:-options"]

  :source-paths ["src/all"]
  :resource-paths ["resources/all"]
  :test-paths ["src/test"]

  :aot [#"leihs.admin.*"]
  :main leihs.admin.back.main

  :plugins [[lein-environ "1.1.0"]
            [lein-cljsbuild "1.1.7"]
            [lein-asset-minifier "0.4.3"
             :exclusions [org.clojure/clojure]]]

  :cljsbuild {:builds
              {:min {:source-paths ["src/all" "src/prod"]
                     :jar true
                     :compiler
                     {:output-to "target/cljsbuild/public/admin/js/app.js"
                      :output-dir "target/uberjar"
                      :optimizations :simple
                      :pretty-print  false}}
               :app
               {:source-paths ["src/all" "src/dev"]
                :compiler
                {:main "leihs.admin.front.init"
                 :asset-path "/js/out"
                 :output-to "target/cljsbuild/public/admin/js/app.js"
                 :output-dir "target/cljsbuild/public/js/out"
                 :source-map true
                 :optimizations :none
                 :pretty-print  true}}}}

  :sass {:src "resources/all/public/admin/css/"
         :dst "resources/all/public/admin/css/"}

  :figwheel {:http-server-root "public"
             :server-port 3212
             :nrepl-port 3213
             :nrepl-middleware ["cemerick.piggieback/wrap-cljs-repl"]
             :css-dirs ["resources/all/public/admin/css"]}

  :profiles {:dev {:dependencies [[com.cemerick/piggieback "0.2.2"]
                                  [figwheel-sidecar "0.5.14"]
                                  [org.clojure/tools.nrepl "0.2.13"]
                                  [pjstadig/humane-test-output "0.8.3"]
                                  [prone "1.1.4"]
                                  [ring/ring-devel "1.6.3"]
                                  [ring/ring-mock "0.3.2"]]
                   :plugins [[lein-figwheel "0.5.14"]
                             [lein-sassy "1.0.8"]]
                   :source-paths ["src/all" "src/dev"]
                   :resource-paths ["resources/all" "resources/dev" "target/cljsbuild"]
                   :injections [(require 'pjstadig.humane-test-output)
                                (pjstadig.humane-test-output/activate!)]
                   :env {:dev true}}
             :uberjar {:hooks [minify-assets.plugin/hooks]
                       :source-paths ["src/all" "src/prod"]
                       :prep-tasks ["compile" ["cljsbuild" "once" "min"]]
                       :resource-paths ["resources/all" "resources/prod" "target/cljsbuild"]
                       :aot [#"leihs\..*"]
                       :uberjar-name "leihs-admin.jar"
                       }
             :test {:resource-paths ["resources/all" "resources/test" "target/cljsbuild"]}}


)
