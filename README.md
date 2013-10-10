Clj-WordNet
===========

A thin/partial wrapper around some [JWI](http://projects.csail.mit.edu/jwi/) 
functionality, for interfacing the [WordNet](http://wordnet.princeton.edu/) 
database using idiomatic Clojure.

## Prerequisites

You will need [Leiningen](https://github.com/technomancy/leiningen) 2.3.2 or
above installed.

## Building

To build and install the library locally, run:

     lein install

## Including in your project

There is an initial version hosted at [Clojars](https://clojars.org/clj-wordnet/clj-wordnet).
For leiningen include a dependency:

```clojure
[clj-wordnet "0.0.4"]
```
    
For maven-based projects, add the following to your `pom.xml`:

```xml
<dependency>
  <groupId>clj-wordnet</groupId>
  <artifactId>clj-wordnet</artifactId>
  <version>0.0.4</version>
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

(def frump (first (wordnet "frump" :noun)))

(map :lemma (related-words frump :derivationally-related))) 
=> ("frumpy")

(map :lemma (flatten (vals (related-synsets dog :hypernym))))
=> ("domestic_animal" "domesticated_animal" "canine" "canid")
```

## Dictionary

The default dictionary will load definitions from the database as needed
and they will be cached as necessary. If higher performance is required
and there is sufficient memory available to the JVM, then the dictionary 
can be made to be resident entirely in memory, as below. This will force 
an immediate load of the dictionary into RAM, where there may be a 
perceptible delay on startup. 

```clojure
(def wordnet (make-dictionary "../path-to/wordnet/dict/" :in-memory))
```

Note: Wordnet is quite large, and usually won’t fit into the standard heap on most
32-bit JVMs. You need to increase your heap size. On the Sun JVM, this involves 
the command line flag -Xmx along with a reasonable heap size, say, 500 MB or 1 GB.

## Coersion

Wherever possible, using clojure keywords are preferred over JWI-specific enums and
static constants. However, it is entirely possible to use the JWI class instances
interchangeably. For example, 

* the ```POS.NOUN``` enum can be replaced with ```:noun```,

* the ```Pointer.DERIVED_FROM_ADJ``` constant can be replaced with ```:derived-from-adj```

Note: case is not important, and dashes are coverted to/from underscores.

## See Also

The JWI has been mavenized and rolled up into a github repo, here: https://github.com/delver/jwi.
The resulting artifacts have been deployed to http://repo.delver.io/releases, and this has been 
referenced in this project's repository resolution section.

## TODO

* ~~Implement ```(make-dictionary "../path-to/wordnet/dict/" :in-memory)``` to use
  RAM-based dictionary~~

* ~~Coerce functions into separate namespace~~

* ~~Re-implement ```(related-synsets ...)``` and ```(related-words ...)```~~

* ~~Push JWI 2.2.4 to central repository~~

* Unit tests & Travis CI

## License

Same as JWI: MIT / [Creative Commons 3.0](http://creativecommons.org/licenses/by/3.0/legalcode)
