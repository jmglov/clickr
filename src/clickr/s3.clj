(ns clickr.s3
  (:require [amazonica.aws.s3 :as s3]
            [clojure.string :as string]
            [clojure.java.io :as io]))

(def key-regex #"[^-!_.*'()0-9A-Za-z]")

(defn ->key [s]
  (string/replace s key-regex "-"))

(defn upload-photo! [bucket prefix filename photo]
  (let [prefix prefix]
    (with-open [in (io/input-stream filename)]
      (s3/put-object bucket
                     (str prefix "/" (->key (.getName (io/file filename))) ".jpg")
                     in
                     {:content-type "image/jpeg"})))
  photo)
