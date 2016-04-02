(ns ^:skip-aot net.reborg.scccw.system
  (:gen-class)
  (:require [org.httpkit.server :refer [run-server]]
            [net.reborg.scccw]
            [net.reborg.scccw.bootstrap]
            [clojure.tools.nrepl.server :as nrepl]
            [net.reborg.scccw.config :as c]
            [clojure.tools.logging :as log]
            [com.stuartsierra.component :as component]))

(defn- start-server [handler port] (let [server (run-server handler {:port port})] server))
(defn- stop-server [server] (when server (server)))

(defn- start-nrepl-server [port] (let [server (nrepl/start-server :port port :bind "0.0.0.0")] server))
(defn- stop-nrepl-server [server] (when server (nrepl/stop-server server)))

(defrecord ScccwServer []
  component/Lifecycle
  (start [this]
    (let [init (-> this
                   (assoc :server (start-server #'net.reborg.scccw/app (c/scccw-port)))
                   (assoc :nrepl-server (start-nrepl-server (c/nrepl-port)))
                   )]
      (log/info (format "started with %s" (c/debug)))
      init))
  (stop [this]
    (stop-server (:server this))
    (stop-nrepl-server (:nrepl-server this))
    (-> this
        (dissoc :server)
        (dissoc :nrepl-server))))

(defn create-system []
  (ScccwServer.))

(defn -main [& args]
  (alter-var-root
    #'net.reborg.scccw.bootstrap/system
    (fn [_] (.start (create-system)))))
