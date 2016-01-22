(ns clojider-remote.core
 (:require [clojure.edn :as edn]
           [clojure.java.io :as io]
           [clojure.string :refer [split]]
           [clj-time.core :as t]
           [clj-time.format :as f]
           [clj-gatling.chart :as chart]
           [cheshire.core :refer [generate-string parse-stream]]
           [clj-gatling.simulation-util :refer [split-to-number-of-buckets]]
           [clojure.core.async :refer [thread <!!]]
           [amazonica.aws.s3 :as s3]
           [amazonica.aws.lambda :as lambda]))

(def creds (edn/read-string (slurp "config.edn")))

(defn parse-result [result]
  (-> result
      :payload
      (.array)
      (java.io.ByteArrayInputStream.)
      (io/reader)
      (parse-stream true)))

(defn generate-payload [o]
  (java.nio.ByteBuffer/wrap (.getBytes (generate-string o) "UTF-8")))

(defn create-dir [dir]
    (.mkdirs (java.io.File. dir)))

(def scenario
  {:name "Ping scenario"
   :requests [{:name "Ping" :http "http://clj-gatling-demo-server.herokuapp.com/ping"}]})

(defn download-file [results-dir bucket object-key]
  (io/copy (:object-content (s3/get-object creds bucket object-key))
           (io/file (str results-dir "/" (last (split object-key #"/"))))))

(defn- generate-folder-name []
  (let [custom-formatter (f/formatter "yyyyMMddHHmmssSSS")]
    (f/unparse custom-formatter (t/now))))

(defn create-chart [results bucket-name folder-name]
  (let [input-dir (str "tmp/" folder-name "/input")]
    (create-dir input-dir)
    (println "Downloading" results "from" bucket-name)
    (doseq [result results]
      (download-file input-dir bucket-name result))
    (chart/create-chart (str "tmp/" folder-name))))

(defn invoke-lambda [node-id scenarios users lambda-function-name folder-name options]
  (println "Invoking Lambda for" node-id)
  (parse-result (lambda/invoke (assoc creds :client-config {:socket-timeout (* 6 60 1000)})
                               :function-name lambda-function-name
                               :payload (generate-string {:scenarios scenarios
                                                          :users users
                                                          :options (-> options
                                                                       (update :duration t/in-millis)
                                                                       (assoc :folder-name folder-name
                                                                               :node-id node-id))}))))

(defn run-simulation [node-count scenario concurrency lambda-function-name options]
  (let [splitted-users (split-to-number-of-buckets (range concurrency) node-count)
        folder-name (generate-folder-name)
        result-channels (mapv #(thread (invoke-lambda %1 scenario %2 lambda-function-name folder-name options))
                              (range)
                              splitted-users)
        all-results (mapcat :results (map <!! result-channels))]
    (println "Got results" all-results)
    (create-chart all-results (:bucket-name options) folder-name)))

;(run-simulation scenario 100 "clojider-development-lambda" "clojider-results" 100000)
