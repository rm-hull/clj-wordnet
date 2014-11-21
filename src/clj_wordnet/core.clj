(ns clj-wordnet.core
  (:require [clojure.java.io :refer [file]]
            [clojure.string :refer [upper-case lower-case]]
            [clj-wordnet.coerce :as coerce]
            [clojure.string :as s]
            [clojure.set :refer :all])
  (:import [edu.mit.jwi IDictionary Dictionary RAMDictionary]
           [edu.mit.jwi.item IIndexWord ISynset ISynsetID SynsetID IWordID WordID IWord POS IPointer]
           [edu.mit.jwi.data ILoadPolicy]
           [edu.mit.jwi.morph WordnetStemmer]))

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
    { :id     (str (.getID word))
      :pos    (-> word .getPOS .name coerce/to-keyword)
      :lemma  (.getLemma word) }
    { :word   word
      :dict   dict }))

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

(def ^:dynamic *stem?* false)

(defn- stem [^IDictionary dict lemma part-of-speech]
  (.findStems (WordnetStemmer. dict) lemma (coerce/pos part-of-speech)))

(defn- word-ids
  ([^IDictionary dict part-of-speech]
     (let [index-words (iterator-seq (locking coarse-lock (.getIndexWordIterator
                                                           dict
                                                           (coerce/pos part-of-speech))))]
       (mapcat (memfn ^IIndexWord getWordIDs) index-words)))
  ([^IDictionary dict lemma part-of-speech]
     (let [pos (coerce/pos part-of-speech)]
       (if *stem?*
         (mapcat (memfn ^IIndexWord getWordIDs)
                 (for [stem (stem dict lemma part-of-speech)
                       :let [^IIndexWord index-word (locking coarse-lock (.getIndexWord dict stem (coerce/pos part-of-speech)))]
                       :when index-word]
                   index-word))
         (when-let [^IIndexWord index-word (locking coarse-lock (.getIndexWord
                                                                 dict lemma
                                                                 (coerce/pos part-of-speech)))]
           (.getWordIDs index-word))))))

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
      (letfn [(lookup
                ([lemma]
                   (cond
                    (keyword? lemma)
                    (map (partial fetch-word dict) (word-ids dict lemma))

                    (empty? ^String lemma)
                    nil

                    (.startsWith ^String lemma "WID")
                    (fetch-word dict (WordID/parseWordID ^String lemma))

                    (.startsWith ^String lemma "SID")
                    (fetch-synset dict (SynsetID/parseSynsetID ^String lemma))

                    :else
                    (if-let [[_ lemma part-of-speech sense] (re-matches #"^(.+)#(.)#(\d+)$" lemma)]
                      (lookup lemma part-of-speech (Integer/parseInt sense))
                      (mapcat (partial lookup lemma) (POS/values)))))

                ([^String lemma pos]
                   (map (partial fetch-word dict) (word-ids dict lemma pos)))

                ([^String lemma pos ^Integer sense]
                   (nth (lookup lemma pos) (dec sense))))]

        (fn [& args]
          (let [stem #{:stem}]
            (if (some stem args)
              (binding [*stem?* true]
                (apply lookup (remove stem args)))
              (apply lookup args)))))))

(defn word?
  [entry]
  (:word (meta entry)))

(defn synset?
  [entry]
  (:synset (meta entry)))

(defn synset
  "Fetch the synset of word."
  [word]
  {:pre [(word? word)]}
  (let [{^IWord word :word ^IDictionary dict :dict} (meta word)]
    (from-synset dict (.getSynset word))))

(defn words
  "Fetch the words matching the meaning of this synset."
  [synset]
  {:pre [(synset? synset)]}
  (let [{^ISynset synset :synset ^IDictionary dict :dict} (meta synset)]
    (map (partial from-word dict) (.getWords synset))))

(defn related-synsets
  "Returns a list of synsets related to the specified synset by the
  provided pointer."
  [synset pointer]
  {:pre [(synset? synset)]}
  (let [{^ISynset synset :synset ^IDictionary dict :dict} (meta synset)]
    (map
     (partial fetch-synset dict)
     (.getRelatedSynsets synset (coerce/keyword->pointer pointer)))))

(defn semantic-relations
  "Find all semantically related synsets, returning a map of pointers
  to lists of synsets."
  [synset]
  {:pre [(synset? synset)]}
  (let [{^ISynset synset :synset ^IDictionary dict :dict} (meta synset)]
   (into {}
         (map (fn [[^IPointer pointer synset-ids]]
                [(coerce/pointer->keyword pointer)
                 (map (partial fetch-synset dict) synset-ids)])
              (.getRelatedMap synset)))))

(defn related-words
  "Use a lexical pointer to fetch related words, returning a list of words"
  [word pointer]
  {:pre [(word? word)]}
  (let [{^IWord word :word ^IDictionary dict :dict} (meta word)]
   (map
    (partial fetch-word dict)
    (.getRelatedWords word (coerce/keyword->pointer pointer)))))

(defn lexical-relations
  [word]
  {:pre [(word? word)]}
  (let [{^IWord word :word ^IDictionary dict :dict} (meta word)]
   (into {}
         (map (fn [[^IPointer pointer word-ids]]
                [(coerce/pointer->keyword pointer)
                 (map (partial fetch-word dict) word-ids)])
              (.getRelatedMap word)))))

(defn traverse
  [synset & pointers]
  {:pre [(synset? synset)]}
  (let [next (mapcat (partial related-synsets synset) pointers)]
    (concat next (lazy-seq (mapcat #(apply traverse % pointers) next)))))

(defn hypernyms
  "Returns a lazy sequence of hypernyms of the specified synset. If
  any synset in the sequence has multiple hypernyms, their respective
  hypernyms will be concatenated, so do not rely on the order of
  hypernyms being strictly hyponym/hypernym."
  [synset]
  {:pre [(synset? synset)]}
  (traverse synset :hypernym :hypernym-instance))

(defn instances
  [synset]
  {:pre [(synset? synset)]}
  (let [hyponyms (related-synsets synset :hyponym)
        hyponym-instances (related-synsets synset :hyponym-instance)]
    (concat hyponym-instances (lazy-seq (mapcat instances hyponyms)))))
