(ns user
  (:require [amazonica.core :as amazonica]))

(defn refresh-aws-credentials
  ([]
   (refresh-aws-credentials "flickr"))
  ([profile]
   (amazonica/defcredential (amazonica/get-credentials {:profile profile}))))
