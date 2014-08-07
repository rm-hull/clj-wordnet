(ns clj-wordnet.coerce
  (:require [clojure.string :as str]
            [clojure.set :refer [map-invert]])
  (:import [edu.mit.jwi.item POS Pointer]
           [java.lang.reflect Field]))

(defn to-keyword [k]
  (-> k
      name
      (str/replace #"[_ ]" "-")
      str/lower-case 
      keyword))

(def keyword->pointer
  (into {}
        (map (fn [^Field field] [(to-keyword (.getName field)) (.get field nil)])
             (.getFields Pointer))))

(def pointer->keyword
  (map-invert keyword->pointer))

(defn pos
  "Attempts to coerce a keyword, symbol or string into a POS enum"
  [k]
  (cond
    (instance? POS k)
    k

    (#{"n" "v" "a" "r" "s"} (str/lower-case k))
    (POS/getPartOfSpeech ^char (get k 0))

    :else
    (POS/valueOf (str/upper-case (name k)))))

