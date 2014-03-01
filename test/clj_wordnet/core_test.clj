(ns clj-wordnet.core-test
  (:require
    [clojure.test :refer :all]
    [clj-wordnet.test-client :refer [wordnet]]
    [clj-wordnet.core :refer :all]))

(deftest fetch-with-noun
  (is (= "dog" (:lemma (first (wordnet "dog" :noun))))))

(deftest fetch-without-pos
  (is (= "dog" (:lemma (first (wordnet "dog "))))))

(deftest fetch-with-pos-and-index
  (is (= "informal term for a man; \"you lucky dog\""
         (:gloss (wordnet "dog#n#3")))))

(deftest fetch-unknown-word
  (is (empty? (wordnet "fdssfsfs"))))

(deftest fetch-empty-word
  (is (empty? (wordnet ""))))

(deftest fetch-nil-word
  (is (empty? (wordnet nil))))

(deftest fetch-by-stemming
  (is (= "dog" (:lemma (first (wordnet "dogs")))))
  (is (= "buy" (:lemma (first (wordnet "bought"))))))

(deftest relational-synset-test
  (let [dog (first (wordnet "dog" :noun))]
    (is (= ["domesticated_animal" "domestic_animal" "canid" "canine"])
           (map :lemma (flatten (vals (related-synsets dog :hypernym)))))))
