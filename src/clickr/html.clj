(ns clickr.html
  (:require [babashka.fs :as fs]
            [selmer.parser :as selmer]
            [clojure.pprint :as pprint]))

(defn ->edn [data]
  (with-out-str (pprint/pprint data)))

(defn apply-album-template [_ctx template-file album]
  (selmer/render (slurp template-file)
                 {:album (update album :photos vec)
                  :photos-edn (->> album
                                   :photos
                                   (map #(dissoc % :out-file :object))
                                   vec
                                   ->edn)}))

(defn album->html [ctx album]
  (apply-album-template ctx "resources/templates/album.html" album))

(defn album->css [ctx album]
  (apply-album-template ctx "resources/templates/style.css" album))

(defn write-album-html! [ctx {:keys [out-dir] :as album}]
  (when-not out-dir
    (throw (ex-info "Album must be downloaded before writing it to HTML"
                    {:album album})))
  (let [html (album->html ctx album)
        html-file (fs/file out-dir "index.html")
        css (album->css ctx album)
        css-file (fs/file out-dir "style.css")
        cljs (apply-album-template ctx "resources/templates/album.cljs" album)
        cljs-file (fs/file out-dir "album.cljs")
        bb-edn (apply-album-template ctx "resources/templates/bb.edn" album)
        bb-edn-file (fs/file out-dir "bb.edn")]
    (spit html-file html)
    (spit css-file css)
    (spit cljs-file cljs)
    (spit bb-edn-file bb-edn)
    (assoc album :html-file html-file, :css-file css-file
           :cljs-file cljs-file, :bb-edn-file bb-edn-file)))
