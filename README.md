[![Build Status](https://travis-ci.com/magnetcoop/ttlcache.svg?branch=master)](https://travis-ci.com/magnetcoop/ttlcache)
[![Clojars Project](https://img.shields.io/clojars/v/coop.magnet/ttlcache.svg)](https://clojars.org/coop.magnet/ttlcache)

# coop.magnet.ttlcache - forked from uk.me.rkd.ttlcache

A Clojure library designed to improve upon core.cache's TTLCache for
some use cases. Specifically, it allows cache expiry in less than O(N)
time (improving performance when small numbers of entries are
expiring), and it allows per-item TTLs rather than a fixed TTL for the
whole cache, which is useful for applications like DNS caching or
[HTTP caching](https://developers.google.com/speed/articles/caching)
where protocol responses specify a TTL.

Based on [data.priority-map](https://github.com/clojure/data.priority-map)
and [core.cache](https://github.com/clojure/core.cache).

## Installation

[![Clojars Project](https://clojars.org/coop.magnet/ttlcache/latest-version.svg)](https://clojars.org/coop.magnet/ttlcache)

## Usage
```clojure
(require '[coop.magnet.ttlcache :refer [per-item-ttl-cache-factory ttl-cache-factory]])
```

`coop.magnet.ttlcache` contains two public functions, both factory
functions returning an instance of
[CacheProtocol](https://github.com/clojure/core.cache/wiki/Extending):

* `ttl-cache-factory` is designed as a drop-in replacement for the
  core.cache one, specifying a fixed TTL (in milliseconds) for the
  whole cache. See [the core.cache
  docs](https://github.com/clojure/core.cache/wiki/TTL) for more.
* `per-item-ttl-cache-factory` creates a PerItemTTLCache instance. It
  takes two arguments:
  * a map of values to seed the cache with,
  * and a function which is applied to the key and value of any added
    entries, and returns the TTL in milli-seconds for the
    entries. `ttl-cache-factory` is built in this factory, and just
    supplies `(constantly n`) as the function.

The best example is probably the tests:

```clojure
user> (require '[coop.magnet.ttlcache :refer [per-item-ttl-cache-factory ttl-cache-factory]]
               '[clojure.test :refer [testing is]])
nil
user> (def sleepy #(do (Thread/sleep %2) %))
#'user/sleepy
user> (testing "TTL cache does not return a value that has expired."
        (let [cache-values {:a {:val 1 :ttl 900}
                            :b {:val 2 :ttl 500}}
              ttlcache (per-item-ttl-cache-factory cache-values :ttl-getter (fn [key value] (:ttl value)))]
          (is (= {:val 1 :ttl 900}
                 (-> ttlcache
                     (sleepy 700)
                     (. lookup :a))))
          (is (nil? (-> ttlcache
                        (sleepy 700)
                        (. lookup :b))))))
true
user>
```

## Big-O complexity

In versions prior to 0.6 (when this library was developed), the
TTLCache in core.cache uses a pair of maps - one to hold the values
and another to hold the TTLs. This means most operations were quite
efficient, but expiring items from the cache requires iterating over
the whole map, so happens in O(N) time.

This TTLCache uses
[data.priority-map](https://github.com/clojure/data.priority-map) to
store the TTLs in a priority queue, with O(1) find-min and O(log N)
delete-min.

Lookup, insertion and manual cache eviction basically equate to
get/assoc/dissoc on a Clojure priority map, so take O(log n) time
(marginally worse than O(log32 n) for core.cache, but nonetheless very
scalable).

TTL-based expiry (done as part of adding to the cache in both
implementations), however, only takes O(M log N) time, where M is the
number of items evicted and N is the number of items in the
cache. This means it performs better than the core.cache
implementation at the time, when small numbers of items are being
expired at once - in the best case, where no items expire, it is O(1)
(just a heap peek).

Expiry of large numbers of items at once is less efficient than
core.cache - obviously the scalability of O(M log N) gets closer to
O(N) as M gets larger, but also because large numbers of operations on
the pure-Clojure priority map perform worse than operations on an
ordinary Java-based `transient`-able map.

## Tests vs core.cache

```
$ lein test

lein test coop.magnet.core-cache-comparison-test
With    0 out of 5,000 cache items being expired, adding an item to my TTLCache is 230.9435502424312 times faster than the core.cache one
With   50 out of 5,000 cache items being expired, adding an item to my TTLCache is 4.757156584034744 times faster than the core.cache one
With  250 out of 5,000 cache items being expired, adding an item to my TTLCache is 0.752112969429968 times faster than the core.cache one
With 4950 out of 5,000 cache items being expired, adding an item to my TTLCache is 26.03863189088509 times SLOWER than the core.cache one

With a 5,000-entry cache, looking an item up in my TTLCache is  0.9778370698218088  times faster than the core.cache one

With a 5,000-entry cache, removing an item from my TTLCache is  2.2084563541938076  times faster than the core.cache one

core.cache's TTLCache: finding an item in a cache with 5,000 entries takes  1.1357957184637633 longer than one with 1,000 entries
My TTLCache: finding an item in a cache with 5,000 entries takes  1.0456086997086054 longer than one with 1,000 entries

core.cache TTLCache: removing an item from a cache with 5,000 entries takes  0.9503029199706673 longer than one with 1,000 entries
My TTLCache: removing an item from a cache with 5,000 entries takes  1.0909547240548432 longer than one with 1,000 entries

core.cache's TTLCache: adding an item to a cache with 5,000 entries takes  5.133550967673091 longer than one with 1,000 entries
My TTLCache: adding an item to a cache with 5,000 entries takes  1.2136739023112617 longer than one with 1,000 entries
```

## Running the tests

`lein test coop.magnet.ttlcache-test` will run a quick set of unit tests
to verify the function.

`lein test coop.magnet.core-cache-comparison-test` will run a more
exhaustive set of tests based on Criterium to verify that the caches
have the expected O(log n) complexity, and to characterise its
performance strengths and weaknesses against the core.cache version.

## License

Copyright © 2014 Robert Day
Copyright © 2021 Magnet, S. Coop.

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
