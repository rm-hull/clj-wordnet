(ns clj-wordnet.core
  (:use [clojure.java.io :only [file]]
        [clojure.string :only [upper-case lower-case]])
  (:require [clj-wordnet.coerce :as coerce]
            [clojure.string :as s])
  (:import [edu.mit.jwi IDictionary Dictionary RAMDictionary]
           [edu.mit.jwi.item IIndexWord ISynset IWordID IWord Word POS]
           [edu.mit.jwi.data ILoadPolicy]
           [edu.mit.jwi.morph WordnetStemmer]))

; JWI ICacheDictionary is not threadsafe
(def coarse-lock (Object.))

(defn- from-java
  "Lazily descends down into each word, expanding synonyms that have
   not been previously seen."
  ([^IDictionary dict ^Word word] (from-java dict word #{}))
  ([^IDictionary dict ^Word word seen]
    (let [synset (.getSynset word)
          seen   (conj seen word)]
    { :id (.getID word)
      :synset-id (.getID synset)
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

(defn- fetch-words [^IDictionary dict word-ids]
  (map (partial fetch-word dict) word-ids))

(defn- stem [^IDictionary dict lemma part-of-speech]
  (.findStems (WordnetStemmer. dict) lemma (coerce/pos part-of-speech)))

(defn- word-ids [^IDictionary dict lemma part-of-speech & [stem?]]
  (let [pos (coerce/pos part-of-speech)]
    (if stem?
      (mapcat (memfn ^IIndexWord getWordIDs)
        (for [stem (stem dict lemma part-of-speech)
              :let [^IIndexWord index-word (locking coarse-lock (.getIndexWord dict stem pos))]
              :when index-word]
          index-word))

      (when-let [index-word (locking coarse-lock (.getIndexWord dict lemma pos))]
        (.getWordIDs index-word)))))

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
      (let [lemma (-> lemma str s/trim)
            part-of-speech (if (empty? part-of-speech) (POS/values) part-of-speech)]
        (when-not (empty? lemma)
          (if-let [[_ lemma part-of-speech sense] (re-matches #"^(.+)#(.)#(\d+)$" lemma)]
            ; Handle things like "dog#n#1", and return a single instance
            (first
              (filter
                #(= (.getWordNumber ^IWordID (:id %)) (Integer/parseInt sense))
                (fetch-words dict (word-ids dict lemma part-of-speech false))))

            ; else return a list of matches
            (fetch-words dict
              (mapcat
                #(word-ids dict lemma % true)
                part-of-speech))))))))

(defn related-synsets
  "Use a semantic pointer to fetch related synsets, returning a
   map of synset-id -> list of words"
  [m & [pointer]]
  (when m
    (let [^IDictionary dict (:dict m)
          ^IWord word (:word m)
          related-synsets (if pointer
                            (.getRelatedSynsets (.getSynset word) (coerce/pointer pointer))
                            (.getRelatedSynsets (.getSynset word)))]
      (apply merge-with concat
        (for [synset-id related-synsets
              word      (.getWords (locking coarse-lock (.getSynset dict synset-id)))]
          {synset-id [(from-java dict word)]})))))

(defn related-words
  "Use a lexical pointer to fetch related words, returning a list of words"
  [m & [pointer]]
  (let [^IDictionary dict (:dict m)
        ^IWord word (:word m)]
    (when word
      (fetch-words
        dict
        (if pointer
          (.getRelatedWords word (coerce/pointer pointer))
          (.getRelatedWords word))))))

(defn synset-words
  [m]
  (let [^IDictionary dict (:dict m)
        ^IWord word (:word m)]
    (when word
      (map
        (partial from-java dict)
        (.getWords (.getSynset word))))))
