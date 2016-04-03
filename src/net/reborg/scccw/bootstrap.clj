(ns net.reborg.scccw.bootstrap
  (:require [clojure.tools.namespace.repl :refer [disable-reload! refresh]]
            [clojure.tools.logging :as log]))

;; prevents system var to disappear on reload
(disable-reload!)

;; the global state lives here
(def system nil)

(defprotocol Lifecycle
  (start [component])
  (stop [component]))

(def ^:private initializer nil)

(defn set-init! [init] (alter-var-root #'initializer (constantly init)))
(defn- stop-system [s] (when s (stop s)))

(defn init []
  (if-let [init initializer]
    (do (alter-var-root #'system #(do (stop-system %) (init))) :ok)
    (throw (Error. "No system initializer function found."))))

(defn start! [] (alter-var-root #'system start) :started)
(defn stop! [] (alter-var-root #'system stop-system) :stopped)
(defn go! [] (init) (start!))
(defn clear! [] (alter-var-root #'system #(do (stop-system %) nil)) :ok)
(defn reset [] (clear!) (refresh :after 'net.reborg.scccw.bootstrap/go!))
