;; Copyright 2014 Rob Day
;; Copyright 2021 Magnet. S. Coop.
;; Released under the EPL
(ns coop.magnet.ttlcache
  (:require [clojure.core.cache :refer [CacheProtocol defcache]]
            [clojure.data.priority-map :refer [priority-map]]))

(defn- expires
  [ttl]
  (+ ttl (System/currentTimeMillis)))

(defn- setval
  [newkey newval ttl [info ttls]]
  [(assoc info newkey newval) (assoc ttls newkey (expires ttl))])

(defn- expire
  ([cache]
   (expire cache (System/currentTimeMillis)))
  ([[info ttls] now]
   (if (empty? ttls)
     [(persistent! info) ttls]
     (let [[key expires] (peek ttls)]
       (if (> now expires)
         (recur [(dissoc info key) (pop ttls)] now)
         [info ttls])))))

(defcache PerItemTTLCache [cache expiry-heap get-ttl]
  CacheProtocol
  (lookup [this item]
          (let [ret (. this lookup item ::nope)]
            (when-not (= ret ::nope) ret)))
  (lookup [this item not-found]
          (if (. this has? item)
            (get cache item)
            not-found))
  (has? [_ item]
        (let [expires (get expiry-heap item 0)]
          (< (System/currentTimeMillis) expires)))
  (hit [this _]
       this)
  (miss [_ item result]
        (let [updated-cache (setval item result (get-ttl item result) [cache expiry-heap])
              after-expiry (expire updated-cache)]
          (PerItemTTLCache. (first after-expiry)
                            (second after-expiry)
                            get-ttl)))
  (seed [_ base]
        (let [now (System/currentTimeMillis)]
          (PerItemTTLCache. base
                            (into (priority-map) (for [x base]
                                                   [(key x) (+ now (get-ttl (key x) (val x)))]))
                            get-ttl)))
  (evict [_ key]
         (PerItemTTLCache. (dissoc cache key)
                           (dissoc expiry-heap key)
                           get-ttl))
  Object
  (toString [_]
            (str cache \, \space expiry-heap)))

(defn per-item-ttl-cache-factory
  "Returns a TTL cache with the cache and expiration-table initialied to `base` --
  each item in `base` with its own time-to-live.

  This function also takes a `:get-ttl` keyword argument that defines
  a function which is applied to the key and value of any added entry,
  and is expected to return the TTL -in milli-seconds- for the entry."
  [base & {get-ttl :ttl-getter}]
  {:pre [(map? base)
         (fn? get-ttl)]}
  (clojure.core.cache/seed (PerItemTTLCache. {} (priority-map) get-ttl) base))

(defn ttl-cache-factory
  "Returns a TTL cache with the cache and expiration-table initialied to `base` --
  each with the same time-to-live.

  This function also allows an optional `:ttl` argument that defines the default
  time in milliseconds that entries are allowed to reside in the cache. If not
  specified, a default of 2000 milliseconds is used."
  [base & {ttl :ttl :or {ttl 2000}}]
  {:pre [(and (number? ttl) (<= 0 ttl))
         (map? base)]}
  (per-item-ttl-cache-factory base :ttl-getter (constantly ttl)))
