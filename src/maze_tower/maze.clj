(ns maze-tower.maze
  (:require [maze-tower.maze-algo :as maze-algo]
            [maze-tower.grid :as grid]
            [maze-tower.image :as image]
            [me.raynes.fs :as fs]
            [clojure.java.io :as io]
            [maze-tower.util :as util]
            [clojure.string :as str])
  (:import [javax.imageio ImageIO]))

(defn run-and-render [algorithm grid-size render-fn]
  (render-fn (algorithm (apply grid/make-grid grid-size))))

(defn write-grid-to-image-file [grid ^String filename cell-size]
  (let [image (image/image-grid grid cell-size)
        format (-> (fs/extension filename)
                   (subs 1))]
    (ImageIO/write image format (io/file filename))))

(defn direction
  [[row col] [prev-row prev-col]]
  {:pre [(<= (Math/abs (- row prev-row)) 1)
         (<= (Math/abs (- col prev-col)) 1)]}
  (case [(- row prev-row) (- col prev-col)]
    [1 0] :down
    [-1 0] :up
    [0 1] :right
    [0 -1] :left
    :unknown))

(def default-direction-chars [\↑ \↓ \← \→])
(def route-value (zipmap [:up :down :left :right] default-direction-chars))

(defn get-route
  "获取迷宫的路径"
  [grid]
  (let [steps (sort-by second (filter #(vector? (first %1)) (:analysis grid)))
        pos-steps (map first steps)]
    (->> (second (reduce (fn [[prev-pos r] pos]
                           [pos (conj r (direction pos prev-pos))])
                         [(first pos-steps) []]
                         (rest pos-steps)))
         (map route-value)
         (apply str))))

(defn rand-point
  [max-x max-y]
  [(rand-int max-x) (rand-int max-y)])

(defn rand-2-point
  "随机产生两个不同的点"
  [max-x max-y]
  (let [p1 (rand-point max-x max-y)
        p2 (rand-point max-x max-y)]
    (if (= p1 p2)
      (recur max-x max-y)
      [p1 p2])))

(defn gen-maze
  [{:keys [algo rows cols start-mark end-mark]}]
  (let [rows (util/range-value rows)
        cols (util/range-value cols)
        [start end] (rand-2-point rows cols)
        algo (maze-algo/algorithm-fn algo {:distances start
                                           :path-to end
                                           :start-mark start-mark
                                           :end-mark end-mark})]

    (-> (grid/make-grid rows cols)
        algo)))

(defn gen-in-path-len-maze
  "生成寻路路径长度在path-len指定范围的迷宫"
  [{:keys [path-len]
    :or {path-len [50 200]} :as maze-conf}]
  (let [m (gen-maze maze-conf)
        path (get-route m)]
    (if (util/in-range? (count path) path-len)
      [m path]
      (recur maze-conf))))

(defn gen-maze-and-save
  "生成一个迷宫，并保存迷宫图片，返回迷宫图片和寻路路径"
  [{:keys [cell-size out-path] :as maze-conf}]
  (let [[m path] (gen-in-path-len-maze maze-conf)
        cell-size (util/range-value cell-size)]
    (write-grid-to-image-file m out-path cell-size)
    {:image-path out-path
     :route path}))

(defn gen-mazes
  ":output-start-index 为输出图片文件的开始索引编号"
  [{:keys [count output-dir output-start-index] :as maze-conf}]
  (fs/mkdirs output-dir)
  (map (fn [idx]
         (let [out-path (-> (fs/file output-dir (str "maze_" idx ".jpg"))
                             str)]
           (gen-maze-and-save (merge maze-conf
                                     {:out-path out-path}))))
       (range output-start-index
              (+ output-start-index count))))

(defn gen-tower
  "生成迷宫塔,返回解压密码序列
  :output-file 指定输出文件名
  :first-file 最顶层的文件
  :maze-infos 使用的迷宫信息
  :level 层数
  :direction-chars 四个方向对应的字符[上 下 左 右],如果为空，则使用默认方向字符
  :sort 是否按文件大小排序"
  [{:keys [output-file first-file maze-infos level direction-chars sort]}]
  (let [mazes (->> (shuffle maze-infos)
                   (take level))
        mazes (if sort
                (-> (sort-by (comp fs/size :image-path) mazes)
                    reverse)
                mazes)
        ;; tmp-zip (str output-file ".zip")
        pass-trans (zipmap default-direction-chars direction-chars)
        trans-pass (fn [route]
                     (if pass-trans
                       (str/escape route pass-trans)
                       route))]
    (reduce (fn [prev-file {:keys [route image-path]}]
              (let [password (trans-pass route)
                    tmp-zip (str "maze_" output-file ".zip")]
                (prn "zip:" prev-file "pass:" password)
                (util/zip-file! tmp-zip [prev-file] password)
                (util/join-files! output-file image-path tmp-zip)
                (fs/delete tmp-zip)
                (Thread/sleep 200.)
                output-file))
            first-file
            mazes)
    (->> (reverse mazes)
         (map (comp trans-pass :route)))))

(defn test-tower
  "测试解压是否正确"
  [tower-file pwds result-file]
  (let [unzip-path (str tower-file "_unzip")]
    (reduce (fn [zip-path password]
              (util/unzip-cmd! zip-path unzip-path password)
              (str (fs/file unzip-path tower-file)))
            tower-file
            pwds)
    (-> (fs/file unzip-path result-file)
        str
        (util/file-content-equal? result-file))))

(comment

  (def ms (gen-mazes {:count 15
                      :output-dir "maze_images"
                      :output-start-index 10
                      :cell-size [10 30]
                      :algo (first (keys maze-algo/algorithm-functions))
                      :rows [20 100]
                      :cols 30
                      :start-mark "./resources/start.png"
                      :end-mark "./resources/end.png"}))

  (def ts (gen-tower {:output-file "flag.jpg"
                      :first-file "project.clj"
                      :maze-infos ms
                      :level 6
                      :direction-chars "1234"
                      :sort true}))

  (test-tower "flag.jpg" ts "project.clj")

  )
