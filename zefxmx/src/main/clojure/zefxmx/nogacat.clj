(ns zefxmx.nogacat
  (:require
   [clojure.tools.logging :as log]
   [clojure.java.io :as io]
   [cheshire.core :as json]
   [clojure.pprint :as pp]
   [clojure.tools.cli :refer [cli]]
   [clojure.string :as str]
   [clojure.math.numeric-tower :as math]
   [clojure.contrib.string :as ccstring]))

(defn compile-noga-cats
  "Compiles a list of noga specifications, that is its :keywords, into regular
expression patterns."
  [cats]
  (mapv #(assoc % :keywords-compiled
                (mapv re-pattern (:keywords %)))
        cats))

(defn load-noga-from-resource
  "Loads & compile noga code from a resource (json)."
  [resource-name]
  (let [r (clojure.java.io/resource resource-name)]
    (when r
      (compile-noga-cats
       (json/parse-string (slurp r) true)))))

(defn load-noga-from-file
  [path]
  (when path
    (let [f (clojure.java.io/as-file path)]
      (when (.exists f)
        (compile-noga-cats
         (json/parse-string (slurp f) true))))))

(defn- remove-nil
  [s]
  (remove nil? s))

(defn- match-keywords
  "Matches spec's :keywords against text. In case a keyword matches, return
the spec, otherwise nil."
  [spec text]
  (let [found (remove-nil (map #(re-find % text)
                               (:keywords-compiled spec)))]
    (when (not (empty? found))
      (:code spec))))

(defn noga-cat
  "Finds the first matching noga category for a given text."
  [compiled-noga text]
  (when (not (empty? text))
    (vec (remove-nil
          (map #(match-keywords % text)
               compiled-noga)))))
