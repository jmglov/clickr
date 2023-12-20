(ns album)

(def photos
  {{photos-edn | safe}})

(def config
  {:album-div-name "album"
   :album-min-width 900
   :album-width-pct 0.8
   :min-photo-widths [275 240]
   :num-photos-per-row 3
   :photo-padding 4})

(defn get-window-width []
  (.-innerWidth js/window))

(defn provisional-album-width [{:keys [album-width-pct] :as config}]
  (* (get-window-width) album-width-pct))

(defn set-album-width!
  "Sets the width of the album div based on the current window size and returns
   the new size of the album div."
  [{:keys [album-div-name album-min-width num-photos-per-row photo-padding]
    :as config}]
  (let [provisional-width (provisional-album-width config)
        padding-width (* photo-padding (dec num-photos-per-row))
        min-width (+ album-min-width padding-width)
        new-width (if (>= provisional-width min-width)
                    provisional-width
                    (* (get-window-width) 0.95))]
    (-> (.getElementById js/document album-div-name)
        .-style
        (.setProperty "width" (str new-width "px")))
    new-width))

(defn scale-photos [_config scaling-factor photos]
  (->> photos
       (map (fn [photo]
              (-> photo
                  (update :height / scaling-factor)
                  (update :width / scaling-factor))))))

(defn set-photo-styles!
  [_config {:keys [id width height x-offset y-offset] :as photo}]
  (let [div-id (str "photo-" id)
        transform (str "translate(" x-offset "px, " y-offset "px)")]
    (doto (-> (.getElementById js/document div-id) .-style)
      (.setProperty "width" (str width "px"))
      (.setProperty "height" (str height "px"))
      (.setProperty "transform" transform))
    photo))

(defn get-scale-factor [{:keys [album-width photo-padding] :as config} photos]
  (let [row-width (->> photos
                       (map :width)
                       (reduce +))
        available-width (- album-width (* photo-padding (dec (count photos))))]
    (/ row-width available-width)))

(defn normalise-height [_config photos]
  (let [min-height (apply min (map :height photos))]
    (->> photos
         (map (fn [{:keys [height width] :as photo}]
                (if (> height min-height)
                  (assoc photo
                         :height min-height
                         :width (/ min-height (/ height width)))
                  photo))))))

(defn arrange-row [{:keys [photo-padding] :as config} photos]
  (let [normalised-photos (normalise-height config photos)
        scale-factor (get-scale-factor config normalised-photos)]
    (->> normalised-photos
         (scale-photos config scale-factor)
         (reduce (fn [{:keys [x-offset] :as acc} {:keys [width] :as photo}]
                   (let [new-x-offset (+ x-offset width photo-padding)]
                     (-> acc
                         (assoc :x-offset new-x-offset)
                         (update :arranged conj (assoc photo :x-offset x-offset)))))
                 {:x-offset 0, :arranged []})
         :arranged)))

(defn arrange-photos
  [{:keys [num-photos-per-row photo-padding] :as config} photos]
  (->> photos
       (partition-all num-photos-per-row)
       (reduce (fn [{:keys [y-offset] :as acc} row-photos]
                 (let [arranged-row (->> row-photos
                                         (arrange-row config)
                                         (map #(assoc % :y-offset y-offset)))
                       new-y-offset (+ y-offset
                                       (:height (first arranged-row))
                                       photo-padding)]
                   (-> acc
                       (assoc :y-offset new-y-offset)
                       (update :arranged concat arranged-row))))
               {:y-offset photo-padding, :arranged []})
       :arranged))

(defn get-num-photos-per-row
  [{:keys [min-photo-widths num-photos-per-row] :as config} album-width]
  (loop [num-photos num-photos-per-row
         [min-width & min-widths] min-photo-widths]
    (let [avg-width (/ album-width num-photos)]
      (if (or (nil? min-width)
              (= 1 num-photos)
              (>= avg-width min-width))
        num-photos
        (recur (dec num-photos) min-widths)))))

(defn display-album!
  [{:keys [num-photos-per-row photo-padding] :as config} photos]
  (let [album-width (set-album-width! config)
        num-photos-per-row (get-num-photos-per-row config album-width)
        config (assoc config
                      :album-width album-width
                      :num-photos-per-row num-photos-per-row)
        padding-width (* photo-padding (dec num-photos-per-row))
        photo-width (-> (- album-width padding-width)
                        (/ num-photos-per-row))]
    (->> photos
         (arrange-photos config)
         (map (partial set-photo-styles! config))
         doall)))

(defn update-page! [& _]
  (display-album! config photos))

(.addEventListener js/window "resize" update-page!)
(update-page!)
