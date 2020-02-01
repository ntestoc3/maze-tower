(defproject maze-tower "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [me.raynes/fs "1.4.6"] ;; file util
                 [cljfx "1.6.2"] ;; javafx gui
                 [net.lingala.zip4j/zip4j "2.3.1"] ;; zip with password
                 [com.taoensso/timbre "4.10.0"] ; logging
                 [cprop/cprop "0.1.15"] ;; env manage
                 ]
  :main ^:skip-aot maze-tower.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all
                       :injections [(javafx.application.Platform/exit)]}})
