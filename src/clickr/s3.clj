(ns clickr.s3
  (:require [amazonica.aws.s3 :as s3]
            [clojure.string :as string]
            [clojure.java.io :as io])
  (:import (java.io ByteArrayInputStream)))

(def key-regex #"[^-!_.*'()0-9A-Za-z]")

(defn ->key [s]
  (string/replace s key-regex "-"))

(defn put-html! [bucket prefix filename html]
  (let [bytes (.getBytes html "UTF-8")]
    (with-open [in (ByteArrayInputStream. bytes)]
      (s3/put-object bucket
                     (str prefix "/" filename)
                     in
                     {:content-type "text/html"
                      :content-length (count bytes)}))))

(defn upload-photo! [bucket prefix filename photo]
  (with-open [in (io/input-stream filename)]
    (s3/put-object bucket
                   (str prefix "/" (->key (.getName (io/file filename))) ".jpg")
                   in
                   {:content-type "image/jpeg"}))
  photo)
