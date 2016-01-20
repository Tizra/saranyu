(ns saranyu.core
  (:require
   [saranyu
    [v2 :as v2]
    [v4 :as v4]
    [s3 :as s3]
    [crypto :as crypto]]
   [clojure.string :refer [split]])
  (:import
   [java.net URL]))

(defn- get-host [url]
  (.getHost (URL. url)))

;s3 url can be s3-region.amazonaws.com or bucket.s3-region.amazon-aws.com
(defn- s3?
  [request]
  (let [host (get-host (:url request))
        parts (split host #"[.]")]
    (or (.startsWith host "s3")
        (.startsWith (second parts) "s3"))))

(defn- v2?
  [request]
  (let [host (get-host (:url request))]
    (or (.startsWith host "ec2")
        (.startsWith host "elasticache")
        (.startsWith host "storagegateway")
        (.startsWith host "sdb"))))

(defn sign
  "Generates the signature for the url using the given options and returns a map of request details,
   the caller can use to interact with the AWS API."
  [request]
  (cond
   (v2? request) (v2/sign request)
   (s3? request) (s3/sign request)
   :else (v4/sign request)))
