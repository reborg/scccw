(ns user
  (:require [clojure.test]
            [net.reborg.scccw.system]
            [net.reborg.scccw.bootstrap :as b :refer [system stop start]]))

(b/set-init! #'net.reborg.scccw.system/create-system)
(defn reset [] (binding [clojure.test/*load-tests* false] (b/reset)))
