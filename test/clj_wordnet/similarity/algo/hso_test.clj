(ns clj-wordnet.similarity.algo.hso-test
  (:require
    [clojure.test :refer :all]
    [clj-wordnet.test-client :refer [wordnet]]
    [clj-wordnet.core :refer :all]
    [clj-wordnet.similarity.traverser :refer :all]
    [clj-wordnet.similarity.algo.hso :as hso])
  (:import
    [edu.mit.jwi.item IWordID]))

(def car (wordnet "car#n#1"))
(def bus (wordnet "bus#n#1"))

(deftest hso-relatedness
  (let [result (time (hso/relatedness car bus))]
    (println result)
    (is (= (result :score) 5))
    (is (= (.getSynsetID (-> result :path ^IWordID last)) (bus :synset-id)))))
