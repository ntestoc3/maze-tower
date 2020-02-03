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

(defn non-exception-long-converter
  "不抛出异常的转换器,可提供默认值，出现异常则使用默认值"
  ([] (non-exception-long-converter 0))
  ([default]
   (proxy [StringConverter] []
     (fromString [s]
       (try
         (Long/parseLong s)
         (catch Exception e default)))
     (toString [v]
       (str v)))))

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

(defmacro image-view
  "从资源或文件中加载图片，如果找不到文件显示空白
  如果使用函数，会出现image的fx/sub更新后，但是不会调用这个函数进行更新，
  使用宏就等于嵌入代码，可以正常更新"
  [image & opts]
  `(merge {:fx/type :image-view}
          (when ~image
            (when-let [is# (util/file-istream ~image)]
              {:image {:is is#}}))
          ~(apply hash-map opts)
          ))

(defn image-button
  "图片按钮
  `content-display` 图片的显示位置"
  [{:keys [fx/context text image on-action content-display opts]
    :or {content-display :left}}]
  (merge {:fx/type :button
          :graphic-text-gap 10
          :content-display content-display
          :graphic (image-view image
                               :fit-width 20
                               :fit-height 20)
          :on-action on-action
          :text text}
         opts))

(defn file-choose-pane
  "文件选择组件
  :title为文字标识
  :image选择文件的图片
  :key要修改的context key,
  :type标识选择文件还是文件夹:file或:dir,默认选择文件
  :filter 文件过滤器，仅针对:type为:file有效"
  [{:keys [fx/context title image key type filter tooltip]
    :or {type :file}}]
  {:fx/type :h-box
   :children [{:fx/type :label
               :max-height ##Inf
               :text title}
              {:fx/type :text-field
               :max-height ##Inf
               :tooltip {:fx/type :tooltip
                         :show-duration [10 :s]
                         :text tooltip}
               :h-box/hgrow :always
               :editable false
               :text (fx/sub context key)}
              {:fx/type image-button
               :image image
               :on-action {:event/type ::events/file-choose
                           :title title
                           :type type
                           :filter filter
                           :key key}}]})

(defn log-form [_]
  {:fx/type :titled-pane
   :text "日志记录"
   :collapsible false
   :content {:fx/type :text-area
             :editable false
             }})

(defn maze-config-form [{:keys [fx/context]}]
  (let [maze-gen-num (fx/sub context :maze-gen-num)]
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
                             :on-action {:event/type ::events/file-choose
                                         :title "选择迷宫的开始标记图片"
                                         :type :file
                                         :filter events/image-filter
                                         :key :maze-start-pic}
                             :text "开始标记"}
                            {:fx/type image-button
                             :image  (fx/sub context :maze-end-pic)
                             :content-display :right
                             :on-action {:event/type ::events/file-choose
                                         :title "选择迷宫的结束标记图片"
                                         :type :file
                                         :filter events/image-filter
                                         :key :maze-end-pic}
                             :text "结束标记"}
                            {:fx/type :label
                             :max-height ##Inf
                             :text "生成数量："}
                            {:fx/type :combo-box
                             :value maze-gen-num
                             :editable true
                             :converter (non-exception-long-converter 0)
                             :tooltip {:fx/type :tooltip
                                       :show-duration [10 :s]
                                       :text "生成的迷宫图片数量"}
                             :on-value-changed {:event/type ::events/value-changed
                                                :key :maze-gen-num
                                                :fx/sync true}
                             :items [1 10 30 50 100 150 200 300]
                             :max-height ##Inf}
                            ]}
                {:fx/type :h-box
                 :spacing 10
                 :fill-height true
                 :padding {:top 5}
                 :children [{:fx/type file-choose-pane
                             :h-box/hgrow :always
                             :title "迷宫图片保存文件夹:"
                             :image  "open_dir.png"
                             :key :maze-pics-dir
                             :type :dir}
                            {:fx/type :h-box
                             :children [{:fx/type :label
                                         :max-height ##Inf
                                         :text "迷宫图片总数："}
                                        {:fx/type :label
                                         :max-height ##Inf
                                         :text (str (fx/sub context subs/pics-count))}]}]}]}))

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
               :on-action {:event/type ::events/del-all-maze-pic}
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
                           :tip "当前显示迷宫图片的索引"
                           :key :curr-pic-index}
                          {:fx/type image-button
                           :image "next.png"
                           :on-action {:event/type ::events/next-maze-pic
                                       :fx/sync true}
                           }]}]})

(defn maze-pic-pane [{:keys [fx/context]}]
  (let [image-path (fx/sub context subs/curr-image-path)]
    {:fx/type :v-box
     :spacing 10
     :children [{:fx/type :scroll-pane
                 :pannable true
                 :v-box/vgrow :always
                 :content (image-view image-path)}
                {:fx/type :h-box
                 :spacing 10
                 :children [{:fx/type :label
                             :text "文件名:"}
                            {:fx/type :text-field
                             :h-box/hgrow :always
                             :editable false
                             :text image-path}]}
                {:fx/type :h-box
                 :spacing 10
                 :children [{:fx/type :label
                             :text "迷宫路径："}
                            {:fx/type :text-field
                             :h-box/hgrow :always
                             :editable false
                             :text (fx/sub context subs/curr-maze-route)}]}]}))

(defn tower-gen-pane [{:keys [fx/context]}]
  (let [tower-level (fx/sub context :tower-level)
        total-maze (fx/sub context subs/pics-count)]
    {:fx/type :h-box
     :spacing 10
     :children [{:fx/type :label
                 :max-height ##Inf
                 :text "迷宫塔层数："}
                {:fx/type :combo-box
                 :value tower-level
                 :editable true

                 ;; 如果使用跟上次同样的值，则等于没改变，不会刷新界面
                 :converter (non-exception-long-converter 0)
                 :tooltip {:fx/type :tooltip
                           :show-duration [10 :s]
                           :text "生成的迷宫塔的层数
注意!必须先生成足够数量的迷宫图片"}
                 :on-value-changed {:event/type ::events/tower-level-change}
                 :items (take-while #(<= % total-maze)
                                    [5 10 30 50 100 150])
                 :max-height ##Inf}
                {:fx/type file-choose-pane
                 :h-box/hgrow :always
                 :title "迷宫塔最顶层文件："
                 :image  "file.png"
                 :tooltip "即包含的flag文件"
                 :key :tower-top-file
                 :type :file}
                {:fx/type image-button
                 :h-box/margin {:left 50}
                 :text "生成迷宫塔"
                 :image "taowa.png"
                 :on-action {:event/type ::events/gen-tower}}]}))

(defn root [_]
  {:fx/type :stage
   :on-close-request {:event/type ::events/stop}
   :showing true
   :title "迷宫塔"
   :scene {:fx/type :scene
           :root {:fx/type  :border-pane
                  :min-width 900
                  :min-height 600
                  :pref-width 900
                  :pref-height 600
                  :top {:fx/type maze-config-form
                        :border-pane/margin 10
                        }
                  :right {:fx/type function-form
                          :border-pane/margin 10
                          }
                  :center {:fx/type maze-pic-pane
                           :border-pane/margin 10
                           }
                  :bottom {:fx/type tower-gen-pane
                           :border-pane/margin 10}
                  }}})
