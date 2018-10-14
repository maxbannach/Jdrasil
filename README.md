# Jdrasil
A Modular Library for Computing Tree Decompositions

Authors: [Max Bannach](http://www.tcs.uni-luebeck.de/de/mitarbeiter/bannach/), [Sebastian Berndt](https://seberndt.github.io), and [Thorsten Ehlers](http://www.zs.informatik.uni-kiel.de/de/mitarbeiter)

# About
Jdrasil is a library to compute tree decompositions of simple, undirected graphs. It was developed for the first Parameterized Algorithms and Computational Experiments Challenge [(PACE)](https://pacechallenge.org). It provides exact sequential and parallel, as well as heuristic, and approximation algorithms.

Jdrasil is build in a very modular fashion. This allows researchers to simply add new algorithms, heuristics, or preprocessing routines, which can then be combined in any way.

You can obtain the latest stable version of Jdrasil [here](https://maxbannach.github.io/Jdrasil/current/Jdrasil.jar). Use the [manual](https://maxbannach.github.io/Jdrasil/current/manual.pdf) for an overview of the features of Jdrasil, or browse the [JavaDoc](https://maxbannach.github.io/Jdrasil/javadoc) for some implementation details. You can also build the latest version of Jdrasil manually (see below).

# Installation
Jdrasil uses [Gradle](https://gradle.org) as build tool. Thanks to the gradle wrapper, nothing extra has to be installed in order to install Jdrasil.

To build Jdrasil, simply invoke the gradle build script:
```
cd Jdrasil
./gradlew assemble
```
There is also a bat-file for windows systems. After the script is finished, an executable jar will be placed in: 
```
build/jars/Jdrasil.jar
```
The jar can be used as library or as standalone:
```
java −jar build/jars/Jdrasil.jar
java −cp build/jars/Jdrasil.jar jdrasil.Exact
java −cp build/jars/Jdrasil.jar jdrasil.Heuristic
java −cp build/jars/Jdrasil.jar jdrasil.Approximation
```

## Building Start Scripts
Jdrasil comes with PACE like starting scripts: `tw-exact`, `tw-heuristic`, and `tw-approximation`. They can be build with gradle:
```
./gradlew exact
./gradlew heuristic
./gradlew approximation
```
Use the scripts as defined on the [(PACE)](https://pacechallenge.org) website:
```
./tw−exact −s 42 < myGraph.gr > myGraph.td
./tw−heuristic −s 42 < myHugeGraph.gr > myHugeGraph.td
```

## Build the Documentation
Jdrasil comes with a manual and JavaDocs. To build the manual, an up-to-date LuaLaTeX installation is required:
```
./gradlew manual
./gradlew javadoc
```
The manual will be placed in `build/docs/manual/manual.pdf` and the JavaDocs in `build/docs/javadoc`.

## Build for PACE
Jdrasil provides a Gradle task that builds the PACE-version of Jdrasil. In particular, this will build Jdrasil and create the above mentioned start scripts.
```
./gradlew pace
```
