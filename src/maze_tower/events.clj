(ns maze-tower.events
  (:require [cljfx.api :as fx]
            [maze-tower.config :as config]
            [maze-tower.maze :as maze]
            [maze-tower.subs :as subs]
            [clojure.java.io :as io]
            [maze-tower.util :as util]
            [clojure.set :as set]
            [me.raynes.fs :as fs])
  (:import javafx.stage.FileChooser
           javafx.stage.FileChooser$ExtensionFilter
           javafx.stage.DirectoryChooser
           javafx.scene.control.ButtonType
           javafx.stage.Stage))

(defn choose-dir
  "选择文件夹对话框"
  ([] (choose-dir nil))
  ([{:keys [title init-dir]
     :or {title "选择文件夹"
          init-dir "."}}]
   @(fx/on-fx-thread
     (-> (doto (DirectoryChooser.)
           (.setTitle title)
           (.setInitialDirectory (io/file init-dir)))
         (.showDialog (Stage.))))))

(defn make-ext-filters
  "构造扩展名过滤器列表
  `ext-filters` 格式[[\"All Files\" [\"*.*\"]] [\"JPG\" [\"*.jpeg\" \"*.jpg\"]] ...]"
  [ext-filters]
  (map (fn [[name ext]]
         (FileChooser$ExtensionFilter. name ext))
       ext-filters))

(defn choose-file
  "选择文件对话框,选择成功则返回java.io.File对象
  `chooser-type` 对话框类型 [:open :open-multiple :save]之一
  `opts` 可选参数: :title 对话框标题
                 :init-dir 初始文件夹
                 :filters 扩展过滤器"
  ([] (choose-file :open nil))
  ([chooser-type] (choose-file chooser-type nil))
  ([chooser-type {:keys [title init-dir filters]
                  :or {title "选择文件"
                       init-dir "."}}]
   @(fx/on-fx-thread
     (let [fc (doto (FileChooser.)
                (.setTitle title)
                (.setInitialDirectory (io/file init-dir)))
           win (Stage.)]
       (when filters
         (-> (.getExtensionFilters fc)
             (.addAll (make-ext-filters filters))))
       (case chooser-type
         :open
         (.showOpenDialog fc win)

         :open-multiple
         (.showOpenMultipleDialog fc win)

         :save
         (.showSaveDialog fc win))))))

(defn alert-box
  ([msg] (alert-box msg nil))
  ([msg {:keys [button-types]
         :or {button-types [:yes]}}]
   (-> (fx/instance
        (fx/create-component
         {:fx/type :alert
          :alert-type :warning
          ;; :showing true
          :header-text "警告"
          :content-text msg
          :button-types button-types
          }))
       (.showAndWait)
       (as-> $
           (when (.isPresent $)
             (.get $))))))

(comment
  (choose-file :open {:title "选择文件"
                      :filters [["所有文件" ["*.*"]]
                                ["jpg" ["*.jpeg" "*.jpg"]]
                                ["png" ["*.png"]]]})

  (choose-file)

  (choose-dir)

  (choose-file :open-multiple {:title "打开多个文件"
                               :filters [["所有文件" ["*.*"]]
                                         ["jpg" ["*.jpeg" "*.jpg"]]
                                         ["png" ["*.png"]]]})

  (choose-file :save {:title "保存文件"})

  )

(defmulti event-handler :event/type)

(defmethod event-handler :default [event]
  (prn event))

(defn change-context
  [context & kvs]
  (apply config/add-config! kvs)
  {:context (apply fx/swap-context context assoc kvs)})

(defmethod event-handler ::value-changed [{:keys [fx/context fx/event key]}]
  (change-context context key event))

(defmethod event-handler ::pic-index-change [{:keys [fx/context fx/event key]}]
  (let [total-pics (fx/sub context subs/pics-count)
        new-idx (util/in-range-int event 0 (dec total-pics))]
    (prn "new-idx:" new-idx)
    (change-context context key new-idx)))

(defmethod event-handler ::prev-maze-pic [{:keys [fx/context fx/event]}]
  (let [idx (-> (fx/sub context :curr-pic-index)
                dec)]
    {:dispatch {:event/type ::pic-index-change
                :fx/event idx
                :key :curr-pic-index}}))

(defmethod event-handler ::next-maze-pic [{:keys [fx/context fx/event]}]
  (let [idx (-> (fx/sub context :curr-pic-index)
                inc)]
    {:dispatch {:event/type ::pic-index-change
                :fx/event idx
                :key :curr-pic-index}}))

(defmethod event-handler ::stop [{:keys [fx/context fx/event]}]
  (config/save-config!))

(defmethod event-handler ::gen-maze [{:keys [fx/context fx/event]}]
  (let [old-mazes (fx/sub context :maze-pic-infos)
        mazes (concat old-mazes
                      (maze/gen-mazes {:count (fx/sub context :maze-gen-num)
                                       :output-dir (fx/sub context :maze-pics-dir)
                                       :cell-size (fx/sub context :maze-cell-size)
                                       :path-len (fx/sub context :maze-path-len)
                                       :algo (fx/sub context :maze-algo)
                                       :rows (fx/sub context :maze-rows)
                                       :cols (fx/sub context :maze-cols)
                                       :output-start-index (count old-mazes)
                                       :start-mark (fx/sub context :maze-start-pic)
                                       :end-mark (fx/sub context :maze-end-pic)}))]
    (change-context context :maze-pic-infos mazes)))

(defmethod event-handler ::del-all-maze-pic [{:keys [fx/context fx/event]}]
  (when (= ButtonType/YES (alert-box "确定删除所有生成的迷宫？" {:button-types [:yes :no]}))
    (let [pics (fx/sub context :maze-pic-infos)
          all-pic-files (map :image-path pics)]
      (doseq [f all-pic-files]
        (fs/delete f))
      (change-context context
                      :maze-pic-infos []
                      :curr-pic-index 0))))

(def image-filter [["所有文件" ["*.*"]]
                   ["jpg" ["*.jpeg" "*.jpg"]]
                   ["png" ["*.png"]]])

(defmethod event-handler ::change-start-mark [{:keys [fx/context fx/event]}]
  (when-let [new-file (choose-file :open {:title "选择开始标记的图片文件"
                                          :filter image-filter})]
    (change-context context :maze-start-pic (str new-file))))

(defmethod event-handler ::change-end-mark [{:keys [fx/context fx/event]}]
  (when-let [new-file (choose-file :open {:title "选择结束标记的图片文件"
                                          :filter image-filter})]
    (change-context context :maze-end-pic (str new-file))))

(defmethod event-handler ::change-pic-dir [{:keys [fx/context fx/event]}]
  (when-let [new-dir (choose-dir {:title "选择图片保存文件夹"
                                  :init-dir (fx/sub context :maze-pics-dir)})]
    (change-context context :maze-pics-dir (str new-dir))))
