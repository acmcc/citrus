(ns counter.core
  (:require-macros [citrus.core :as citrus])
  (:require [rum.core :as rum]
            [citrus.core :as citrus]
            [goog.dom :as dom]))

;;
;; define controller & event handlers
;;

(def initial-state 0)

(defmulti control (fn [event] event))

(defmethod control :init []
  {:state initial-state})

(citrus/defhandler control :load
  {:cofx [[:local-storage :count]]}
  [_ [key] _ coeffects]
  {:state (-> coeffects :local-storage int)})

(defmethod control :save [_ [key] counter]
  {:local-storage {:op    :set
                   :value counter
                   :key   key}})

(defmethod control :inc [_ _ counter]
  (let [next-counter (inc counter)]
    {:state         next-counter
     :local-storage {:op    :set
                     :value next-counter
                     :key   :count}}))

(defmethod control :dec [_ _ counter]
  (let [next-counter (dec counter)]
    {:state         next-counter
     :local-storage {:op    :set
                     :value next-counter
                     :key   :count}}))


;;
;; define UI component
;;

(rum/defc Counter < rum/reactive [r]
  [:div
   [:button {:on-click #(citrus/dispatch! r :counter :dec)} "-"]
   [:span (rum/react (citrus/subscription r [:counter]))]
   [:button {:on-click #(citrus/dispatch! r :counter :inc)} "+"]])


;;
;; define effects handler
;;

(defn local-storage [r c {:keys [op key value on-success]}]
  (case op
    :get
    (->> (name key)
         js/localStorage.getItem
         (citrus/dispatch! r c on-success))
    :set
    (-> (name key)
        (js/localStorage.setItem value))))


;;
;; start up
;;

;; create Reconciler instance
(defonce reconciler
         (citrus/reconciler
           {:state           (atom {})
            :controllers     {:counter control}
            :effect-handlers {:local-storage local-storage}
            :co-effects      {:local-storage #(js/localStorage.getItem (name %))}}))

;; initialize controllers
(defonce init-ctrl (citrus/broadcast-sync! reconciler :init))

;; load from localStorage
(defonce load (citrus/dispatch-sync! reconciler :counter :load :count))

;; render
(rum/mount (Counter reconciler)
           (dom/getElement "app"))
