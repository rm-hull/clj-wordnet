(ns clj-wordnet.similarity.traverser
  (:require
    [potemkin :refer [fast-memoize]]
    [clj-wordnet.core :refer [related-synsets synset-words]]))

(def directional-synset-pointers
  { :horizontal [:also-see :antonym :attribute
                 :pertainym :similar-to]
    :downward   [:cause :entailment :holonym
                 :holonym-member :holonym-substance :holonym-part
                 :hyponym]
    :upward     [:hypernym :meronym
                 :meronym-member :meronym-part
                 :meronym-substance]})

(defn grouped-synsets [m direction]
  (let [synset-id (:synset-id m)
        f (memoize
            (fn [synset-id direction]
              (apply merge-with concat
                (map
                  (partial related-synsets m)
                  (directional-synset-pointers direction)))))]
    (f synset-id direction)))


(defn contained? [m1 m2]
  (some identity
    (for [^String a (map :lemma (synset-words m1))
          ^String b (map :lemma (synset-words m2))]
      (or
        (>= (.indexOf a b) 0)
        (>= (.indexOf b a) 0)))))
