(ns clj-wordnet.core
  (:use [clojure.java.io :only [file]]
        [clojure.string :only [upper-case lower-case]])
  (:require [clj-wordnet.coerce :as coerce]
            [clojure.string :as s])
  (:import [edu.mit.jwi IDictionary Dictionary RAMDictionary]
           [edu.mit.jwi.item IIndexWord ISynset ISynsetID SynsetID IWordID WordID IWord POS IPointer]
           [edu.mit.jwi.data ILoadPolicy]))

; JWI ICacheDictionary is not threadsafe
(def coarse-lock (Object.))

(defn- from-synset
  [^IDictionary dict ^ISynset synset]
    (with-meta 
      { :id (str (.getID synset))
        :gloss (.getGloss synset)}
      { :synset synset
        :dict dict }))

(defn- from-word
  "Descends down into each word, expanding synonyms that have not been
   previously seen"
  [^IDictionary dict ^IWord word]
  (with-meta
    { :id (str (.getID word))
      :pos   (-> word .getPOS .name coerce/to-keyword)
      :lemma (.getLemma word)
      :synset (from-synset dict (.getSynset word))}
    { :word word
      :dict dict }))

(defn- fetch-word [^IDictionary dict ^IWordID word-id]
  (from-word
    dict
    (locking coarse-lock
      (.getWord dict word-id))))

(defn- fetch-synset [^IDictionary dict ^ISynsetID synset-id]
  (from-synset
   dict
   (locking coarse-lock
     (.getSynset dict synset-id))))

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
        (cond
         (empty? lemma) nil
         (.startsWith lemma "WID") (fetch-word dict (WordID/parseWordID lemma))
         (.startsWith lemma "SID") (fetch-synset dict (SynsetID/parseSynsetID lemma))
         :else  (->>
                 (if (empty? part-of-speech) (POS/values) part-of-speech)
                 (mapcat (partial word-ids dict lemma))
                 (map (partial fetch-word dict))))))))

(defn words
  [synset]
  (let [{^ISynset synset :synset ^IDictionary dict :dict} (meta synset)]
    (map (partial from-word dict) (.getWords synset))))

(defn related-synsets
  "Use a semantic pointer to fetch related synsets, returning a map of
   synset-id -> list of words"
  [synset pointer]
  (let [{^ISynset synset :synset ^IDictionary dict :dict} (meta synset)]
    (map
     (partial fetch-synset dict)
     (.getRelatedSynsets synset (coerce/pointer pointer)))))

(defn semantic-relations
  [synset]
  (let [{^ISynset synset :synset ^IDictionary dict :dict} (meta synset)]
   (into {}
         (map (fn [[^IPointer pointer synset-ids]]
                [(coerce/to-keyword (.getName pointer))
                 (map (partial fetch-synset dict) synset-ids)])
              (.getRelatedMap synset)))))

(defn related-words
  "Use a lexical pointer to fetch related words, returning a list of words"
  [word pointer]
  (let [{^IWord word :word ^IDictionary dict :dict} (meta word)]
   (map
    (partial fetch-word dict)
    (.getRelatedWords word (coerce/pointer pointer)))))

(defn lexical-relations
  [word]
  (let [{^IWord word :word ^IDictionary dict :dict} (meta word)]
   (into {}
         (map (fn [[^IPointer pointer word-ids]]
                [(coerce/to-keyword (.getName pointer))
                 (map (partial fetch-word dict) word-ids)])
              (.getRelatedMap word)))))
