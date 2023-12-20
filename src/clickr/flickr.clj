(ns clickr.flickr
  (:require [babashka.fs :as fs]
            [clickr.util :as util]
            [clojure.java.io :as io]
            [clojure.string :as str])
  (:import (com.flickr4java.flickr Flickr
                                   RequestContext
                                   REST)
           (com.flickr4java.flickr.auth Permission)
           (com.flickr4java.flickr.photos Size)
           (com.flickr4java.flickr.util FileAuthStore)
           (java.io BufferedInputStream
                    FileOutputStream)
           (javax.imageio ImageIO)))

(defn make-auth-store []
  (FileAuthStore. (fs/file (fs/home) ".flickrAuth")))

(defn authorise [{:keys [flickr] :as ctx}]
  (let [{:keys [client auth-store req-token token-key]} flickr
        auth-interface (.getAuthInterface client)
        auth (-> auth-store .retrieveAll first)]
    (cond
      ;; We have a valid access token from the auth store
      auth
      (do
        (.setAuth (RequestContext/getRequestContext) auth)
        (update ctx :flickr assoc :auth auth))

      ;; We have a request token and a token key, so exchange the request
      ;; token for an access token
      (and req-token token-key)
      (let [access-token (.getAccessToken auth-interface req-token token-key)
            auth (.checkToken auth-interface access-token)]
        (.setAuth (RequestContext/getRequestContext) auth)
        (.store auth-store auth)
        (update ctx :flickr dissoc :req-token :token-key :url))

      ;; We don't have any tokens, so grab a request token and the URL to
      ;; authorise it
      :default
      (let [req-token (.getRequestToken auth-interface)
            url (.getAuthorizationUrl auth-interface req-token Permission/READ)]
        (update ctx :flickr assoc :url url, :req-token req-token)))))

(defn init-client [{:keys [api-key secret] :as ctx}]
  (let [client (Flickr. api-key secret (REST.))
        auth-store (make-auth-store)]
    (-> ctx
        (assoc :flickr {:client client, :auth-store auth-store})
        authorise)))

(defn ->photo [_ photo]
  (let [id (.getId photo)
        extension (.getOriginalFormat photo)
        filename (format "%s.%s" id extension)]
    {:id id
     :filename filename
     :title (.getTitle photo)
     :description (.getDescription photo)
     :date-taken (.getDateTaken photo)
     :width (.getOriginalWidth photo)
     :height (.getOriginalHeight photo)
     :geo-data (.getGeoData photo)
     :rotation (.getRotation photo)
     :object photo}))

(defn ->album [{:keys [flickr] :as ctx} photoset]
  (let [ps-interface (.getPhotosetsInterface (:client flickr))]
    {:id (.getId photoset)
     :title (.getTitle photoset)
     :description (.getDescription photoset)
     :photos (->> (.getPhotos ps-interface (:id album) 500 1)
                  (map (partial ->photo ctx)))
     :object photoset}))

(defn get-albums [{:keys [flickr] :as ctx}]
  (let [user-id (.. (:auth flickr) getUser getId)]
    (->> (-> (:client flickr)
             (.getPhotosetsInterface)
             (.getList user-id)
             (.getPhotosets))
         (map (partial ->album ctx)))))

(defn download-photo! [{:keys [flickr out-dir] :as ctx}
                       {:keys [filename] :as photo}]
  (let [p-interface (.getPhotosInterface (:client flickr))
        out-file (fs/file out-dir filename)]
    (when-not (fs/exists? out-file)
      (with-open [in (BufferedInputStream. (.getImageAsStream p-interface (:object photo) Size/LARGE))
                  out (FileOutputStream. out-file)]
        (io/copy in out)))
    (let [img (ImageIO/read out-file)]
      (assoc photo
             :out-file out-file
             :width (.getWidth img)
             :height (.getHeight img)))))

(defn download-album! [ctx {:keys [id photos] :as album}]
  (let [album-dir (->> id (fs/file (fs/temp-dir)) fs/create-dirs fs/file)
        photos (->> photos
                    (map (partial download-photo! (assoc ctx :out-dir album-dir)))
                    doall)]
    (assoc album :out-dir album-dir, :photos photos)))
