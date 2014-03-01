(ns clj-wordnet.similarity.algo.hso-test
  (:require
    [clojure.test :refer :all]
    [clj-wordnet.test-client :refer [wordnet]]
    [clj-wordnet.core :refer :all]
    [clj-wordnet.similarity.traverser :refer :all]
    [clj-wordnet.similarity.algo.hso :as hso]))

(def car (wordnet "car#n#1"))
(def bus (wordnet "bus#n#1"))

(deftest hso-relatedness
  (time
    (is (= {:score 7 :error nil}
           (hso/relatedness car bus)))))





