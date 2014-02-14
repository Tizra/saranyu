(ns saranyu.s3
  (:require
   [saranyu
    [util :refer :all]
    [crypto :refer :all]]
   [clojure.tools.logging :as log]
   [clojure.string :refer [split join upper-case trim lower-case]])
  (:import
   [java.net URL]))

(def ^:private s3-sub-resources #{:versioning :location :acl :torrent
                                  :lifecycle :versionId :logging :notification
                                  :partNumber :policy :requestPayment :uploadId
                                  :uploads :versions :website})

(defn- s3-path
  [host path]
  (if (empty? (re-find #"^s3.*.amazonaws.com" host))
    (str "/" (first (split host #".s3")) path)
    path))

(defn- add-query
  [path q-map]
  (let [opts  (into (sorted-map)
                    (map #(if (contains? q-map %) {% (% q-map)})
                         s3-sub-resources))]
    (if (empty? opts)
      path
      (str path "?" (map-to-query-string opts)))))

(defn- canonical-path
  [url]
  (let [url (URL. url)
        path (s3-path (.getHost url) (.getPath url))
        q-string (.getQuery url)
        path (if (empty? q-string) path
                 (add-query path (query-string-to-map q-string)))]
    (if (empty? path) "/" path)))

(defn- get-amz-headers
  [headers]
  (apply dissoc headers
         (keep #(-> % key name (.startsWith "x-amz-") (if nil (key %)))
               headers)))

(defn- amz-headers-str
  [headers]
  (let [amz-headers (get-amz-headers headers)
        headers-str (join new-line
                          (map #(str (lower-case (name (key %))) ":" (trim (val %)))
                               amz-headers))]
    (if-not (empty? headers-str)
      (str headers-str new-line))))

(defn- string-to-sign
  [request]
  (let [headers-str (amz-headers-str (without-nils (request :headers)))
        canon-path (canonical-path (request :url))]
    (log/debug "S3: Headers string is " headers-str)
    (log/debug "S3: Canonical Path is " canon-path)
    (str (upper-case (name (request :method))) new-line
         new-line
         (request :content-type) new-line
         new-line
         headers-str
         canon-path)))

(defn- s3-header
  "Gets the S3 Authorisation header for the given url"
  [request]
  (let [str-to-sign (string-to-sign request)
        signature (base64 (calculate-hmac str-to-sign (get-mac-sha1 *secret*)))
        signature-string (str "AWS " *key* ":" signature)]
    (log/debug "S3: String to Sign is " str-to-sign)
    (log/debug "S3: Signature String is " signature-string)
    {"Authorization" signature-string}))

(defn- auth-headers
  ([request]
     (let [date (rfc2616-time)
           headers (without-nils {"x-amz-date" date
                                  "x-amz-security-token" *session-token*})]
       (merge headers (s3-header (merge request {:headers headers}))))))

(defn sign
  "http://s3.amazonaws.com/doc/s3-developer-guide/RESTAuthentication.html"
  [{:keys [url method content-type] :as req}]
  (log/debug "S3: Processing S3 signature for url : " url)
  (let [method (or method :get)
        content-type (if (and (= method :put) (nil? content-type))
                       "application/xml"
                       content-type)
        headers (auth-headers (-> req
                                  (assoc :method method)
                                  (assoc :content-type content-type)))]
    (without-nils {:url url
                   :method method
                   :headers headers
                   :body (req :body)
                   :content-type content-type})))
