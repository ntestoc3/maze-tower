(ns maze-tower.maze.image
  (:require [clojure.java.io :as io]
            [maze-tower.maze.grid :as grid]
            [maze-tower.util :as util])
  (:import [java.awt Color Graphics2D]
           [java.awt.image BufferedImage]
           [java.awt Image]
           java.awt.geom.AffineTransform
           [javax.imageio ImageIO]))

;; TODO: update this to work like the cljs version and use a RenderState
;; object and not require annotating the cells.
(defn draw-cells [^Graphics2D g grid cell-size draw-fn]
  (doseq [coord (grid/grid-coords grid)]
    (let [[y1 x1] (map #(* % cell-size) coord)
          [y2 x2] (map #(+ % cell-size) [y1 x1])
          cell (grid/grid-cell grid coord)]
      (draw-fn g cell cell-size x1 y1 x2 y2))))

(defn open-scaled-img [filename width height]
  (-> (util/file-open filename)
      ImageIO/read
      (.getScaledInstance width height Image/SCALE_SMOOTH)))

(defn draw-cell-background [^Graphics2D g cell cell-size x1 y1 _ _]
  (when-let [color (:color cell)]
    (.setColor g color)
    (.fillRect g x1 y1 cell-size cell-size))
  (when-let [img (:image cell)]
    (.drawImage g (open-scaled-img img cell-size cell-size)
                (AffineTransform. 1. 0. 0. 1. ^java.lang.Float (float x1) ^java.lang.Float (float y1))
                nil)))

(defn draw-cell-walls [^Graphics2D g cell _ x1 y1 x2 y2]
  (when-not (::grid/north cell) (.drawLine g x1 y1 x2 y1))
  (when-not (::grid/west cell) (.drawLine g x1 y1 x1 y2))

  (when-not (grid/linked? cell (::grid/east cell)) (.drawLine g x2 y1 x2 y2))
  (when-not (grid/linked? cell (::grid/south cell)) (.drawLine g x1 y2 x2 y2)))

(defn image-grid [{:keys [::grid/rows ::grid/cols] :as grid} cell-size]
  (let [img-width (inc (* cell-size cols))
        img-height (inc (* cell-size rows))
        background Color/white
        wall Color/black
        img (BufferedImage. img-width img-height BufferedImage/TYPE_INT_RGB)
        g (.createGraphics img)]
    (.setColor g background)
    (.fillRect g 0 0 img-width img-height)
    (draw-cells g grid cell-size draw-cell-background)
    (.setColor g wall)
    (draw-cells g grid cell-size draw-cell-walls)
    img))
