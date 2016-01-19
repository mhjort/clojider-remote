(ns clojider-remote.core
 (:require [clojure.edn :as edn]
           [clojure.java.io :as io]
           [clj-gatling.chart :as chart]
           [cheshire.core :refer [generate-string parse-stream]]
           [amazonica.aws.s3 :as s3]
           [amazonica.aws.lambda :as lambda]))

(def creds (edn/read-string (slurp "config.edn")))

(defn parse-result [result]
  (-> result
      :payload
      (.array)
      (java.io.ByteArrayInputStream.)
      (io/reader)
      (parse-stream)))

(defn generate-payload [o]
  (java.nio.ByteBuffer/wrap (.getBytes (generate-string o) "UTF-8")))

(defn create-dir [dir]
    (.mkdirs (java.io.File. dir)))

(def scenario
  {:name "Ping scenario"
   :requests [{:name "Ping" :http "http://clj-gatling-demo-server.herokuapp.com/ping"}]})

(defn download-file [results-dir bucket object-key]
  (io/copy (:object-content (s3/get-object creds bucket object-key))
           (io/file (str results-dir "/" object-key))))

;(do
;  (create-dir "tmp/input")
;  (download-file "tmp/input" "clojider-results" "simulation0.log")
;  (chart/create-chart "tmp"))

;(parse-result (lambda/invoke creds :function-name "clojider-development-lambda"
;                             :payload (generate-payload {:scenarios [scenario]
;                                                         :users 10
;                                                         :options {:requests 100}})))

