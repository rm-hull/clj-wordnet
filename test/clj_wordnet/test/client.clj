(ns clj-wordnet.test.client
  (:use [clojure.test]
        [clj-wordnet.core]))

(def wordnet (make-dictionary "../delver/resources/wordnet/"))

(deftest fetch-with-noun
  (is (= "dog" (:lemma (first (wordnet "dog" :noun))))))

(deftest fetch-without-pos
  (is (= "dog" (:lemma (first (wordnet "dog "))))))

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
  (is (= '(:member-holonym :part-meronym :hyponym :hypernym)
         (keys (semantic-relations (wordnet "SID-02086723-N"))))))

(deftest lexical-relations-test
  (is (:derivationally-related-form (lexical-relations (wordnet "WID-00982557-A-01-quick")))))
