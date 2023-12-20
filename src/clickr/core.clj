(ns clickr.core
  (:require [clickr.flickr :as flickr]
            #_[clickr.html :as html]
            #_[clickr.s3 :as s3]
            [clojure.java.io :as io]))

(comment
  (def flickr-api-key (env :flickr-api-key))
  (def flickr-secret (env :flickr-secret))
  (def s3-bucket (env :s3-bucket))
  (def thumbnail-dir "thumbnails")

  (defn copy-album! [{:keys [title photos] :as album}]
    (let [prefix (s3/->key title)]
      (s3/put-html! s3-bucket prefix "index.html" (html/->html (html/make-index album thumbnail-dir)))
      (->> photos
           (map #(s3/upload-photo! s3-bucket prefix (:filename %) %))
           (map #(s3/upload-photo! s3-bucket (str prefix "/" thumbnail-dir) (:thumbnail-filename %) %))
           (map #(do (io/delete-file (:filename %)) %))
           (map #(do (io/delete-file (:thumbnail-filename %) %)))
           doall))
    album)

  (defn copy-to-s3 []
    (let [client (flickr/make-client flickr-api-key flickr-secret)
          albums (->> (flickr/get-albums client)
                      (map (partial flickr/download-album! client))
                      (map copy-album!)
                      doall)]
      albums)))
