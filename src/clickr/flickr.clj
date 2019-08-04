(ns clickr.flickr
  (:import (com.flickr4java.flickr Flickr
                                   RequestContext
                                   REST)
           (com.flickr4java.flickr.auth Permission)
           (com.flickr4java.flickr.photos Size)
           (com.flickr4java.flickr.util FileAuthStore)
           (java.io BufferedInputStream
                    File
                    FileOutputStream)
           (java.util UUID))
  (:require [clojure.java.io :as io]
            [clojure.string :as string]))

(def tmpdir "/tmp")

(defn make-auth-store []
  (FileAuthStore. (io/file (str (System/getProperty "user.home")
                                File/separatorChar
                                ".flickrAuth"))))

(defn login-url [{:keys [flickr] :as client} api-key secret]
  (let [auth (.getAuthInterface flickr)
        req-token (.getRequestToken auth)]
    (merge client
           {:auth-url (.getAuthorizationUrl auth req-token Permission/READ)
            :auth auth
            :req-token req-token})))

(defn login [{:keys [auth auth-store req-token] :as client} code]
  (let [access-token (.checkToken auth (.getAccessToken auth req-token code))]
    (.store auth-store access-token)
    (assoc client :access-token access-token)))

(defn make-client [api-key secret]
  (let [client {:flickr (Flickr. api-key secret (REST.))
                :auth-store (make-auth-store)}
        [auth & _] (.retrieveAll (:auth-store client))]
    (if auth
      (let [request-context (RequestContext/getRequestContext)]
        (.setAuth request-context auth)
        (assoc client
               :auth auth
               :request-context request-context))
      (let [client (login-url client api-key secret)]
        (println "Authentication required; visit"
                 (:auth-url client)
                 "in your browser, then call (login client code)")
        client))))

(defn ->photo [photo]
  (let [filename (str tmpdir File/separatorChar "clickr-" (UUID/randomUUID))
        thumbnail-filename (str filename "-thumbnail")]
    {:id (.getId photo)
     :title (.getTitle photo)
     :description (.getDescription photo)
     :date-taken (.getDateTaken photo)
     :width (.getOriginalWidth photo)
     :height (.getOriginalHeight photo)
     :geo-data (.getGeoData photo)
     :rotation (.getRotation photo)
     :filename filename
     :thumbnail-filename thumbnail-filename
     :object photo}))

(defn ->album [{:keys [flickr] :as client} photoset]
  {:title (.getTitle photoset)
   :description (.getDescription photoset)
   :primary-photo (.. photoset getPrimaryPhoto getId)
   :photos (map ->photo (.getPhotos (.getPhotosetsInterface flickr) (.getId photoset) 500 1))
   :object photoset})

(defn get-albums [{:keys [auth flickr] :as client}]
  (let [user-id (.. auth getUser getId)]
    (->> (-> flickr
             (.getPhotosetsInterface)
             (.getList user-id)
             (.getPhotosets))
         (map (partial ->album client)))))

(defn download-file! [image-stream filename]
  (with-open [in (BufferedInputStream. image-stream)
              out (FileOutputStream. (io/file filename))]
    (io/copy in out)))

(defn download-photo!
  [{:keys [flickr] :as client} {:keys [filename thumbnail-filename object] :as photo}]
  (let [photos-interface (.getPhotosInterface flickr)]
    (download-file! (.getImageAsStream photos-interface object Size/LARGE) filename)
    (download-file! (.getImageAsStream photos-interface object Size/THUMB) thumbnail-filename))
  photo)

(defn download-album! [client {:keys [photos] :as album}]
  (doall (map (partial download-photo! client) photos))
  album)
