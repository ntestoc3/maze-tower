(ns maze-tower.gui
  (:require [cljfx.api :as fx]
            [cljfx.lifecycle :as lifecycle]
            [cljfx.coerce :as coerce]
            [clojure.core.cache :as cache]
            [maze-tower.views :as views]
            [maze-tower.config :as config]
            [maze-tower.events :as events]))

;; 可设置项  迷宫生成算法，  迷宫行列范围  迷宫最小-最大路径  迷宫格最小-最大尺寸
;; 迷宫图片保存目录
;; 迷宫起点和终点图片

;; 指定flag文件，四个方向的代表字符，迷宫塔层数

;; 日志输出

(def *state
  (atom (fx/create-context
         {:maze-algo (config/get-config :maze-algo "recursive-backtracker")
          :maze-rows (config/get-config :maze-rows [10 100])
          :maze-cols (config/get-config :maze-cols [10 100])
          :maze-path-len (config/get-config :maze-path-len [50 100])
          :maze-cell-size (config/get-config :maze-cell-size [20 80])
          :maze-pics-dir (config/get-config :maze-pics-dir "maze_images/")
          :maze-start-pic (config/get-config :maze-start-pic "start.png")
          :maze-end-pic (config/get-config :maze-end-pic "end.png")
          :maze-gen-num (config/get-config :maze-gen-num 10)
          :tower-level (config/get-config :tower-level 30)
          :maze-pic-infos (config/get-config :maze-pic-infos [])
          :curr-pic-index (config/get-config :curr-pic-index 0)
          :showing true}
         cache/lru-cache-factory)))


(def event-handler
  (-> events/event-handler
      (fx/wrap-co-effects
       {:fx/context (fx/make-deref-co-effect *state) ;; 关联状态
        })
      (fx/wrap-effects
       {:dispatch fx/dispatch-effect
        :context (fx/make-reset-effect *state) ;; 更新状态
        })))


(def renderer
  (fx/create-renderer
    :middleware (comp
                  fx/wrap-context-desc
                  (fx/wrap-map-desc (fn [_]
                                      {:fx/type views/root})))
    :opts {:fx.opt/map-event-handler event-handler
           :fx.opt/type->lifecycle #(or (fx/keyword->lifecycle %)
                                        (fx/fn->lifecycle-with-context %))}))

(defn show
  []
  (fx/mount-renderer *state renderer))

(comment
  (show)


  )
