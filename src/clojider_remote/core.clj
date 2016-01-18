(ns clojider-remote.core
 (:require [clojure.edn :as edn]
           [amazonica.aws.lambda :as lambda]))

(def creds (edn/read-string (slurp "config.edn")))

;(lambda/invoke creds :function-name "clojider-development-lambda"
;                     :payload "{\"key1\": 1, \"key2\": 2, \"key4\": 3}")
