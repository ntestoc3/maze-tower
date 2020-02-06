(defproject maze-tower "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [me.raynes/fs "1.4.6"] ;; file util
                 [cljfx "1.6.3" :exclusions [[org.openjfx/javafx-media]
                                             [ org.openjfx/javafx-web]]] ;; javafx gui
                 [org.clojure/core.cache "0.8.2"]  ;; cache
                 [net.lingala.zip4j/zip4j "2.3.1"] ;; zip with password
                 [com.taoensso/timbre "4.10.0"]    ; logging
                 [clojure.java-time "0.3.2"]       ; datetime
                 [cprop/cprop "0.1.15"]            ;; env manage

                 ;; 添加JavaFX的windows系统支持, mac如果要支持也需要添加依赖
                 [org.openjfx/javafx-graphics "13" :classifier "win"]
                 [org.openjfx/javafx-graphics "13" :classifier "mac"]
                 ]
  :omit-source true
  :main ^:skip-aot maze-tower.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all
                       :injections [(javafx.application.Platform/exit)]}})
