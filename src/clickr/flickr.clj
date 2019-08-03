(ns clickr.flickr
  (:import (com.flickr4java.flickr Flickr
                                   RequestContext
                                   REST)
           (com.flickr4java.flickr.auth Permission)
           (com.flickr4java.flickr.util FileAuthStore)
           (java.io File))
  (:require [clojure.string :as string]))

(defn make-auth-store []
  (FileAuthStore. (File. (str (System/getProperty "user.home")
                              File/separatorChar
                              ".flickrAuth"))))

(defn login-url [{:keys [flickr] :as client} api-key secret]
  (let [auth (.getAuthInterface flickr)
        req-token (.getRequestToken auth)]
    (merge client
           {:auth-url (.getAuthorizationUrl auth req-token Permission/READ)
            :auth auth
            :req-token req-token})))

(defn login [{:keys [auth auth-store] :as client} code]
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

(defn photosets [{:keys [auth flickr]}]
  (let [user-id (.. auth getUser getId)]
    (-> flickr
        (.getPhotosetsInterface)
        (.getList user-id)
        (.getPhotosets))))
