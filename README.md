Clj-WordNet
===========

A thin/partial wrapper around some [JWI](http://projects.csail.mit.edu/jwi/) 
functionality, for interfacing the [WordNet](http://wordnet.princeton.edu/) 
database using idiomatic Clojure.

## Prerequisites

You will need [Leiningen](https://github.com/technomancy/leiningen) 2.1.2 or
above installed.

## Building

To build and install the library locally, run:

     lein install

## Including in your project

There (will be shortly) a version hosted at [Clojars](https://clojars.org/rm-hull/clj-wordnet).
For leiningen include a dependency:

```clojure
[rm-hull/clj-wordnet "0.0.1"]
```
    
For maven-based projects, add the following to your `pom.xml`:

```xml
<dependency>
  <groupId>rm-hull</groupId>
  <artifactId>clj-wordnet</artifactId>
  <version>0.0.1</version>
</dependency>
```

## WordNet Database

The WordNet database is not bundled in this project; it must be downloaded
separately from [here](http://wordnet.princeton.edu/wordnet/download/current-version/).

## Examples

```clojure
(def wordnet (make-dictionary "../path-to/wordnet/dict/"))

(def dog (first (wordnet "dog" :noun)))

(:lemma dog)
=> "dog"

(:pos dog)
=> :noun

(:gloss dog)
=> "a member of the genus Canis (probably descended from the common wolf) that
    has been domesticated by man since prehistoric times; occurs in many breeds; 
    \"the dog barked all night\""   

(map :lemma (:synonyms dog))
=> ("domestic_dog", "Canis_familiaris")
```

## TODO

* Implement ```(make-dictionary "../path-to/wordnet/dict/" :in-memory)``` to use
  RAM-based dictionary

* ~~Coerce functions into separate namespace~~

* Re-implement ```(related-synsets ...)``` and ```(related-words ...)```

* Push JWI 2.2.4 to central repository

## License

Same as JWI: MIT / [Creative Commons 3.0](http://creativecommons.org/licenses/by/3.0/legalcode)
