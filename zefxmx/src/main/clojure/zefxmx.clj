(ns zefxmx
  (:require
   [net.cgrand.enlive-html :as html]
   [clojure.tools.logging :as log]
   [clojure.java.io :as io]
   [clojure.pprint :as pp]
   [clojure.tools.cli :refer [cli]]
   [clojure.string :as str]
   [clojure.math.numeric-tower :as math]
   [clojure.contrib.string :as ccstring]))

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

(defn- extract-addresses
  [xml]
  (let [addresses (html/select xml [:instances :addresses :address])]
    (mapv (fn [xmlp]
           {:text (html/select xmlp [:addressText html/text-node])
            :street (html/select xmlp [:addressDetails :street html/text-node])
            :street-no (html/select xmlp [:addressDetails :buildingNum html/text-node])
            :zip (html/select xmlp [:addressDetails :zip html/text-node])
            :city (html/select xmlp [:addressDetails :city html/text-node])})
         addresses)))

(defn extract-details
  "Extracts the relevant details from a zefix xml."
  [ch-id xml]
  (let [canton "ZH"
        name (inst-text-node xml :rubrics :names (child 1) :native)
        legal-form (inst-text-node xml :heading :legalForm)
        ;;[:instances :instance :heading :legalFormText html/text-node]})
        inscr-date (inst-text-node :heading :inscriptionDate)
        purpose (inst-text-node :rubrics :purposes (child 1))
        addresses (extract-addresses xml)]
    {:ch-id ch-id
     :name name
     :legal-form legal-form
     :inscriptionDate inscr-date
     :purpose purpose
     :addresses addresses}))

(defn process-folder
  [folder]
  (let [files (filter #(.endsWith (.getName %) "xml")
                        (file-seq (io/file folder)))]
    files))

(defn -main
  "Main application entry point."
  [& args]
  (log/info "Starting opendata...")
  (let [[options args banner]
        (cli args
             "Zefix xml to json converter."
             ["-i" "Directory that contains zefix company xmls to be processed."]
             ["-o" "File where to store the results. Defaults to stdout."]
             ["-h" "--help" "Shows this help" :flag true])]
    (cond
     (or (:help options) (println banner)
     :else (process-folder (:i options)
                           (:o options)))))