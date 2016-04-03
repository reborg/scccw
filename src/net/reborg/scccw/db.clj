(ns net.reborg.scccw.db
  (:require [net.reborg.scccw.bootstrap :refer [system]]
            [clojure.tools.logging :as log]))

(defn start []
  (try
    (str "conn")
    (catch Throwable t
      (log/error "Unable to connect to database:" (.getMessage t)))))

(defn stop [conn]
  (try
    (when conn "close conn.")
    (catch Throwable t
      (log/error "Unable to stop the connection pool:" (.getMessage t)))))

(defn get-all-accounts []
  (let [conn (:db system)]
    (str "got all accounts from " conn)))
