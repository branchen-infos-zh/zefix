(defproject zefidx "0.0.1-SNAPSHOT"
  :description "Web crawler to extrac company numbers from zefix."
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/clojure-contrib "1.2.0"]

                 [enlive "1.1.4"]
                 [org.clojure/tools.cli "0.2.4"]
                 [org.clojure/math.numeric-tower "0.0.2"]

                 ;; Logging
                 [org.clojure/tools.logging "0.2.6"]
                 [ch.qos.logback/logback-classic "1.0.9"]]

  :main zefidx
  :uberjar {:aot [zefidx]}

  :source-paths ["src" "src/main/clojure"]
  :java-source-paths ["src/main/java"]
  :test-paths ["test" "src/test/clojure"]
  :resource-paths ["src/main/resources"]
  :compile-path "target/classes"
  ;:native-path "src/native"
  :target-path "target/"
  :jar-name "zefidx.jar"
  :uberjar-name "zefidx-standalone.jar")
