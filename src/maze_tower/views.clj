(ns maze-tower.views
  (:require [cljfx.api :as fx]
            [cljfx.ext.table-view :as ext-table-view]
            [clojure.java.io :as io]
            [maze-tower.events :as events]
            [maze-tower.util :as util]
            [maze-tower.maze-algo :as maze-algo]
            [maze-tower.subs :as subs]
            [maze-tower.config :as config])
  (:import javafx.util.StringConverter))

(def range-value-converter
  (proxy [StringConverter] []
    (fromString [s]
      (util/parse-range s))
    (toString [v]
      (util/range->str v))))

(defn text-input [{:keys [fx/context key type value-converter filter tip value-change-event opts]
                   :or {type :text-field
                        value-converter :default
                        value-change-event ::events/value-changed
                        tip nil
                        opts {}}}]
  (merge {:fx/type type
          :text-formatter (merge {:fx/type :text-formatter
                                  :value-converter value-converter
                                  :value (fx/sub context key)
                                  :on-value-changed {:event/type value-change-event
                                                     :key key
                                                     :fx/sync true}}
                                 (when filter
                                   {:filter filter}))}
         (when tip
           {:tooltip {:fx/type :tooltip
                      :show-duration [10 :s]
                      :text tip}})
         opts))

(defn image-view
  "从资源或文件中加载图片，如果找不到文件显示空白"
  [{:keys [fx/context image opts]}]
  (merge {:fx/type :image-view}
         (when image
           (when-let [is (util/file-istream image)]
             {:image {:is is}}))
         opts
         ))

(defn image-button
  "图片按钮
  `content-display` 图片的显示位置"
  [{:keys [fx/context text image on-action content-display opts]
    :or {content-display :left}}]
  (merge {:fx/type :button
          :graphic-text-gap 10
          :content-display content-display
          :graphic {:fx/type image-view
                    :opts {:fit-width 20
                           :fit-height 20}
                    :image image}
          :on-action on-action
          :text text}
         opts))

(defn log-form [_]
  {:fx/type :titled-pane
   :text "日志记录"
   :collapsible false
   :content {:fx/type :text-area
             :editable false
             }})

