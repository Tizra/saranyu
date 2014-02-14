(ns saranyu.v2
  (:require
   [saranyu
    [util :refer :all]
    [crypto :refer :all]]
   [clojure.tools.logging :as log]
   [clojure.string :refer [join lower-case upper-case]])
  (:import
   [java.net URL]))

(defn- auth-params
  []
  {:SignatureVersion "2"
   :AWSAccessKeyId *key*
   :Timestamp (current-time)
   :SignatureMethod hmac-sha256-algorithm})

(defn build-query-string
  [params]
  (join "&"
        (map (fn [[k v]] (str (url-encode (name k)) "="
                              (url-encode (str v))))
             params)))

(defn- string-to-sign
  [method host path query-params]
  (str method
       new-line
       host
       new-line
       path
       new-line
       (build-query-string query-params)))

(defn- get-path
  [url]
  (let [path (.getPath url)]
    (if (empty? path)
      "/"
      path)))

(defn- v2-signature
  [{:keys [url method]} query-params]
  (let [query-params (into (sorted-map) query-params)
        url (URL. url)
        host (lower-case (.getHost url))
        path (get-path url)
        str-to-sign (string-to-sign (upper-case (name method)) host path query-params)]
    (log/debug "V2: Recieved query string parameters are " query-params)
    (log/debug "V2: Path = " path)
    (log/debug "V2: String to sign is " str-to-sign)
    (base64 (calculate-hmac str-to-sign (get-mac-sha256 (to-bytes *secret*))))))

(defn- auth-url
  "Builds a v2 signed url, which can be used with the aws api"
  [{:keys [url method params] :as request}]
  (let [query-params (merge (keys-as-keyword params) (auth-params))
        signature (v2-signature request query-params)
        query-string (build-query-string
                      (into (sorted-map)
                            (merge {:Signature signature} query-params)))]
    (log/debug "V2: Signature String is " signature)
    (str url "?" query-string)))

(defn sign
  "http://docs.aws.amazon.com/general/latest/gr/signature-version-2.html"
  [request]
  (log/debug "V2: Processing signature for url : " (request :url))
  (let [request (merge request {:method "get"})
        v2-url (auth-url request)]
    {:url v2-url
     :method (request :method)}))
