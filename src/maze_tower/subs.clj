(ns maze-tower.subs
  (:require [cljfx.api :as fx]
            ))

(defn pics-count
  "获取迷宫图片总数"
  [ctx]
  (-> (fx/sub ctx :maze-pic-infos)
      count
      str))
