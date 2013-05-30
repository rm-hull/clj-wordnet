(ns clj-wordnet.core
  (:require [swiss-arrows.core :refer :all])
  (:use [clojure.java.io :only [file]])
  (:import [edu.mit.jwi IDictionary Dictionary RAMDictionary]
           [edu.mit.jwi.item IWordID Word POS Pointer]))

(def pointer-lookup
  { :also-see Pointer/ALSO_SEE
    :antonym  Pointer/ANTONYM
    :cause    Pointer/CAUSE
    :hypernym Pointer/HYPERNYM
    :hyponym  Pointer/HYPONYM
    :similar-to Pointer/SIMILAR_TO
   }
  )

(defn- coerce-pointer 
  "Attempts to coerce a keyword, symbol or string into a POINTER enum"
  [k]
  (if (instance? Pointer k)
    k
    (pointer-lookup k)))

(defn- coerce-pos 
  "Attempts to coerce a keyword, symbol or string into a POS enum"
  [k]
  (if (instance? POS k)
    k
    (POS/valueOf (clojure.string/upper-case (name k)))))

(defn- from-java 
  "Descends down into each word, expanding synonyms that have not been
   previously seen"
  [^IDictionary dict ^Word word seen]
  (let [synset (.getSynset word)
        seen   (conj seen word)
        next-yak (fn [& words] (map #(from-java dict % seen) words))
        ]
  { :id (.getID word)
    :lemma (.getLemma word)
    :gloss (.getGloss synset)
    :synonyms (->> 
                (.getWords synset) 
                set 
                (remove seen)
                (apply next-yak))
    :related (fn [ptr] (->>
                         ;(.getRelatedWords word (coerce-pointer ptr))
                         ;(map next-yak)
                         (.getRelatedSynsets synset (coerce-pointer ptr))
                         (map #(apply next-yak (.getWords (.getSynset dict %))))
                         ))}))

(defn- word [^IDictionary dict ^IWordID word-id]
  (from-java dict (.getWord dict word-id) #{}))

(defn make-dictionary 
  "Initializes a dictionary implementation that mounts files on disk
   and has caching, returns a function which takes a lemma and part-of-speech
   and returns list of matching entries"
  [wordnet-dir]
  (let [dict (Dictionary. (file wordnet-dir))]
    (.open dict)
    (fn [lemma part-of-speech]
      (map (partial word dict) (.getWordIDs (.getIndexWord dict lemma (coerce-pos part-of-speech)))))))

(comment 

(def wordnet (make-dictionary "../delver/data/wordnet/dict/"))
 
(map :gloss (wordnet "dog" :noun)) 

(def dog (first (wordnet "dog" :noun)))
 
(def run (first (wordnet "run" :verb)))
 
(:lemma dog)
 
(:gloss dog)
 
(map :lemma (:synonyms dog))
 
(:synonyms dog)
 
(clojure.pprint/pprint 
((:related dog) :hypernym)
)  

) 

