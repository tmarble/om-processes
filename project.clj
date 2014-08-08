(defproject om-processes "0.1.0-SNAPSHOT"
  :description "David Nolen's 10,000 processes in Om"
  :url "https://github.com/tmarble/om-processes"
  :license {:name "Eclipse Public License - v 1.0"
            :url "http://www.eclipse.org/legal/epl-v10.html"
            :distribution :repo
            :comments "same as Clojure"}

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-2280"]
                 [org.clojure/core.async "0.1.303.0-886421-alpha"]
                 [om "0.7.1"]]

  :plugins [[lein-cljsbuild "1.0.4-SNAPSHOT"]]

  :jvm-opts ["-server" "-Xmx256m"] ;; optional

  :cljsbuild {
    :builds [{:id "om-processes"
              :source-paths ["src/cljs"]
              :compiler {
                         :output-dir "resources/public/js/"
                         :output-to "resources/public/js/om-processes.js"
                         :optimizations :none
                         :source-map true}}
             {:id "prod"
              :source-paths ["src/cljs"]
              :compiler {
                         :output-dir "production/resources/public/js/"
                         :output-to "production/resources/public/js/om-processes.js"
                         :optimizations :advanced
                         :pretty-print false}}
             ]}
  )
