(ns zefxmx
  (:gen-class)
  (:require
   [zefxmx.nogacat :as n]
   [net.cgrand.enlive-html :as html]
   [cheshire.core :as ch]
   [clojure.tools.logging :as log]
   [clojure.java.io :as io]
   [clojure.pprint :as pp]
   [clojure.tools.cli :refer [cli]]
   [clojure.string :as str]
   [clojure.math.numeric-tower :as math]
   [clojure.contrib.string :as ccstring]))

(def noga-cats-compiled
  (n/compile-noga-cats n/*noga-cat-2008*))

;; Utility functions
(defn- write-file
  "Writes all ids to a file."
  [data file]
  (let [f (io/file file)]
    (log/debug "Writing ids to" (.getAbsolutePath f))
    ;; Make sure parent dir exists
    (-> f
        (.getAbsoluteFile)
        (.getParentFile)
        (.mkdirs))
    (with-open [wrtr (io/writer f)]
      (dorun
       (map #(do (.write wrtr (str % "\n")))
            data)))))

(defn- write-stdout
  "Prints data to stdout"
  [data]
  (dorun (map #(println %) data)))

(defn- read-file
  [file]
  (html/html-resource file))

(defn- ->json
  [data]
  (ch/generate-string data))

;; The following function are a shortcut for enlive functions
(defn- select
  [dom selector]
  (first (html/select dom selector)))

(defn- child
  [i]
  (html/nth-child i))

(defn- inst
  ([i & rest]
     (flatten (merge [:instances (child i)] rest html/text-node))))

(defn- inst-text-node
  [xml & rest]
  (select xml (inst 1 rest)))

(defn- current-addr?
  "Checks whether the address node is the currently active address."
  [addr-node]
  (= "1" (get-in addr-node [:attrs :status])))

(defn- extract-addresses
  [xml]
  (let [addresses (html/select xml [:instances :addresses :address])]
    (mapv (fn [xmlp]
           {:text (select xmlp [:addressText html/text-node])
            :street (select xmlp [:addressDetails :street html/text-node])
            :street-no (select xmlp [:addressDetails :buildingNum html/text-node])
            :zip (select xmlp [:addressDetails :zip html/text-node])
            :city (select xmlp [:addressDetails :city html/text-node])
            :current (current-addr? xmlp)
            })
         addresses)))

(defn extract-xml-details
  "Extracts the relevant details from a zefix xml."
  [xml]
  (let [canton "ZH"
        comp-id (inst-text-node xml :heading :identification :CHNum)
        name (inst-text-node xml :rubrics :names (child 1) :native)
        legal-form (inst-text-node xml :heading :legalForm)
        inscr-date (inst-text-node xml :heading :inscriptionDate)
        purpose (inst-text-node xml :rubrics :purposes (child 1))
        addresses (extract-addresses xml)
        noga (n/noga-cat purpose noga-cats-compiled)]
    {:comp-id comp-id
     :name name
     :legal-form legal-form
     :inscription-date inscr-date
     :purpose purpose
     :addresses addresses
     :noga noga}))

(defn process-file
  [file]
  (let [xml (read-file file)
        out (extract-xml-details xml)]
    out))

(defn process-folder
  [folder out-file]
  (let [files (filter #(.endsWith (.getName %) "xml")
                        (file-seq (io/file folder)))
        data (->json (mapv #(process-file %)
                             files))]
    (if out-file
      (write-file data out-file)
      (write-stdout data))
    data))

(defn -main
  "Main application entry point."
  [& args]
  (log/debug "Starting zefxmx...")
  (let [[options args banner]
        (cli args
             "Zefix xml to json converter."
             ["-i" "Directory that contains zefix company xmls to be processed."]
             ["-o" "File where to store the results. Defaults to stdout."]
             ["-h" "--help" "Shows this help" :flag true])]
    (cond
     (:help options) (println banner)
     :else (process-folder (:i options)
                           (:o options))))
  (log/debug "Stopping zefxmx..."))
