(ns saranyu.v4
  (:require
   [saranyu
    [util :refer :all]
    [crypto :refer :all]]
   [clojure.string :refer [join lower-case upper-case trim split]]
   [clojure.tools.logging :as log])
  (:import
   [java.net URL]))

(def ^:const ^:private v4-algorithm "AWS4-HMAC-SHA256")
(def ^:const ^:private default-region "us-east-1")

(defn- parse-url
  [uri]
  (let [url (URL. uri)
        host (.getHost url)
        protocol (.getProtocol url)
        path (.getPath url)]
    {:uri  (str protocol "://" host "/")
     :host host
     :path (if (empty? path) "/" path)
     :query (.getQuery url)}))

(defn- canonical-headers
  [headers]
  (str (join new-line (map (fn [[k v]]
                             (str (lower-case (name k)) ":" (trim v)))
                           headers)) new-line))

(defn- signed-headers
  [headers]
  (join ";"
        (map #(lower-case (name %)) (keys headers))))

(defn- canonical-request
  [method path query headers body]
  (let [body (or body "")
        q-string (map-to-query-string (query-string-to-map query) true)
        canon-request (str (upper-case (name method)) new-line
                           path new-line
                           q-string new-line
                           (canonical-headers headers) new-line
                           (signed-headers headers) new-line
                           (hex (sha-256 body)))]
    (log/debug "V4: Query string parameters are " q-string )
    (log/debug "V4: Canonical request is " canon-request)
    (->
     canon-request
     (sha-256)
     (hex))))

(defn- credential-scope
  [host time]
  (let [endpoint (split (subs host 0 (.indexOf  host ".amazonaws.com")) #"[.]")
        region (second endpoint)
        region (if region region default-region)
        cr-scope (str (v4-date time)
                      "/"
                      (lower-case region)
                      "/"
                      (lower-case (first endpoint))
                      "/"
                      "aws4_request")]
    (log/debug "V4: Credentials scope is " cr-scope)
    cr-scope))

(defn- string-to-sign
  [host time v4-request]
  (let [str-to-sign (str v4-algorithm new-line
                         time new-line
                         (credential-scope host time) new-line
                         v4-request)]
   (log/debug "V4: String to sign is " str-to-sign)
    str-to-sign))

(defn- signing-key
  [host time]
  (let [parts (split (credential-scope host time) #"/")]
    (->>
     (calculate-hmac (nth parts 0) (get-mac-sha256 (to-bytes (str "AWS4" *secret*))))
     (get-mac-sha256)
     (calculate-hmac (nth parts 1))
     (get-mac-sha256)
     (calculate-hmac (nth parts 2))
     (get-mac-sha256)
     (calculate-hmac (nth parts 3)))))

(defn- auth-headers
  "Returns the version 4 signature authorisation headers"
  [{:keys [url method content-type headers body]}]
  (let [opts (parse-url url)
        host (:host opts)
        time (ISO8601-time)
        s-key (signing-key host time)
        headers (into (sorted-map)
                      (merge (keys-as-keyword headers)
                             (without-nils {:content-type content-type
                                            :x-amz-date time
                                            :host host})))
        sign-string (string-to-sign
                     host
                     time
                     (canonical-request method (:path opts) (:query opts) headers body))
        signature (hex (calculate-hmac sign-string (get-mac-sha256 s-key)))]
    (log/debug "V4: Headers are " headers)
    (log/debug "V4: Sign string is " sign-string)
    (log/debug "V4: Signature is " signature)
    (merge (keys-as-string headers) {"Authorization" (str v4-algorithm
                                                          " Credential="
                                                          *key*
                                                          "/"
                                                          (credential-scope host time)
                                                          ", SignedHeaders="
                                                          (signed-headers headers)
                                                          ", Signature="
                                                          signature)})))

(defn- add-query-string
  [{:keys [url params] :as request}]
  (if params
    (let [query-string (map-to-query-string
                        (into (sorted-map) (keys-as-keyword params)))
          appender (if (pos? (.indexOf url "?")) "&" "?")
          url-with-params (str url appender query-string)]
      (assoc request :url url-with-params))
    request))

(defn- add-default-method
  [{:keys [method] :as request}]
  (if method
    request
    (assoc request :method :get)))

(defn sign
  "http://docs.aws.amazon.com/general/latest/gr/signature-version-4.html"
  [{:keys [url method] :as request}]
  (log/debug "V4: Processing signature for url : " url)
  (let [request (-> request
                    add-query-string
                    add-default-method)
        headers (auth-headers request)]
    (without-nils {:url (request :url)
                   :body (request :body)
                   :method (request :method)
                   :content-type (request :content-type)
                   :headers headers})))
