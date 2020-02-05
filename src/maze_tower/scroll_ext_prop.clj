(ns maze-tower.scroll-ext-prop
  (:require [cljfx.composite :as composite]
            [cljfx.lifecycle :as lifecycle]
            [cljfx.coerce :as coerce]
            [cljfx.prop :as prop]
            [cljfx.mutator :as mutator]))

(def with-scroll-prop
  (lifecycle/make-ext-with-props
   lifecycle/dynamic
   {:on-scroll-left-changed (prop/make
                             (mutator/property-change-listener #(.scrollLeftProperty %))
                             lifecycle/change-listener)
    :on-scroll-top-changed (prop/make
                            (mutator/property-change-listener #(.scrollTopProperty %))
                            lifecycle/change-listener)
    }))
