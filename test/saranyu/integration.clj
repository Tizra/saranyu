(ns saranyu.integration
  (:require [midje.sweet :refer :all]
            [environ.core :refer [env]]
            [saranyu.core :refer [sign]]
            [clj-http.client :as client]))

(fact-group :integration
            (fact "can talk to AWS using S3 signature"
                  (let [request {:url "https://s3-eu-west-1.amazonaws.com"
                                 :key (env :aws-key)
                                 :secret (env :aws-secret)}
                        request (sign request)]

                    (client/get (request :url) (merge request {:throw-exceptions false})) => (contains {:status 200})))

            (fact "can talk to AWS using v2 signature"
                  (let [request {:url "https://ec2.eu-west-1.amazonaws.com"
                                 :key (env :aws-key)
                                 :secret  (env :aws-secret)
                                 :params {:Version "2013-10-01"
                                          :Action "DescribeSecurityGroups"}
                                 }
                        request (sign request)]
                    (client/get (request :url) (merge request {:throw-exceptions false})) => (contains {:status 200})))

            (fact "can talk to AWS using v4 signature"
                  (let [request {:url "https://elasticloadbalancing.eu-west-1.amazonaws.com"
                                 :key   (env :aws-key)
                                 :secret  (env :aws-secret)
                                 :params {:Version "2012-06-01"
                                          :Action "DescribeLoadBalancers"}
                                 }
                        request (sign request)]
                    (client/get (request :url) (merge request {:throw-exceptions false})) => (contains {:status 200}))))
