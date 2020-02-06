(ns maze-tower.gui.subs
  (:require [cljfx.api :as fx]))

(defn pics-count
  "获取迷宫图片总数"
  [ctx]
  (-> (fx/sub ctx :maze-pic-infos)
      count
      long))

(defn curr-image-path
  [ctx]
  (let [idx (fx/sub ctx :curr-pic-index)
        mazes (fx/sub ctx :maze-pic-infos)]
    (when-not (empty? mazes)
      (-> (nth mazes idx)
          :image-path))))

(defn curr-maze-route
  [ctx]
  (let [idx (fx/sub ctx :curr-pic-index)
        mazes (fx/sub ctx :maze-pic-infos)]
    (when-not (empty? mazes)
      (-> (nth mazes idx)
          :route))))
