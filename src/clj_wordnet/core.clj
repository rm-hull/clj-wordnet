(ns clj-wordnet.core
  (:use [clojure.java.io :only [file]]
        [clojure.string :only [upper-case lower-case]])
  (:require [clj-wordnet.coerce :as coerce])
  (:import [edu.mit.jwi IDictionary Dictionary RAMDictionary]
           [edu.mit.jwi.item IWordID Word POS Pointer]))

(defn- from-java 
  "Descends down into each word, expanding synonyms that have not been
   previously seen"
  [^IDictionary dict ^Word word seen]
  (let [synset (.getSynset word)
        seen   (conj seen word)
        next-yak (fn [& words] (map #(from-java dict % seen) words))
        ]
  { :id (.getID word)
    :pos   (-> word .getPOS .name lower-case keyword)
    :lemma (.getLemma word)
    :gloss (.getGloss synset)
    :synonyms (->> 
                (.getWords synset) 
                set 
                (remove seen)
                (apply next-yak))
    :dict dict
    :related (fn [ptr] (->>
                         ;(.getRelatedWords word (coerce/pointer ptr))
                         ;(map next-yak)
                         (.getRelatedSynsets synset (coerce/pointer ptr))
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
      (map (partial word dict) (.getWordIDs (.getIndexWord dict lemma (coerce/pos part-of-speech)))))))


(comment 

(def wordnet (make-dictionary "../delver/data/wordnet/dict/"))
 
(map :gloss (wordnet "dog" :noun)) 

(def dog (first (wordnet "dog" :noun)))
 
(def run (first (wordnet "run" :verb)))
 
(:pos dog)
 
(:gloss dog)
 
(map :lemma (:synonyms dog))
 
(:synonyms dog)
 
(clojure.pprint/pprint 
((:related dog) :hypernym)
)  
  
) 
