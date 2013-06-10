(ns clj-wordnet.test.client
  (:use [clojure.test] 
        [clj-wordnet.core]))

(def wordnet (make-dictionary "../delver/data/wordnet/dict"))

(deftest fetch-with-noun
  (is (= "dog" (:lemma (first (wordnet "dog" :noun))))))

(deftest fetch-without-pos
  (is (= "dog" (:lemma (first (wordnet "dog"))))))

(deftest fetch-unknown-word
  (is (= '() (wordnet "fdssfsfs"))))

