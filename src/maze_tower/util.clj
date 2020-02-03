(ns maze-tower.util
  (:require [clojure.java.io :as io]
            [me.raynes.fs :as fs]
            [clojure.java.shell :refer [sh]]
            [clojure.string :as str])
  (:import net.lingala.zip4j.ZipFile
           net.lingala.zip4j.model.ZipParameters
           net.lingala.zip4j.model.enums.EncryptionMethod
           java.awt.Desktop
           ))

(defn in-range-int
  "数字n在min-max范围内，如果超过则取相应的最大最小值"
  [n min max]
  (cond
    (<= n min) min
    (>= n max) max
    :else n))

(defn file-open
  "读取文件，如果找不到文件则读取资源文件"
  [file-path]
  (some-> (if (fs/file? file-path)
            (fs/file file-path)
            (io/resource file-path))))

(defn file-dir
  "获取文件的目录,如果是目录则返回目录本身"
  [file-path]
  (if (fs/directory? file-path)
    file-path
    (str (fs/parent file-path))))

(defn file-istream
  "获取文件的输入流"
  [file-path]
  (some-> (file-open file-path)
          io/input-stream))

(defn range?
  [range-v]
  (or (number? range-v)
      (set? range-v)
      (and (seqable? range-v)
           (= 2 (count range-v))
           (>= (second range-v)
               (first range-v)))))

(defn range-value
  "获取一个范围数字
  如果是数字n,则为固定的n
  如果是[x y] 则为x-y之间的随机数,包含x和y
  如果是#{a b ...} 集合，则为任意一个集合中的数字"
  [range-v]
  {:pre [(range? range-v)]}
  (cond
    (set? range-v)
    (rand-nth (vec range-v))

    (seqable? range-v)
    (let [[x y] range-v]
      (+ x
         (rand-int (inc (- y x)))))

    :else range-v))

(defn in-range?
  "数字i是否在范围值range-v中 "
  [i range-v]
  {:pre [(int? i)
         (range? range-v)]}
  (cond
    (set? range-v)
    (range-v i)

    (seqable? range-v)
    (let [[x y] range-v]
      (<= x i y))

    :else (= i range-v)))

(defn parse-range
  "解析范围数字,可以是单个数字，或者是x-y之间的范围,或者是a,b,c数字集合"
  [s]
  (let [s (str/trim s)]
    (if-let [grps (re-matches #"\s*(\d+)\s*-\s*(\d+)\s*" s)]
      [(Integer/parseInt (second grps))
       (Integer/parseInt (last grps))]
      (let [grps (str/split s #"\s*,\s*")]
        (if (> (count grps) 1)
          (-> (map #(Integer/parseInt %) grps)
              set)
          (-> (first grps)
              (Integer/parseInt)))))))

(defn range->str
  "范围值转换为字符串表示"
  [range-v]
  {:pre [(range? range-v)]}
  (cond
    (set? range-v)
    (str/join "," range-v)

    (seqable? range-v)
    (str/join "-" range-v)

    :else (str range-v)))

(defn desktop-open
  "打开文件浏览器"
  [path]
  (let [f (io/file path)
        path (if (.isFile f)
               (.getParent f)
               f)]
    (when (Desktop/isDesktopSupported)
      (-> (Desktop/getDesktop)
          (.open (io/file path))))))

(defn zip-file
  "创建压缩文件
  `out-fname` output zip file name.
  `files` files add to zip file.
  `password` optional, zip password."
  ([out-fname files] (zip-file out-fname files nil))
  ([out-fname files password]
   (let [params (doto (ZipParameters.)
                  (.setEncryptFiles (if password
                                      true
                                      false))
                  (.setEncryptionMethod EncryptionMethod/AES))
         files (map io/file files)]
     (-> (ZipFile. out-fname (char-array password))
         (.addFiles files params)))))

(defn join-files
  "连接多个文件到out-file"
  [out-file & files]
  (with-open [w (io/writer out-file)]
    (doseq [f files]
      (io/copy (io/file f) w))))

(comment
  (zip-file "test.zip" ["maze.txt"] "123456")
  (zip-file "test.zip" ["maze.txt"])

  (join-files "test.aaa.txt" "maze.txt" "project.clj" "maze.txt")


  (range->str 8)

  (range->str [2 6])

  (range->str #{1 3 65 8})

  (in-range? 3 #{1 3 8 5})

  (in-range? 10 [1 10])

  (in-range? 2 1)

  )
