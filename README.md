# Jdrasil
A Modular Tool to Compute Tree Decompositions

Authors: [Max Bannach](http://www.tcs.uni-luebeck.de/de/mitarbeiter/bannach/), [Sebastian Berndt](http://www.tcs.uni-luebeck.de/de/mitarbeiter/berndt/), and [Thorsten Ehlers](http://www.zs.informatik.uni-kiel.de/de/mitarbeiter)

# About
Jdrasil is a tool to compute tree decompositions of simple, undirected graphs. It was developed for the first Parameterized Algorithms and Computational Experiments Challenge [(PACE)](https://pacechallenge.wordpress.com). It provides exact sequential and parallel, as well as heuristic algorithms.

Jdrasil is build in a very modular fashion. This allows researchers to simply add new algorithms, heuristics, or preprocessing routines, which can then be combined in any way.

# Installation
For an installation that fits the PACE requirements change the path to
the java compiler, the c++ compiler, the java directory containing `jni.h` and the java
executable in the first four lines of `Makefile`.
Afterwards you can simply execute the following:
```
cd jdrasil
make
```
The programs `tw-exact`, `tw-heuristic`, `tw-exact-parallel`, `tw-heuristic-parallel` should now work. You can use them as follows:
```
tw-exact < graph.gr
tw-exact -s seed < graph.gr
```
For a description of the graph file format, see the [PACE website](https://pacechallenge.wordpress.com/track-a-treewidth/).

# Maven
If you wish to use Jdrasil as Maven project, you have to build the C++ dependecies first.
```
make glucose
make pblib
```
Now you can do the "usual Maven stuff" in the folder containing `pom.xml`.
