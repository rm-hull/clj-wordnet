(ns clj-wordnet.core-test
  (:require
    [clojure.test :refer :all]
    [clj-wordnet.test-client :refer [wordnet]]
    [clj-wordnet.core :refer :all]))

(deftest fetch
  (is (empty? (wordnet "fdssfsfs")))
  (is (empty? (wordnet "")))
  (is (empty? (wordnet nil)))
  (is (= "dog" (:lemma (first (wordnet "dog" :noun))))))
  (is (= "dog" (:lemma (first (wordnet "dog ")))))
  (is (= "metal supports for logs in a fireplace; \"the andirons were too hot to touch\""
         (:gloss (wordnet "dog#n#3"))))

(deftest synonyms
  (is (= ["domestic_dog" "Canis_familiaris"]
         (map :lemma (:synonyms (wordnet "dog#n#1"))))))

(deftest fetch-by-stemming
  (is (= "dog" (:lemma (first (wordnet "dogs")))))
  (is (= "buy" (:lemma (first (wordnet "bought"))))))

(deftest relational-synset-test
  (let [dog (first (wordnet "dog" :noun))]
    (is (= ["domesticated_animal" "domestic_animal" "canid" "canine"])
           (map :lemma (flatten (vals (related-synsets dog :hypernym)))))))
