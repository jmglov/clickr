(ns clickr.s3
  (:require [babashka.fs :as fs]
            [cognitect.aws.client.api :as aws]))

(defn init-client [{:keys [aws-region] :as ctx}]
  (let [client (aws/client {:api :s3, :region aws-region})]
    (assoc ctx :s3 {:client client})))

(defn upload-photo! [{:keys [s3 s3-bucket s3-prefix] :as ctx}
                     {:keys [out-file] :as photo}]
  (let [s3-key (format "%s/%s/%s"
                       s3-prefix
                       (-> photo :out-file fs/parent fs/file-name)
                       (-> photo :out-file fs/file-name))]
    (aws/invoke (:client s3)
                {:op :PutObject
                 :request {:Bucket s3-bucket
                           :Key s3-key
                           :Body (fs/read-all-bytes out-file)}})
    (assoc photo :s3-key s3-key)))

(defn upload-album! [ctx {:keys [photos] :as album}]
  (update album :photos #(doall (map (partial upload-photo! ctx) %))))
