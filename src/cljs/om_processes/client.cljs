(ns om-processes.client
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :as async
             :refer [<! >! chan put! timeout]]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [goog.dom :as gdom]))

(enable-console-print!)

;; The big-table state is represented as a vector of vectors
;; where each value is an integer
;; (quot value 10) is the group number for the color style
;; (mod value 10) is digit to display

(def app-state (atom {:processes {:big-table nil
                                  :render-rate 0}}))

(def group (atom 0)) ;; color group (see inline style in index.html)

;; the following is for optional performance instrumation (not
;; part of the application state)
(def stats (atom {:last-time nil :cells 0}))
(def cells-per-sample 10000)

(defn render! [processes width height queue]
  (doseq [[idx v] queue]
    (let [row (quot idx width)
          column (mod idx width)]
      ;; optional performance counting below
      (swap! stats
        (fn [s]
          (let [{:keys [last-time cells]} s]
            (if (or (nil? last-time) (zero? (mod cells cells-per-sample)))
              (let [now (.now js/Date)]
                (if last-time
                  (let [elapsed (- now last-time) ;; in ms
                        render-rate (quot (* 1000 cells-per-sample) elapsed)]
                    (om/update! processes [:render-rate] render-rate)))
                {:last-time now :cells (inc cells)})
              {:last-time last-time :cells (inc cells)}))))
      ;; ---------------------------------------
      (om/transact! processes [:big-table row column]
        (fn [_] (+ (* 10 @group) v)))))
  (swap! group (fn [g] (mod (inc g) 5))))

;; Om component to show big-table
(defn processes-view [processes owner]
  (reify
    om/IInitState
    (init-state [_]
      (let [width 100
            height 100
            rate 40
            render-size 1000]
        {:width width :height height :rate rate :render-size render-size}))
    om/IWillMount
    (will-mount [_]
      (let [{:keys [width height rate render-size]} (om/get-state owner)
            big-table (vec (repeat height
                             (vec (repeat width 0))))
            table-size (* width height)
            render (chan render-size)]
        (om/update! processes [:big-table] big-table)
        (loop [i 0]
          (when (< i table-size)
            (go (while true
                  (<! (timeout (+ 1000 (rand-int table-size))))
                  (>! render [(rand-int table-size) ;; random cell
                          (rand-int 10)]))) ;; random digit
            (recur (inc i))))
        (go (loop [refresh (timeout rate) queue []]
              (let [[v c] (alts! [refresh render])]
                (condp = c
                  refresh (do (render! processes width height queue)
                              ;; (<! (timeout 0)) ;; unnecessary?
                              (recur (timeout rate) []))
                  render (recur refresh (conj queue v))))))
        ))
    om/IRenderState
    (render-state [_ state]
      (let [{:keys [width height]} state
            {:keys [big-table render-rate]} (om/value processes)]
        (dom/div #js {:id "processes-view"}
          (dom/span nil (str "render-rate: " render-rate " cells/second"))
          (apply dom/table #js {:id "big-table"}
            (map
              (fn [row]
                (apply dom/tr nil
                  (map
                    (fn [column]
                      (let [group (quot column 10) ;; color group
                            digit (mod column 10)]
                        (dom/td #js {:className (str "group" group)}
                          digit)))
                    row)))
              big-table)))))))

(defn app-view [app owner]
  (reify
    om/IRender
    (render [_]
      (let [{:keys [processes]} app] ;; processes is a cursor
        (dom/div #js {:id "app-view"}
          (dom/h1 nil "om-processes")
          (om/build processes-view processes))))))

(om/root app-view app-state {:target (gdom/getElement "app")})
