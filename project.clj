(defproject clj-wordnet "0.1.0"
  :description "A WordNet/JWI wrapper library"
  :url "https://github.com/delver/clj-wordnet"
  :license {:name "Creative Commons 3.0"
            :url "http://creativecommons.org/licenses/by/3.0/legalcode"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [edu.mit/jwi "2.3.3"]]
  :source-path "src"
  :min-lein-version "2.3.4"
  :description "A partial wrapper library for programmatic access to a WordNet database"
  :global-vars { *warn-on-reflection* true}
  :repositories [["delver" {:url "http://repo.delver.io/releases"
                            :snapshots false
                            :update :always}]]
)
