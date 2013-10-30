(ns zefxmx
  (:gen-class)
  (:require
   [zefxmx.nogacat :as n]
   [net.cgrand.enlive-html :as html]
   [clj-http.client :as client]
   [cheshire.core :as json]
   [clojure.tools.logging :as log]
   [clojure.java.io :as io]
   [clojure.pprint :as pp]
   [clojure.tools.cli :refer [cli]]
   [clojure.string :as str]
   [clojure.math.numeric-tower :as math]
   [clojure.contrib.string :as ccstring])

  (:import
   [java.net URLEncoder]))

(def ^:dynamic *noga-compiled*
  "The noga categories. Defaults to noga.json loaded from the classpath."
  (n/load-noga-from-resource "noga.json"))

;; (def ^:dynamic *geocode-url-format*
;;   "The open street map geocode url."
;;   "http://nominatim.openstreetmap.org/search?q=%s&format=json&addressdetails=1&countrycodes=ch")

(def ^:dynamic *geocode-url-format*
  "The (unlimited) geocode api."
  "http://open.mapquestapi.com/nominatim/v1/search.php?format=json&q=%s&addressdetails=0&limit=1&countrycodes=ch")

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
       (map #(do (.write wrtr (str %)))
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
  ;; We really need to use streams here... we might process a whole lot of data here...
  (json/generate-string data));{:pretty true}))

(defn- json->
  [json-str]
  (json/parse-string json-str true))

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

(defn- geocode
  [address-text]
  (try
    (let [url (format *geocode-url-format* (URLEncoder/encode address-text))
          resp (first (json-> (:body (client/get url))))]
      (when resp
        {:lat (:lat resp)
         :lng (:lon resp)}))
    (catch Exception e
      (log/error "Failed to geo code address." e))))

(defn- extract-address-date
  [xml address-xml]
  (let [ins (:ins (:attrs address-xml))
        del (:del (:attrs address-xml))]
    {:from (select xml [:instances
                        :instance
                        :citations
                        (html/attr= :ref ins)
                        :diary
                        :date
                        html/text])
     :to (select xml [:instances
                      :instance
                      :citations
                      (html/attr= :ref del)
                      :diary
                      :date
                      html/text])}))

(defn- extract-addresses
  [xml]
  (let [addresses (html/select xml [:instances :addresses :address])]
    (mapv (fn [xmlp]
            (let [text (select xmlp [:addressText html/text-node])]
              {:text text
               :street (select xmlp [:addressDetails :street html/text-node])
               :street-no (select xmlp [:addressDetails :buildingNum html/text-node])
               :zip (select xmlp [:addressDetails :zip html/text-node])
               :city (select xmlp [:addressDetails :city html/text-node])
               :current (current-addr? xmlp)
               :coordinate (geocode text)
               :date (extract-address-date xml xmlp)}))
         addresses)))

(defn extract-xml-details
  "Extracts the relevant details from a zefix xml."
  [xml]
  (let [canton "ZH"
        comp-id (inst-text-node xml :heading :identification :CHNum)
        name (inst-text-node xml :rubrics :names (child 1) :native)
        legal-form (inst-text-node xml :heading :legalForm)
        deletion-date (inst-text-node xml :heading :deletionDate)
        inscr-date (inst-text-node xml :heading :inscriptionDate)
        purpose (inst-text-node xml :rubrics :purposes (child 1))
        addresses (extract-addresses xml)
        noga (n/noga-cat *noga-compiled* purpose)]
    (log/debug "Extracting details for" comp-id)
    {:comp-id comp-id
     :name name
     :legal-form legal-form
     :deletion-date deletion-date
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
  [folder]
  (let [files (filter #(and (.isFile %)
                            (.endsWith (.getName %) "xml"))
                      (file-seq (io/file folder)))
        ;; We could use pmap here... the problem is that the
        ;; geocoding host requires us to use a single thread
        ;; as the ip might be blacklisted otherwise
        data (->json (vec (map #(process-file %)
                                files)))]
    data))

(defn- do-run
  [dir out-file noga-file]
  (let [data (if-let [noga-loaded (n/load-noga-from-file noga-file)]
               (do (log/debug "Using user-provided noga file:" noga-file)
                   (binding [*noga-compiled* noga-loaded]
                     (process-folder dir)))
               (process-folder dir))]
    (if out-file
      (write-file data out-file)
      (write-stdout data))))

(defn -main
  "Main application entry point."
  [& args]
  (log/debug "Starting zefxmx...")
  (let [[options args banner]
        (cli args
             "Zefix xml to json converter."
             ["-i" "Directory that contains zefix company xmls to be processed."]
             ["-o" "File where to store the results. Defaults to stdout."]
             ["-n" "File pointing to noga codes specifications (in json)."]
             ["-h" "--help" "Shows this help" :flag true])]
    (cond
     (:help options) (println banner)
     :else (do-run (:i options)
                   (:o options)
                   (:n options))))
  (log/debug "Shutting down agents...")
  (shutdown-agents)
  (log/debug "Stopping zefxmx..."))
