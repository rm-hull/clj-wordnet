(ns clj-wordnet.core
  (:use [clojure.java.io :only [file]]
        [clojure.string :only [upper-case lower-case]])
  (:require [clj-wordnet.coerce :as coerce])
  (:import [edu.mit.jwi IDictionary Dictionary RAMDictionary]
           [edu.mit.jwi.item IWordID IWord Word]))

(defn- from-java 
  "Descends down into each word, expanding synonyms that have not been
   previously seen"
  ([^IDictionary dict ^Word word] (from-java dict word #{})) 
  ([^IDictionary dict ^Word word seen]
    (let [synset (.getSynset word)
          seen   (conj seen word)]
    { :id (.getID word)
      :pos   (-> word .getPOS .name lower-case keyword)
      :lemma (.getLemma word)
      :gloss (.getGloss synset)
      :synonyms (->> (.getWords synset) set (remove seen) (map #(from-java dict % seen)))
      :word word
      :dict dict })))

(defn- fetch-word [^IDictionary dict ^IWordID word-id]
  (from-java dict (.getWord dict word-id)))

(defn make-dictionary 
  "Initializes a dictionary implementation that mounts files on disk
   and has caching, returns a function which takes a lemma and part-of-speech
   and returns list of matching entries"
  [wordnet-dir]
  (let [dict (Dictionary. (file wordnet-dir))]
    (.open dict)
    (fn [lemma part-of-speech]
      (map 
        (partial fetch-word dict) 
        (.getWordIDs (.getIndexWord dict lemma (coerce/pos part-of-speech)))))))

(defn related-synsets 
  "Use a semantic pointer to fetch related synsets, returning a map of
   synset-id -> list of words"
  [m pointer]
  (let [^IDictionary dict (:dict m)
        ^IWord word (:word m)]
    (apply merge-with concat
      (for [synset-id (.getRelatedSynsets (.getSynset word) (coerce/pointer pointer)) 
            word      (.getWords (.getSynset dict synset-id)) ]
        { synset-id [(from-java dict word)] }))))

(defn related-words 
  "Use a lexical pointer to fetch related words, returning a list of words"
  [m pointer]
  (let [^IDictionary dict (:dict m)
        ^IWord word (:word m)]
    (map 
      (partial fetch-word dict)
      (.getRelatedWords word (coerce/pointer pointer)))))
