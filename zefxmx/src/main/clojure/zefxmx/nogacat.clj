(ns zefxmx.nogacat
  (:require
   [clojure.tools.logging :as log]
   [clojure.java.io :as io]
   [clojure.pprint :as pp]
   [clojure.tools.cli :refer [cli]]
   [clojure.string :as str]
   [clojure.math.numeric-tower :as math]
   [clojure.contrib.string :as ccstring]))

(def ^:dynamic *noga-cat-2008*
  "Noga 2008 categories and textual keywords thereof. This is used
to extract noga 2008 codes from natural text. The more keywords are provided
the better will the categorization work. If multiple noga code specifications
use the same keywords the algorithm will choose the category that matches
first."
  [{:code "code1"
    :keywords ["marketing*" "sales" "verkauf"]}
   {:code "code2"
    :keywords ["abc" "def"]}
   {:code "other"
    :keywords [".*"]}])

(defn compile-noga-cats
  "Compiles a list of noga specifications, that is its :keywords, into regular
expression patterns."
  [cats]
  (mapv #(assoc % :keywords-compiled
                (mapv re-pattern (:keywords %)))
        cats))

(defn- remove-nil
  [s]
  (remove nil? s))

;; TODO: This really needs some simplification. Can we use reduce here?
(defn noga-cat
  "Finds the first matching noga category for a given text."
  [text noga-cats-compiled]
  (:code
   (first
    (remove-nil
     (map (fn [m]
            (when (not (empty?
                        (remove-nil
                         (map #(re-find % text)
                              (:keywords-compiled m)))))
              m))
          noga-cats-compiled)))))
