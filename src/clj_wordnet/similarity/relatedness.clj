(ns clj-wordnet.similarity.relatedness)

(defn make-relatedness [score & [error]]
  {:score score :error error})
