(ns clj-wordnet.core-test
  (:require
    [clojure.test :refer :all]
    [clj-wordnet.test-client :refer [wordnet]]
    [clj-wordnet.core :refer :all]))

(deftest fetch-with-noun
  (is (= "dog" (:lemma (first (wordnet "dog" :noun))))))

(deftest fetch-exact
  (is (= "metal supports for logs in a fireplace; \"the andirons were too hot to touch\""
         (-> "dog#n#7" wordnet synset :gloss))))

(deftest fetch-without-pos
  (is (= "dog" (:lemma (first (wordnet "dog "))))))

(deftest fetch-by-stemming
  (is (= nil (:lemma (first (wordnet "dogs")))))
  (is (= "dog" (:lemma (first (wordnet "dogs" :stem)))))
  (is (= "buy" (:lemma (first (wordnet "bought" :stem))))))

(deftest fetch-unknown-word
  (is (empty? (wordnet "fdssfsfs"))))

(deftest fetch-empty-word
  (is (empty? (wordnet ""))))

(deftest fetch-nil-word
  (is (empty? (wordnet nil))))

(deftest word-id-lookup
  (let [dog (first (wordnet "dog" :noun))]
    (is (= dog (wordnet "WID-02086723-N-01-dog")))))

(deftest synset-id-lookup
  (is (= "a member of the genus Canis (probably descended from the common wolf) that has been domesticated by man since prehistoric times; occurs in many breeds; \"the dog barked all night\""
         (:gloss (wordnet "SID-02086723-N")))))

(deftest synset-words
  (is (= '("dog" "domestic_dog" "Canis_familiaris")
         (map :lemma (words (wordnet "SID-02086723-N"))))))

(deftest related-synset-test
  (is (= '("SID-02085998-N" "SID-01320032-N")
         (map (comp str :id) (related-synsets (wordnet "SID-02086723-N") :hypernym)))))

(deftest semantic-relations-test
  (is (= '(:holonym-member :hypernym :hyponym :meronym-part)
         (sort (keys (semantic-relations (wordnet "SID-02086723-N")))))))

(deftest lexical-relations-test
  (is (:derivationally-related (lexical-relations (wordnet "WID-00982557-A-01-quick")))))

(deftest hypernym-test
  (is (= '("SID-02085998-N" "SID-01320032-N" "SID-02077948-N" "SID-01889397-N" "SID-01864419-N" "SID-01474323-N" "SID-01468898-N" "SID-00015568-N" "SID-00004475-N" "SID-00004258-N" "SID-00003553-N" "SID-00002684-N" "SID-00001930-N" "SID-00001740-N" "SID-00015568-N" "SID-00004475-N" "SID-00004258-N" "SID-00003553-N" "SID-00002684-N" "SID-00001930-N" "SID-00001740-N")
         (map :id (hypernyms (synset (wordnet "dog#n#1")))))))
