# components.md

components.md is an extremely simple, convention-based, Clojure project organization.

It's not a framework by design, because I don't believe in components reuse/sharing through libraries. components.md is instead designed around the idea that each project starts from a copy-pasting of a few necessary things to get started, including components already implemented if necessary. When talking about components in this document, I'm referring to the stateful parts of a Clojure application, not code reuse in general. I do like code reuse in general :)

The principles/conventions/definitions:

* A namespace is stateful if some information needs to survive its reload. If after reloading a namespace you assume that an open connection is still open, then that is a stateful part. "defonce" are good indicators.
* components.md does not enforce a code contract on components. This is the main difference from other components frameworks that force a defrecord start/stop lifecycle. I use conventions instead.
* Every part of the code is free to access the global state. No "injections" of other components or declared dependencies.
* we only want a component when some stateful interaction is involved. Most of the times the stateful "object" is not even part of the project but comes from dependencies (connections, thread pools, sockets, streams and so on). This stateful part (and only this) is what ends up in the global "def". No component should be created if there is nothing stateful about it.
* components.md is a developing tool only. It's here because when we start a REPL, we don't want to setup connections manually, or remember to clear a few in-memory caches. When switching to prod we bypass this whole setup.

## install

components.md is not a framework nor a library, it is actually this document itself! Copy paste the parts below and change at will. Alternatively, clone this project to see a running example.

## recipe

* 1 org.clojure/tools.namespace dependency in project.clj
* 3 conventional namespaces (bootstrap, system and user)
* 1 namespace each component

Let's get started:

### 1: bootstrap

The bootstrap namespace contains the only reference to the global system variable. Other namespaces simply require bootstrap and access system to fetch any stateful objects there (db, sockets, connections, etc) Boostrap also contains functions to handle the global state, like (reset).

```clojure
(ns bootstrap
  (:require [clojure.tools.namespace.repl :refer [disable-reload! refresh]]))

(disable-reload!)
(def system nil)
(def ^:private initializer nil)

(defprotocol Lifecycle
  (start [component])
  (stop [component]))

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
```

### 2: system

The system namespace contains the implementation of the lifecycle functions. It contains the logic to retrieve the stateful part (usually Java objects like connections, pools, sockets and so on) from each component. It then stores the actual stateful object in the main system def (in bootstrap). It does so by calling the start/stop function on each component. So system will likely have all components in the require. System also contains the main function when the application is not running at the REPL. The final output that goes into the bootstrap/system var is a simple map. If components have bootstrap dependencies, the start function here is the right place to handle them.

```clojure
(ns ^:skip-aot system
  (:gen-class)
  (:require [bootstrap]
            [database]
            [webserver]))

(defrecord MyApp []
  bootstrap/Lifecycle
  (start [this]
    (let [init (-> this
                   (assoc :db (db/start))
                   (assoc :webserver (webserver/start "localhost" 3000)))]
      init))
  (stop [this]
    (db/stop (:db this))
    (webserver/stop (:webserver this))
    (-> this
        (dissoc :db)
        (dissoc :webserver))))

(defn create-system [] (MyApp.))

(defn -main [& args]
  (alter-var-root #'bootstrap/system (fn [_] (.start (create-system)))))
```

### 3: user

The user namespace (usually in the /dev folder separated by other production code in the src folder) brings components functionalities at the REPL exposing the (reset) function.  If you have tests, it will prevent them to reload (and run).

```clojure
(ns user
  (:require [clojure.test]
            [system]
            [bootstrap :as b :refer [system stop start]]))

(b/set-init! #'system/create-system)

(defn reset [] (binding [clojure.test/*load-tests* false] (b/reset)))
```

### 4: one sample component

So, how does a component look like? It is a simple namespace which contains the logic to connect/disconnect and any additional functions. The stateful part of the component (the connection) ends up in the main system definition (in bootstrap) through the start function. Other functions can retrieve connection/state from bootstrap anytime they need, like in the "get-all-accounts" example.

```clojure
(ns database
  (:require [bootstrap :refer [system]]
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
```

## Q&A

* Q: Why the component doesn't implement a defrecord?
* A: Personally, I'm not too worried they are not a defrecord. Why do I need to enforce a contract in my own code when I know I need to call a start/stop function?
* Q: But in your example you have a "defrecord"
* A: Right, it's the only one and you'll never touch it again. The important thing is that is not forcing YOUR components to do the same. Also: it's there to avoid a circular namespace dependency problem, not as a public interface.
* Q: What if you need the same component in another project?
* A: I copy paste. And usually, modify.
* Q: Argh, copy paste is bad, what if you find a bug in the original code?
* A: I fix it in all projects. Once stable, it won't change anymore.
* Q: But this is bad, this is not scalable!
* A: You see, this all idea of reusable libraries of components never worked and it gets complicated pretty fast. See the past 20 years of OO frameworks.  Do you really want a Spring/J2EE in your beautiful Clojure app?
