(ns zefidx
  (:gen-class)
  (:require
   [net.cgrand.enlive-html :as html]
   [clojure.tools.logging :as log]
   [clojure.java.io :as io]
   [clojure.pprint :as pp]
   [clojure.string :as str]
   [clojure.tools.cli :refer [cli]]
   [clojure.math.numeric-tower :as math]
   [clojure.contrib.string :as ccstring]))

(def ^:dynamic *zefix-rechtsformen*
  [{:id "" :name "Alle"}
   {:id "1" :name "Einzelunternehmung"}
   {:id "2" :name "Kollektivgesellschaft"}
   {:id "10" :name "Kommanditgesellschaft"}
   {:id "3" :name "Aktiengesellschaft (AG)"}
   {:id "4" :name "Gesellschaft mit beschränkter Haftung"}
   {:id "5" :name "Genossenschaft"}
   {:id "6" :name "Verein"}
   {:id "7" :name "Stiftung"}
   {:id "8" :name "besondere Rechtsformen"}
   {:id "9" :name "Zweigniederlassung (ZN)"}
   {:id "11" :name "Zweigniederlassung (ausländische)"}
   {:id "12" :name "Kommanditaktiengesellschaft"}
   {:id "13" :name "Institute des öffentlichen Recht"}
   {:id "14" :name "Gemeinderschaft"}
   {:id "15" :name "SICAV"}
   {:id "16" :name "SICAF"}
   {:id "17" :name "Kommanditgesell. für kollektive Kapitalanlagen"}
   {:id "18" :name "Nichtkaufmännische Prokura"}])

;; This is the url to fetch the complete register for switzerland. It is also slightly differen in the format of the data
;;(def ^:dynamic *zefix-query-url* "http://www.zefix.admin.ch/WebServices/Zefix/Zefix.asmx/SearchFirm?name=_%s_%%20&suche_nach=-&rf=%s&sitz=&sitzgem=&id=&language=1&phonetisch=no&posMin=%s")

(def ^:dynamic *zefix-query-url*
  "http://search.powernet.ch/webservices/net/Zefix/Zefix.asmx/SearchFirm?amt=20&name=_%s_%%20&suche_nach=bisherig&rf=%s&sitz=&id=&language=1&phonetisch=no&suche_nache=&suffix=&posMin=%s")

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
  (dorun (map println data)))

(defn- fetch-url
  "Retrieves handelregister data from zefix."
  [url]
  (html/html-resource (java.net.URL. url)))

(defn- generate-zefix-query-url
  "Generates the url to query biz ids."
  ([word]
     (generate-zefix-query-url word ""))
  ([word rf]
     (generate-zefix-query-url word rf 1))
  ([word rf pos]
     (format *zefix-query-url* word rf pos)))

(defn- gen-range
  "Generate min-numbers for zefix ch-id requests."
  [num]
  (map #(-> %
            (* 1000)
            (+ 1))
       (range 0 (math/ceil (/ num 1000)))))

;; And now the true logic
(defn list-rechtsformen
  "Lists the Rechtsformen available for querying the Handelsregister."
  []
  (pp/print-table *zefix-rechtsformen*))

(defn- extract-zh-comp-ids
  "Extract the ids from a ZH zefix request."
  [word rf num]
  ;; Check num. Zefix returns 1000 entries per page
  (let [xreq (math/ceil (/ num 1000))]
    (into #{} (flatten (map
                        #(let [url (generate-zefix-query-url word rf %)
                               dom (fetch-url url)]
                           (log/info "Parsing response for" url)
                           (html/select dom #{[:div.list
                                               :ul :li
                                               :> (html/nth-child 2) :> html/text-node]}))
                        (gen-range num))))))

(defn query-comp-ids
  "Query swiss company numbers from zefix."
  [word num rf out-file]
  (log/info "Querying data [word: " word ", rf: " rf ", num: " num "]")
  (let [comp-ids (extract-zh-comp-ids word rf num)]
    (if out-file
      (write-file comp-ids out-file)
      (write-stdout comp-ids))))

(defn -main
  "Main application entry point."
  [& args]
  (log/debug "Starting zefidx...")
  (let [[options args banner]
        (cli args
             "Query zefix for company numbers."
             ["-l" "--list-rf" "List Rechtsformen ids" :flag true]
             ["-p" "The pattern to used for querying. It will be wrapped with '_x_" :flag false]
             ["-n" "Number of records to fetch" :default 1 :parse-fn #(Integer. %)]
             ["-r" "Set the Rechtsform to be queried" :default ""]
             ["-o" "File where to write the data to. By default to stdout."]
             ["-h" "--help" "Show this help" :flag true])]
    (cond
     (:help options) (println banner)
     ;; List Rechtsformen
     (:list-rf options) (list-rechtsformen)
     ;; Check whether we've got all information to
     ;; query comp-ids and then do so.
     (empty? (:p options)) (println banner)
     :else (query-comp-ids (:p options)
                         (:n options)
                         (:r options)
                         (:o options))))
  (log/debug "Shutting down..."))
