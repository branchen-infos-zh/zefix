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
  "The noga categories. This must be in compiled form."
  nil)

;; (def ^:dynamic *geocode-url-format*
;;   "The open street map geocode url."
;;   "http://nominatim.openstreetmap.org/search?q=%s&format=json&addressdetails=1&countrycodes=ch")

(def ^:dynamic *geocode-url-format*
  "The (unlimited) geocode api."
  "http://open.mapquestapi.com/nominatim/v1/search.php?format=json&q=%s&addressdetails=0&limit=1&countrycodes=ch")

(def ^:dynamic *enrich-address?*
  "If set to true will geo-code addresses."
  nil)

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
  (let [data (first (html/select dom selector))]
    (if (string? data)
      (.trim data))))

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
  [comp-id address-text]
  (try
    (let [url (format *geocode-url-format* (URLEncoder/encode address-text))
          resp (first (json-> (:body (client/get url))))]
      (when resp
        {:lat (:lat resp)
         :lng (:lon resp)}))
    (catch Exception e
      (log/errorf e "Failed geo-coding address: %s" comp-id))))

(defn- extract-address-date
  [comp-id xml address-xml]
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
  [comp-id xml]
  (let [addresses (html/select xml [:instances :addresses :address])]
    (mapv (fn [xmlp]
            (let [text (select xmlp [:addressText html/text-node])]
              {:text text
               :street (select xmlp [:addressDetails :street html/text-node])
               :street-no (select xmlp [:addressDetails :buildingNum html/text-node])
               :zip (select xmlp [:addressDetails :zip html/text-node])
               :city (select xmlp [:addressDetails :city html/text-node])
               :current (current-addr? xmlp)
               :coordinate (when *enrich-address?*
                             (geocode comp-id text))
               :date (extract-address-date comp-id xml xmlp)}))
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
        addresses (extract-addresses comp-id xml)
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

(defn- do-run-json
  [file out-file]
  (log/debugf "Enriching existing json file %s" file)
  (let [data (json/parse-stream (clojure.java.io/reader file) true)
        data-t (->json
                 (mapv (fn [[:as m]]
                         (log/debugf "Processing %s..." (get m :comp-id))
                         (-> m
                           (assoc :noga (n/noga-cat *noga-compiled* (get m :purpose)))
                           (assoc :addresses (mapv (fn [[:as a]]
                                                     (assoc a :coordinate
                                                            (if (and (nil?
                                                                       (get a :coordinate))
                                                                  *enrich-address?*)
                                                              (geocode
                                                                (:comp-id m)
                                                                (:text a))
                                                              (get a :coordinate))))
                                               (:addresses m)))))
                   data))]
    (if out-file
      (write-file data-t out-file)
      (write-stdout data-t))))

(defn- do-run
  [dir out-file]
  (let [data (process-folder dir)]
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
             ["-j" "Use json file as source. -i and -j options are exclusive.
Does only enrich noga codes for now."]
             ["-g" "Enrich geo data for addresses" :flag true :default false]
             ["-o" "File where to store the results. Defaults to stdout."]
             ["-n" "File pointing to noga codes specifications (in json)."]
             ["-h" "--help" "Shows this help" :flag true])]
    (binding [*enrich-address?* (:g options)
              *noga-compiled* (or
                                (and (:n options) (n/load-noga-from-file (:n options)))
                                (n/load-noga-from-resource "noga.json"))]
      (cond
        (:help options) (println banner)
        (:j options) (do-run-json
                       (:j options)
                       (:o options))
        :else (do-run
                (:i options)
                (:o options)))
      (log/debug "Shutting down agents...")
      (shutdown-agents)
      (log/debug "Stopping zefxmx..."))))
