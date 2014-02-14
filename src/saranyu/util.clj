(ns saranyu.util
  (:require
   [clj-time.local :refer [local-now to-local-date-time]]
   [clj-time.format :as format]
   [clojure.string :refer [join split]]))

(def ^:const new-line "\n")

(def rfc2616-format (format/formatter "EEE, dd MMM yyyy HH:mm:ss 'GMT'"))

(defn rfc2616-time
  "http://www.rfc-editor.org/rfc/rfc2616.txt"
  []
  (format/unparse rfc2616-format (local-now)))

(def v4-format (format/formatter "yyyyMMdd"))

(defn v4-date
  [time]
  (format/unparse v4-format (to-local-date-time time)))

(def ISO8601-format (format/formatter "yyyyMMdd'T'HHmmss'Z'"))

(defn ISO8601-time
  "https://en.wikipedia.org/wiki/ISO_8601"
  []
  (format/unparse ISO8601-format (local-now)))

(defn current-time
  "Current time in UTC format"
  []
  (str (local-now)))

(defn url-encode
  "The java.net.URLEncoder class encodes for application/x-www-form-urlencoded. (RFC 3986 encoding)"
  [s]
  (-> (java.net.URLEncoder/encode s "UTF-8")
      (.replace "+" "%20")
      (.replace "*" "%2A")
      (.replace "%7E" "~")))

(defn to-bytes
  "Converts a string to a byte array"
  [str]
  (.getBytes str "UTF-8"))

(defn keys-as-string
  "Returns a map with all of its keys as string"
  [m]
  (into {}
        (for [[k v] m]
          [(name k) v])))

(defn keys-as-keyword
  "Returns a map with all of its keys as keyword"
  [m]
  (into {}
        (for [[k v] m]
          [(keyword k) v])))

(defn- str-val
  [val]
  (if (coll? val)
    val
    (str val)))

(defn without-nils
  "Remove all keys from a map that have nil/empty values."
  [m]
  (into {} (filter (comp not empty? str-val val) m)))

(defn map-to-query-string
  "Builds a query string from map"
  ([params use-empty-str]
     (join "&"
           (map (fn [[k v]] (if (empty? (str v))
                              (str (name k) (when use-empty-str "="))
                              (str (name k) "=" (str v))))
                params)))
  ([params]
     (map-to-query-string params false)))

(defn query-string-to-map
  "Builds a map from query string values"
  [query]
  (when query (->> (split query #"&")
                   (map #(split % #"="))
                   (map (fn [[k v]] [(keyword (url-encode k)) (when v (url-encode v))]))
                   (into (sorted-map)))))
