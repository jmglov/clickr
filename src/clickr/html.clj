(ns clickr.html
  (:require [clojure.java.io :as io]
            [hiccup.page :refer [html5]]))

(defn ->filename [f]
  (.getName (io/file f)))

(defn ->html [[head body]]
  (html5 {:lang "en"} head body))

(defn ->photo [thumbnail-dir {:keys [title description filename thumbnail-filename]}]
  (let [[filename thumbnail-filename] (map ->filename [filename thumbnail-filename])]
    (-> [:div
         [:a {:href filename}
          [:img {:src (str thumbnail-dir "/" thumbnail-filename)}]]]
        (concat (when title [[:p {:style {:font-weight "bold"}} title]]))
        (concat (when description [[:p {:style {:font-style "italic"}} description]]))
        vec)))

(defn make-index [{:keys [title description photos] :as album} thumbnail-dir]
  [[:head [:title title]]
   [:body
    [:h1 title]
    [:p description]
    [:div
     (map (partial ->photo thumbnail-dir) photos)]]])
