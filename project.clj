(defproject saranyu "0.0.3-Tizra"
  :description "A signature generating library for AWS APIs"
  :url "https://github.com/mixradio/saranyu"
  :license {:name "New BSD License or Modified BSD License"
            :url "https://raw.github.com/mixradio/saranyu/master/resources/LICENSE.txt"}
  :scm {:name "git"
        :url "https://github.com/daviddurand/saranyu"}
  :dependencies [[org.clojure/clojure "1.8.0-beta1"]
                 [commons-codec "1.8"]
                 [clj-time "0.6.0"]]

  :profiles {:dev {:dependencies [[environ "0.4.0"]
                                  [clj-http "0.7.7"]
                                  [midje "1.5.1"]]
                   :plugins [[lein-midje "3.0.1"]
                             [jonase/kibit "0.0.8"]]}})
