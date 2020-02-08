(ns maze-tower.config
  (:require [cprop.core :refer [load-config]]
            [cprop.source :as source]
            [maze-tower.util :as util]))

(def default-config "config.edn")

(util/extract-resource! default-config)

(defn read-curr-config
  "读取当前配置文件内容"
  []
  (source/ignore-missing-default (fn []
                                   (source/from-file default-config))
                                 nil))

(def env
  (load-config
   :file default-config
   :merge
   [(source/from-system-props)
    (source/from-env)]))


(def config (atom {}))

(defn- some-first
  [& exps]
  (if-some [r (first exps)]
    r
    (when (seq? exps)
      (recur (next exps)))))

(defn get-config
  "从全局配置和环境中 获取配置项k的值"
  ([k] (get-config k nil))
  ([k default] (some-first
                (get @config k)
                (get env k)
                default)))

(defn get-in-config
  "从全局配置和环境中 获取配置path的值"
  [path]
  (some-first
   (get-in @config path)
   (get-in env path)))

(defn add-config!
  "添加配置项到全局配置"
  [& kvs]
  (apply swap! config assoc kvs))

(defn add-in-config!
  "添加配置项到全局配置"
  [path v]
  (swap! config assoc-in path v))

(defn save-config!
  ([] (save-config! (or (:conf env) default-config)))
  ([file-name]
   (spit file-name (-> (merge (read-curr-config)
                              @config)
                       pr-str))))
