(ns clickr.core
  (:require [cheshire.core :as json]
            [clj-http.client :as http]
            [clojure.string :as string]
            [environ.core :refer [env]])
  (:import (java.net URLEncoder)
           (java.util Base64)
           (javax.crypto Mac)
           (javax.crypto.spec SecretKeySpec)))

(def flickr-api-key (env :flickr-api-key))
(def flickr-secret (env :flickr-secret))

(def flickr-base-uri "https://www.flickr.com/services/")
(def flickr-rest-uri (str flickr-base-uri "rest/"))
(def flickr-request-token-uri (str flickr-base-uri "oauth/request_token"))

(defn ->base64 [bytes]
  (.encode (Base64/getEncoder) bytes))

(defn ->hmac [bytes token-secret]
  (let [mac (make-mac token-secret)]
    (.doFinal mac bytes)))

(defn params->string [params]
  (->> params
       (map (fn [[k v]] (str k "=" v)))
       (string/join "&")
       (URLEncoder/encode)))

(defn oauth-query-params [api-key callback-uri]
  {"oauth_nonce" (str (rand-int 100000000))
   "oauth_timestamp" (str (int (/ (System/currentTimeMillis) 1000)))
   "oauth_consumer_key" flickr-api-key
   "oauth_signature_method" "HMAC-SHA1"
   "oauth_version" "1.0"
   "oauth_callback" (URLEncoder/encode callback-uri "UTF-8")})

(defn key-bytes [token-secret]
  (-> (str token-secret "&")
      (.getBytes "UTF-8")))

(defn make-mac [token-secret]
  (doto (Mac/getInstance "HmacSHA1")
    (.init (SecretKeySpec. (key-bytes token-secret) "HmacSHA1"))))

(defn oauth-signature [token-secret uri query-params]
  (let [base (string/join "&" ["GET" (URLEncoder/encode uri) (params->string query-params)])]
    (-> base
        (.getBytes "UTF-8")
        (->hmac token-secret)
        ->base64
        (String. "UTF-8")
        (.trim))))