(defn maze-config-form [{:keys [fx/context]}]
  {:fx/type :v-box
   :children [{:fx/type :h-box
               :spacing 10
               :fill-height true
               :padding {:top 5}
               :children [{:fx/type :label
                           :max-height ##Inf
                           :text "生成算法："}
                          {:fx/type :choice-box
                           :value (fx/sub context :maze-algo)
                           :on-value-changed {:event/type ::events/value-changed
                                              :key :maze-algo
                                              :fx/sync true}
                           :items (keys maze-algo/algorithm-functions)
                           :max-height ##Inf}
                          {:fx/type :label
                           :max-height ##Inf
                           :text "行数："}
                          {:fx/type text-input
                           :max-height ##Inf
                           :value-converter range-value-converter
                           :tip "生成迷宫的行数:
可以使用固定数字，比如5  生成固定行数
或者范围，比如1-10 生成1-10之间的随机行数(包含1和10)
或者逗号隔开的多个数字，比如1,2,5,3  生成行数为从中随机挑选一个数字"
                           :key :maze-rows}
                          {:fx/type :label
                           :max-height ##Inf
                           :text "列数："}
                          {:fx/type text-input
                           :max-height ##Inf
                           :value-converter range-value-converter
                           :tip "生成迷宫的列数:
可以使用固定数字，比如5  生成固定列数
或者范围，比如1-10 生成1-10之间的随机列数(包含1和10)
或者逗号隔开的多个数字，比如1,2,5,3  生成列数为从中随机挑选一个数字"
                           :key :maze-cols}
                          {:fx/type :label
                           :max-height ##Inf
                           :text "路径长度："}
                          {:fx/type text-input
                           :max-height ##Inf
                           :value-converter range-value-converter
                           :tip "生成迷宫的寻路路径长度:
可以使用固定数字，比如5  生成固定路径长度
或者范围，比如20-100 生成20-100之间的随机路径长度(包含20和100)
或者逗号隔开的多个数字，比如20,30,50,100 生成的路径长度为从中随机挑选一个数字"
                           :key :maze-path-len}]}
              {:fx/type :h-box
               :spacing 10
               :fill-height true
               :padding {:top 5}
               :children [{:fx/type :label
                           :max-height ##Inf
                           :text "单元格大小："}
                          {:fx/type text-input
                           :max-height ##Inf
                           :value-converter range-value-converter
                           :tip "生成迷宫图片的单元格大小(单位为像素)：
可以使用固定数字，比如20  生成20px * 20px的单元格大小
或者范围，比如20-100 生成20-100之间的随机单元格大小(包含20和100)
或者逗号隔开的多个数字，比如20,30,50,100 生成的单元格大小为从中随机挑选一个数字"
                           :key :maze-cell-size}
                          {:fx/type image-button
                           :image  (fx/sub context :maze-start-pic)
                           :content-display :right
                           :on-action {:event/type ::events/add-data}
                           :text "开始标记"}
                          {:fx/type image-button
                           :image  (fx/sub context :maze-end-pic)
                           :content-display :right
                           :on-action {:event/type ::events/add-data}
                           :text "结束标记"}]}
              {:fx/type :h-box
               :spacing 10
               :fill-height true
               :padding {:top 5}
               :children [{:fx/type :label
                           :max-height ##Inf
                           :text "生成数量："}
                          {:fx/type :combo-box
                           :value (fx/sub context :maze-gen-num)
                           :editable true
                           :converter :long
                           :tooltip {:fx/type :tooltip
                                     :show-duration [10 :s]
                                     :text "生成的迷宫图片数量"}
                           :on-value-changed {:event/type ::events/value-changed
                                              :key :maze-gen-num
                                              :fx/sync true}
                           :items [1 10 30 50 100 150 200 300]
                           :max-height ##Inf}
                          {:fx/type :label
                           :max-height ##Inf
                           :text "迷宫图片保存文件夹:"}
                          {:fx/type :h-box
                           :h-box/hgrow :always
                           :children [{:fx/type :text-field
                                       :max-height ##Inf
                                       :h-box/hgrow :always
                                       :editable false
                                       :text (fx/sub context :maze-pics-dir)}
                                      {:fx/type image-button
                                       :image  "open_dir.png"
                                       :on-action {:event/type ::events/change-pic-dir}}]}
                          {:fx/type :h-box
                           :children [{:fx/type :label
                                       :max-height ##Inf
                                       :text "迷宫图片总数："}
                                      {:fx/type :label
                                       :max-height ##Inf
                                       :text (str (fx/sub context subs/pics-count))}]}]}]})

(defn function-form [{:keys [fx/context]}]
  {:fx/type :v-box
   :children [{:fx/type image-button
               :on-action {:event/type ::events/gen-maze}
               :image "gen.png"
               :opts {:max-width ##Inf}
               :text "生成"}
              {:fx/type image-button
               :v-box/margin {:top 50}
               :opts {:max-width ##Inf}
               :image "del.png"
               :on-action {:event/type ::events/del-all-maze-pid}
               :text "清空"}
              {:fx/type :h-box
               :v-box/margin {:top 50}
               :children [{:fx/type image-button
                           :image "prev.png"
                           :on-action {:event/type ::events/prev-maze-pic}}
                          {:fx/type text-input
                           :max-height ##Inf
                           :value-change-event ::events/pic-index-change
                           :opts {:pref-width 40}
                           :value-converter :long
                           :filter (fn [change]
                                     (let [new-txt (.getControlNewText change)]
                                       (try
                                         (let [v (Integer/parseInt new-txt)
                                               total-pics (config/get-config :curr-pic-index)]
                                           (when (<= 0 v (dec total-pics))
                                             change))
                                         (catch Exception e nil))))
                           :tip "当前显示迷宫图片的索引"
                           :key :curr-pic-index}
                          {:fx/type image-button
                           :image "next.png"
                           :on-action {:event/type ::events/next-maze-pic
                                       :fx/sync true}
                           }]}]})

(defn maze-pic-pane [{:keys [fx/context]}]
  {:fx/type :v-box
   :spacing 10
   :children [{:fx/type :scroll-pane
               :v-box/vgrow :always
               :content {:fx/type image-view
                         :image (fx/sub context :curr-image-path)}}
              {:fx/type :h-box
               :max-width ##Inf
               :spacing 10
               :children [{:fx/type :label
                           :text "文件名:"}
                          {:fx/type :text-field
                           :h-box/hgrow :always
                           :editable false
                           :text (fx/sub context :curr-image-path)}]}
              {:fx/type :h-box
               :spacing 10
               :children [{:fx/type :label
                           :text "迷宫路径："}
                          {:fx/type :text-field
                           :h-box/hgrow :always
                           :editable false
                           :text (fx/sub context :curr-maze-route)}]}]})

(defn root [_]
  {:fx/type :stage
   :on-close-request {:event/type ::events/stop}
   :showing true
   :title "迷宫塔"
   :scene {:fx/type :scene
           :root {:fx/type  :border-pane
                  :min-width 850
                  :min-height 500
                  :top {:fx/type maze-config-form
                        :border-pane/margin 10
                        }
                  :right {:fx/type function-form
                          :border-pane/margin 10
                          }
                  :center {:fx/type maze-pic-pane
                           :border-pane/margin 10
                           }
                  :bottom {:fx/type log-form
                           :border-pane/margin 10}
                  }}})
