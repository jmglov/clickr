(ns clickr.util
  (:require [clojure.java.io :as io]
            [clojure.pprint :as pprint]))

(defn pprint [object]
  (with-out-str
    (pprint/pprint object)))

(defn pprint-file [object filename]
  (with-open [w (io/writer filename)]
    (pprint/pprint object w)))

(comment

  (pprint
   {:some-long-key (vec (range 10))
    :another-key {:why-not? "indeed"
                  :and-more-stuff 42}})
  ;; => "{:some-long-key [0 1 2 3 4 5 6 7 8 9],\n :another-key {:why-not? \"indeed\", :and-more-stuff 42}}\n"

  (pprint-file
   {:some-long-key (vec (range 10))
    :another-key {:why-not? "indeed"
                  :and-more-stuff 42}}
   "/tmp/foo.edn")
  ;; => nil

  (slurp "/tmp/foo.edn")
  ;; => "{:some-long-key [0 1 2 3 4 5 6 7 8 9],\n :another-key {:why-not? \"indeed\", :and-more-stuff 42}}\n"

  )
