(ns clickr.core
  (:require [clickr.flickr :as flickr]
            [clickr.s3 :as s3]
            [clojure.java.io :as io]
            [environ.core :refer [env]]))

(def flickr-api-key (env :flickr-api-key))
(def flickr-secret (env :flickr-secret))
(def s3-bucket (env :s3-bucket))

(defn copy-album! [{:keys [title photos] :as album}]
  (->> photos
       (map #(s3/upload-photo! s3-bucket (s3/->key title) (:filename %) %))
       (map #(s3/upload-photo! s3-bucket (str (s3/->key title) "/thumbnails") (:thumbnail-filename %) %))
       (map #(do (io/delete-file (:filename %)) %))
       (map #(do (io/delete-file (:thumbnail-filename %) %)))
       doall)
  album)

(defn copy-to-s3 []
  (let [client (flickr/make-client flickr-api-key flickr-secret)
        albums (->> (flickr/get-albums client)
                    (take 1)
                    (map (partial flickr/download-album! client))
                    (map copy-album!)
                    doall)]
    albums))
