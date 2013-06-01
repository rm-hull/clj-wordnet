(ns clj-wordnet.coerce
  (:require [clojure.string :as str])
  (:import [edu.mit.jwi.item POS Pointer]
           [java.lang.reflect Field]))

(defn- to-keyword [k]
  (-> 
    k
    name 
    (str/replace "_" "-")
    str/lower-case 
    keyword))

(defn- field-kv [^Field field]
  [ (to-keyword (.getName field)) (.get field nil) ])

(def pointer 
  "Attempts to coerce a keyword, symbol or string into a POINTER instance"
  (let [lut (into {} (map field-kv (.getFields Pointer)))]
    (fn [k]
      (if (instance? Pointer k)
        k
        (lut (to-keyword k))))))

(defn pos 
  "Attempts to coerce a keyword, symbol or string into a POS enum"
  [k]
  (if (instance? POS k)
    k
    (POS/valueOf (str/upper-case (name k)))))

(pointer :also-see)

(clojure.pprint/pprint pointer-lookup)
