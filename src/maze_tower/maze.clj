(ns maze-tower.maze
  (:require [maze-tower.maze-algo :as maze-algo]
            [maze-tower.grid :as grid]
            [maze-tower.image :as image]
            [me.raynes.fs :as fs]
            [clojure.java.io :as io])
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
  [width height]
  (let [[start end] (rand-2-point width height)
        algo (maze-algo/algorithm-fn "recursive-backtracker" {:distances start
                                                              :path-to end
                                                              })]

    (-> (grid/make-grid width height)
        algo)))

(defn gen-in-range-path-maze
  [min-path max-path]
  (let [m (gen-maze (+ 10 (rand-int 90))
                    (+ 10 (rand-int 90)))
        path (get-route m)]
    (if (<= min-path (count path) max-path)
      [m path]
      (recur min-path max-path))))

(fs/delete-dir "maze_imgs/")
(fs/mkdir "maze_imgs/")
(fs/delete "maze.txt")

(doseq [idx (range 0 10)]
  (let [[m path] (gen-in-range-path-maze 50 200)
        fname (str "maze_imgs/maze_" idx ".jpg")]
    (spit "maze.txt" (str fname " " path "\n") :append true)
    (write-grid-to-image-file m fname (+ 20 (rand-int 80)))
    ))

;; (def m2 (gen-maze 50 50))
;; (write-grid-to-image-file m2 "./maze/test.jpg" (+ 20 (rand-int 80)))

;; (doseq [d ds1]
;;   (println d))

;; (doseq [[pos step] steps]
;;   (println step ":" pos ))

;; (def m3 (last (recursive-backtracker-seq m1)))
;; (def d1 (last (distances-seq m3 [0 0])))
;; (def p1 (last (path-seq m3 d1 [9 9])))
;; (write-grid-to-image-file m3 "test.jpg" 8)
