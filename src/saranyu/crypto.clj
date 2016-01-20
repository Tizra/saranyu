(ns saranyu.crypto
  (:require
   [saranyu.util :refer :all])
  (:import
   [java.net URL]
   [java.net URLEncoder]
   [javax.crypto Mac]
   [javax.crypto.spec SecretKeySpec]
   [java.security MessageDigest]
   [org.apache.commons.codec.binary Base64]
   [org.apache.commons.codec.binary Hex]))

(def ^:const hmac-sha256-algorithm  "HmacSHA256")
(def ^:const hmac-sha1-algorithm  "HmacSHA1")

(defn hex
  [b]
  (Hex/encodeHexString b))

(defn base64
  [b]
  (Base64/encodeBase64String b))

(defn get-mac-sha1
  [aws-secret]
  (let [signing-key (SecretKeySpec. (to-bytes aws-secret) hmac-sha1-algorithm)
        mac (Mac/getInstance hmac-sha1-algorithm)]
    (.init mac signing-key)
    mac))

(defn get-mac-sha256
  [secret]
  (let [signing-key (SecretKeySpec. secret hmac-sha256-algorithm)
           mac (Mac/getInstance hmac-sha256-algorithm)]
       (.init mac signing-key)
       mac))

(defn calculate-hmac
  [data sha]
  (.doFinal sha (to-bytes data)))

(defn sha-256
  [str]
  (let [md (MessageDigest/getInstance "SHA-256")]
    (.update md (to-bytes str))
    (.digest md)))
