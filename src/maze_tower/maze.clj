(ns maze-tower.maze
  (:require [maze-tower.maze-algo :as maze-algo]
            [maze-tower.grid :as grid]
            [maze-tower.image :as image]
            [me.raynes.fs :as fs]
            [clojure.java.io :as io]
            [maze-tower.util :as util])
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

(def route-value {:up 1 :down 2 :left 3 :right 4})

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

(comment

  (gen-mazes {:count 5
              :output-dir "maze_images"
              :output-start-index 10
              :cell-size [10 30]
              :algo (first (keys maze-algo/algorithm-functions))
              :rows [20 100]
              :cols 30
              :start-mark "./resources/start.png"
              :end-mark "./resources/end.png"})


  )
