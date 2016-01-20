(ns saranyu.v2
  (:require
   [saranyu
    [util :as util]
    [crypto :as crypto]]
   [clojure.string :refer [join lower-case upper-case]])
  (:import
   [java.net URL]))

(defn- auth-params
  [request]
  {:SignatureVersion "2"
   :AWSAccessKeyId (:key request)
   :Timestamp (util/current-time)
   :SignatureMethod crypto/hmac-sha256-algorithm})

(defn build-query-string
  [params]
  (join "&"
        (map (fn [[k v]] (str (util/url-encode (name k)) "="
                              (util/url-encode (str v))))
             params)))

(defn- string-to-sign
  [method host path query-params]
  (str method
       util/new-line
       host
       util/new-line
       path
       util/new-line
       (build-query-string query-params)))

(defn- get-path
  [^URL url]
  (let [path (.getPath url)]
    (if (empty? path)
      "/"
      path)))

(defn- v2-signature
  [{:keys [url method secret]} query-params]
  (let [query-params (into (sorted-map) query-params)
        url (URL. url)
        host (lower-case (.getHost url))
        path (get-path url)
        str-to-sign (string-to-sign (upper-case (name method)) host path query-params)]
    (crypto/base64 (crypto/calculate-hmac str-to-sign (crypto/get-mac-sha256 (util/to-bytes secret))))))

(defn- auth-url
  "Builds a v2 signed url, which can be used with the aws api"
  [{:keys [url method params body] :as request}]
  (let [query-params (merge (util/keys-as-keyword params) (auth-params request))
        signature (v2-signature request query-params)
        param-string (build-query-string query-params)
        sign-params (build-query-string {:Signature signature})]
    {:url (str url "?" param-string "&" sign-params)}))

(defn sign
  "http://docs.aws.amazon.com/general/latest/gr/signature-version-2.html"
  [{:keys [method ] :as request}]
  (let [request (merge {:method "GET"} request)
        v2-res (auth-url request)]
    (assoc v2-res :method (:method request))))
