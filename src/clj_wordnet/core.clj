(ns clj-wordnet.core
  (:use [clojure.java.io :only [file]]
        [clojure.string :only [upper-case lower-case]])
  (:require [clj-wordnet.coerce :as coerce]
            [clojure.string :as s])
  (:import [edu.mit.jwi IDictionary Dictionary RAMDictionary]
           [edu.mit.jwi.item IIndexWord ISynset IWordID IWord Word POS]
           [edu.mit.jwi.data ILoadPolicy]))

; JWI ICacheDictionary is not threadsafe
(def coarse-lock (Object.))

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
  (from-java
    dict
    (locking coarse-lock
      (.getWord dict word-id))))

(defn- word-ids [^IDictionary dict lemma part-of-speech]
  (let [pos (coerce/pos part-of-speech)
        ^IIndexWord index-word (locking coarse-lock
                     (.getIndexWord dict lemma pos))]
    (when index-word
      (.getWordIDs index-word))))

(defn make-dictionary
  "Initializes a dictionary implementation that mounts files on disk
   and has caching, returns a function which takes a lemma and part-of-speech
   and returns list of matching entries"
  [wordnet-dir & opt-flags]
  (let [file (file wordnet-dir)
        ^IDictionary dict (if (:in-memory (set opt-flags))
                            (RAMDictionary. file ILoadPolicy/IMMEDIATE_LOAD)
                            (Dictionary. file))]
      (.open dict)
    (fn [lemma & part-of-speech]
      (let [lemma (-> lemma str s/trim)]
        (when-not (empty? lemma)
          (->>
            (if (empty? part-of-speech) (POS/values) part-of-speech)
            (mapcat (partial word-ids dict lemma))
            (map (partial fetch-word dict))))))))

(defn related-synsets
  "Use a semantic pointer to fetch related synsets, returning a map of
   synset-id -> list of words"
  [m pointer]
  (let [^IDictionary dict (:dict m)
        ^IWord word (:word m)]
    (apply merge-with concat
      (for [synset-id (.getRelatedSynsets (.getSynset word) (coerce/pointer pointer))
            word      (.getWords (locking coarse-lock (.getSynset dict synset-id)))]
        { synset-id [(from-java dict word)] }))))

(defn related-words
  "Use a lexical pointer to fetch related words, returning a list of words"
  [m pointer]
  (let [^IDictionary dict (:dict m)
        ^IWord word (:word m)]
    (map
      (partial fetch-word dict)
      (.getRelatedWords word (coerce/pointer pointer)))))
