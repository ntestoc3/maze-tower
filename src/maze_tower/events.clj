(ns maze-tower.events
  (:require [cljfx.api :as fx]
            [maze-tower.config :as config]
            [maze-tower.subs :as subs]
            [clojure.java.io :as io]
            [maze-tower.util :as util])
  (:import javafx.stage.FileChooser
           javafx.stage.FileChooser$ExtensionFilter
           javafx.stage.DirectoryChooser
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
  "选择文件对话框
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

(defmethod event-handler ::value-changed [{:keys [fx/context fx/event key]}]
  (config/add-config! key event)
  {:context (fx/swap-context context assoc key event)})

(defmethod event-handler ::pic-index-change [{:keys [fx/context fx/event key]}]
  (let [total-pics (fx/sub context subs/pics-count)
        new-idx (util/in-range event 0 (dec total-pics))]
    (prn "new-idx:" new-idx)
    (config/add-config! key new-idx)
    {:context (fx/swap-context context assoc key new-idx)}))

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

