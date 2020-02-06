(require '[cljfx.composite :as composite]
         '[cljfx.lifecycle :as lifecycle]
         '[cljfx.coerce :as coerce]
         '[cljfx.api :as fx]
         '[cljfx.prop :as prop]
         '[cljfx.mutator :as mutator])

(def with-scroll-text-prop
  (lifecycle/make-ext-with-props
   lifecycle/dynamic
   {:scroll-text (prop/make
                  (mutator/setter (fn [text-area [txt auto-scroll]]
                                    (let [scroll-pos (if auto-scroll
                                                       ##Inf
                                                       (.getScrollTop text-area))]
                                      (prn "scroll pos:" scroll-pos)
                                      (doto text-area
                                        (.setText txt)
                                        (.setScrollTop scroll-pos)))))
                  lifecycle/scalar
                  :default ["" 0])

    }))

(def *state
  (atom (fx/create-context
          {:logs ""
           :auto-scroll true
           :lbl-value "test log in event handler!"})))

(defn log-form [{:keys [fx/context]}]
  (let [auto-scroll (fx/sub context :auto-scroll)]
    {:fx/type with-scroll-text-prop
     :props {:scroll-text [(fx/sub context :logs)
                           auto-scroll]}
     :desc {:fx/type :text-area
            :editable false
            :context-menu {:fx/type :context-menu
                           :items [{:fx/type :check-menu-item
                                    :text "Auto Scroll"
                                    :selected auto-scroll
                                    :on-action {:event/type :event/auto-scroll-change
                                                :value (not auto-scroll)}}]}
            }}))

;;;;;;;; event-handler
(defmulti event-handler :event/type)

(defmethod event-handler :default [event]
  (prn event))

(defmethod event-handler :event/value-change [{:keys [fx/context fx/event key]}]
  {:context (fx/swap-context context assoc key event)})

(defmethod event-handler :event/auto-scroll-change [{:keys [fx/context value]}]
  {:context (fx/swap-context context assoc :auto-scroll value)})

(defmethod event-handler :event/add-log [{:keys [fx/context log]}]
  (let [old-log (fx/sub context :logs)]
    {:context (fx/swap-context context assoc :logs (str old-log log))}))

(def real-event-handler
  (-> event-handler
      (fx/wrap-co-effects
       {:fx/context (fx/make-deref-co-effect *state)})
      (fx/wrap-effects
       {:context (fx/make-reset-effect *state)})))

;;;;;;;;;;;;;;;; logging, Use functions instead of log appender
(defn log
  [s]
  (real-event-handler {:event/type :event/add-log
                       :fx/sync true
                       :log (str s "\n")}))

(defmethod event-handler :event/button-click [{:keys [fx/context]}]
  ;;** Question 2
  (log "button click event-handler!") ;; !!! This will not work
  ;;  because the context modification below overwrites the log modified context here
  {:context (fx/swap-context context assoc :lbl-value "button clicked!")})

(defn test-event-form [{:keys [fx/context]}]
  {:fx/type :h-box
   :children [{:fx/type :button
               :text "test"
               :on-action {:event/type :event/button-click}}
              {:fx/type :label
               :text (fx/sub context :lbl-value)}]})

;;;;;;;;;;;;;;; renderer
(def renderer
  (fx/create-renderer
    :middleware (comp
                  fx/wrap-context-desc
                  (fx/wrap-map-desc (fn [_]
                                      {:fx/type :stage
                                       :showing true
                                       :scene {:fx/type :scene
                                               :root {:fx/type :v-box
                                                      :children [{:fx/type test-event-form}
                                                                 {:fx/type log-form}]}}})))
    :opts {:fx.opt/map-event-handler real-event-handler
           :fx.opt/type->lifecycle #(or (fx/keyword->lifecycle %)
                                        (fx/fn->lifecycle-with-context %))}))

(fx/mount-renderer *state renderer)

;;; test logging
(doseq [i (range 20)]
  (log (str "log:" i)))
