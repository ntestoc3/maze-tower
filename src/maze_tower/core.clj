(ns maze-tower.core
  (:require [maze-tower.gui :as gui]
            [maze-tower.util :as util]
            [maze-tower.config :as config])
  (:gen-class))


(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (util/log-to-file!)
  (gui/show))
