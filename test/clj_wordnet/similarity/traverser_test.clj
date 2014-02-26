(ns clj-wordnet.similarity.traverser-test
  (:require
    [clojure.test :refer :all]
    [clj-wordnet.test-client :refer [wordnet]]
    [clj-wordnet.core :refer :all]
    [clj-wordnet.similarity.traverser :refer :all]))

(def quick   (first (wordnet "quick"   :adjective)))
(def quickly (first (wordnet "quickly" :adverb)))
(def fast    (first (wordnet "fast"    :adjective)))
(def faster  (first (wordnet "faster"  :adjective)))

(deftest check-contained?
  (is (contained? quick quickly))
  (is (contained? quickly quick))
  (is (not (contained? quickly fast)))
  (is (not (contained? quick fast))))

(deftest check-grouped-synsets
  (is (= (map :lemma (val (first (grouped-synsets quick :horizontal)))) ["fast"]))
  ;(is (= (map :lemma (val (first (grouped-synsets fast :horizontal)))) ["accelerated"]))
  )
