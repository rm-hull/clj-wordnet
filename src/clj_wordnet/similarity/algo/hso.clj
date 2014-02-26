(ns clj-wordnet.similarity.algo.hso
  (:refer-clojure :exclude [find])
  (:require
    [clj-wordnet.core :refer [synset-words make-dictionary]]
    [clj-wordnet.similarity.traverser :refer :all]))

(def min-score {:score 0})
(def max-score {:score 16})

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

(defn find [from to path abbrev distance state chdir]
  (reduce
    (partial max-key :score)
    min-score
    (pmap
      #(med-strong % to (conj path abbrev (:id %)) (inc distance) state chdir)
      (words (grouped-synsets from abbrev)))))

(defn find-udh [from to path distance states chdirs]
  (max-key :score
    (find from to path :upward     distance (states 0) (chdirs 0))
    (find from to path :downward   distance (states 1) (chdirs 1))
    (find from to path :horizontal distance (states 2) (chdirs 2))))

(defn find-dh [from to path distance states chdirs]
  (max-key :score
    (find from to path :downward   distance (states 0) (chdirs 0))
    (find from to path :horizontal distance (states 1) (chdirs 1))))

(defn find-d [from to path distance states chdirs]
  (find from to path :downward distance (states 0) (chdirs 0)))

(defn find-h [from to path distance states chdirs]
  (find from to path :horizontal distance (states 0) (chdirs 0)))

(defn med-strong
  "returns the maximum length of a legal path that was found in a
   given subtree of the recursive search. These return values are used by
   med-strong and the highest of these is returned."
  [from to path distance state chdir]
  (cond
    (and (= (:synset-id from) (:synset-id to)) (pos? distance))
    {:path path :distance distance :score (- 8 distance chdir)}

    (>= distance 5) min-score
    (= state 0) (find-udh from to path distance [1 2 3] [0 0 0])
    (= state 1) (find-udh from to path distance [1 4 5] [0 1 1])
    (= state 2) (find-dh  from to path distance [2 6]   [0 0])
    (= state 3) (find-dh  from to path distance [7 3]   [0 0])
    (= state 4) (find-d   from to path distance [4]     [1])
    (= state 5) (find-dh  from to path distance [4 5]   [2 1])
    (= state 6) (find-h   from to path distance [6]     [1])
    (= state 7) (find-d   from to path distance [7]     [1])
    :else min-score))

(defn relatedness
  "Computes the semantic relatedness of word senses according to
   the method described by Hirst and St-Onge (1998). In their paper they
   describe a method to identify 'lexical chains' in text. They measure the
   semantic relatedness of words in text to identify the links of the lexical
   chains. This measure of relatedness has been implemented in this module."
  [from to]
  (if
    (or
      (= (:synset-id from) (:synset-id to))
      (in-horizon? from to)
      (and
        (contained? from to)
        (in-updown? from to)))
    max-score
    (med-strong from to [(:id from)] 0 0 0)))
