(defproject coop.magnet/ttlcache "0.2.0"
  :description "A variation of core.cache's TTLCache specialised for some different cases. Forked from https://github.com/rkday/ttlcache"
  :url "https://github.com/magnetcoop/ttlcache"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.clojure/core.cache "1.0.207"]
                 [criterium "0.4.6"]
                 [org.clojure/data.priority-map "1.0.0"]]
  :deploy-repositories [["snapshots" {:url "https://clojars.org/repo"
                                      :username :env/clojars_username
                                      :password :env/clojars_password
                                      :sign-releases false}]
                        ["releases"  {:url "https://clojars.org/repo"
                                      :username :env/clojars_username
                                      :password :env/clojars_password
                                      :sign-releases false}]]
  :test-paths ["test"]
  :test-selectors {:default (fn [m] (not (or (:integration m) (:regression m))))
                   :all (constantly true)
                   :integration :integration
                   :regression :regression}
  :profiles
  {:dev [:project/dev :profiles/dev]
   :repl {:repl-options {:host "0.0.0.0"
                         :port 4001}}
   :profiles/dev {}
   :project/dev {:plugins [[jonase/eastwood "0.3.14"]
                           [lein-cljfmt "0.7.0"]]}})
