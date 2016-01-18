(ns clojider-remote.core
 (:require [clojure.edn :as edn]
           [clojure.java.io :as io]
           [cheshire.core :refer [generate-string parse-stream]]
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

;(parse-result (lambda/invoke creds :function-name "clojider-development-lambda"
;                             :payload (generate-payload {:trolo "polo"})))
