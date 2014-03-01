(ns clj-wordnet.similarity.algo.hso
  (:refer-clojure :exclude [find])
  (:require
    [clj-wordnet.core :refer [synset-words make-dictionary]]
    [clj-wordnet.similarity.traverser :refer :all]
    [clj-wordnet.similarity.relatedness :refer :all]))

(def min-score 0)
(def max-score 16)

(defn- synset-ids [synsets]
  (set (keys synsets)))

(defn- words [synsets]
  (distinct (apply concat (vals synsets))))

(defn- in-horizon? [m1 m2]
  (let [horizontal1 (synset-ids (grouped-synsets m1 :horizontal))
        horizontal2 (synset-ids (grouped-synsets m2 :horizontal))]
    (or
      (contains? horizontal1 (:synset-id m2))
      (contains? horizontal2 (:synset-id m1)))))

(defn- in-updown? [m1 m2]
  (let [upward2   (synset-ids (grouped-synsets m2 :upward))
        downward2 (synset-ids (grouped-synsets m2 :downward))]
    (or
      (contains? upward2 (:synset-id m1))
      (contains? downward2 (:synset-id m1)))))

(declare med-strong)

(defn find [synset-groups to path distance state chdir]
  (reduce max 0
    (map
      #(med-strong % to path (inc distance) state chdir)
      (words synset-groups))))

(defn find-udh [from to path distance states chdirs]
  (max
    (find (grouped-synsets from :upward) to (conj path (:id from) :u) distance (states 0) (chdirs 0))
    (find (grouped-synsets from :downward) to (conj path (:id from) :d)distance (states 1) (chdirs 1))
    (find (grouped-synsets from :horizontal) to (conj path (:id from) :h) distance (states 2) (chdirs 2))))

(defn find-dh [from to path distance states chdirs]
  (max
    (find (grouped-synsets from :downward) to (conj path (:id from) :d) distance (states 0) (chdirs 0))
    (find (grouped-synsets from :horizontal) to (conj path (:id from) :h) distance (states 1) (chdirs 1))))

(defn find-d [from to path distance states chdirs]
  (find (grouped-synsets from :downward) to (conj path (:id from) :d) distance (states 0) (chdirs 0)))

(defn find-h [from to path distance states chdirs]
  (find (grouped-synsets from :horizontal) to (conj path (:id from) :h) distance (states 0) (chdirs 0)))

(defn med-strong
  "returns the maximum length of a legal path that was found in a
   given subtree of the recursive search. These return values are used by
   med-strong and the highest of these is returned."
  [from to path distance state chdir]

  (let [score (cond
                (and (= (:lemma from) (:lemma to)) (pos? distance)) (- 8 distance chdir)
                (>= distance 5) 0
                (= state 0) (find-udh from to path distance [1 2 3] [0 0 0])
                (= state 1) (find-udh from to path distance [1 4 5] [0 1 1])
                (= state 2) (find-dh  from to path distance [2 6]   [0 0])
                (= state 3) (find-dh  from to path distance [7 3]   [0 0])
                (= state 4) (find-d   from to path distance [4]     [1])
                (= state 5) (find-dh  from to path distance [4 5]   [2 1])
                (= state 6) (find-h   from to path distance [6]     [1])
                (= state 7) (find-d   from to path distance [7]     [1])
                :else 0)]

    (when (pos? score)
    (println
      {:from (:id from)
       :to (:id to)
       :path path
       :distance distance
       :state state
       :chdir chdir
       :score score
       }))

    score))

(defn relatedness
  "Computes the semantic relatedness of word senses according to
   the method described by Hirst and St-Onge (1998). In their paper they
   describe a method to identify 'lexical chains' in text. They measure the
   semantic relatedness of words in text to identify the links of the lexical
   chains. This measure of relatedness has been implemented in this module."
  [from to]
  (make-relatedness
    (if
      (or
        (= (:synset-id from) (:synset-id to))
        (in-horizon? from to)
        (and
          (contained? from to)
          (in-updown? from to)))
      max-score
      (med-strong from to [] 0 0 0))))
