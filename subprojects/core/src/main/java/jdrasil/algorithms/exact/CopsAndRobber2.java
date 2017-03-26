/*
 * Copyright (c) 2016-present, Max Bannach, Sebastian Berndt, Thorsten Ehlers
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT
 * OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package jdrasil.algorithms.exact;

import jdrasil.graph.*;
import jdrasil.utilities.logging.JdrasilLogger;

import java.util.*;
import java.util.logging.Logger;

/**
 * The tree width of a graph can be characterized from game theoretic point of view with the game of cops and robber.
 * This class implements this game from "the point of view of cleared subgraphs", that is, we do not hash positions of cops,
 * but already cleared areas (in which we can "see" where cops have to stand).
 * The implementation is based on the description in "Nondeterministic Graph Searching: From Pathwidth to Treewidth"
 * by Fomin, Fraigniaud and Nisse.
 *
 * @param <T> vertex type of the graph
 * @author Max Bannach
 */
public class CopsAndRobber2<T extends Comparable<T>> implements TreeDecomposer<T> {

    /** Jdrasils Logger */
    private final static Logger LOG = Logger.getLogger(JdrasilLogger.getName());

    /** The graph we wish to decomposed as array of BitSets */
    private BitSetGraph<T> graph;

    /** Number of vertices of the graph we decompose. */
    private int n;

    /** Hash map used for dynamic programming over subgraphs. */
    private Map<BitSet, Boolean> memory;

    /** Winning strategy of the cops, used to extract the tree decomposition. */
    private Map<BitSet, List<BitSet>> strategy;

    /**
     * The constructor will initialize data structures and translate the given graph into a BitSetGraph.
     * @param graph
     */
    public CopsAndRobber2(Graph<T> graph) {
        this.graph = new BitSetGraph<T>(graph);
        this.n = this.graph.getN();
        this.memory = new HashMap<>();
        this.strategy = new HashMap<>();
    }

    /**
     * Tries to decompose the graph (clear it / catch the robber) from the configuration in which S is already secured.
     * In particular, it is assumed that the cops stand on the interior border of S.
     * @param S
     * @param k
     * @return
     */
    private boolean decompose(BitSet S, int k) {
        if (memory.containsKey(S)) return memory.get(S); // cashed
        strategy.put(S, new LinkedList<>());
        if (S.cardinality() == n) { // catched the robber / cleaned the graph / done
            memory.put(S, true);
            return true;
        }
        boolean result = false;

        // the next move depends on the number of free cops we have
        BitSet delta = graph.interiorBorder(S);
        if (delta.cardinality() < k) {
            // we have a free cop, make an existential step
            for (int v = 0; v < n; v++) {
                if (S.get(v)) continue; // already cleared
                BitSet newS = (BitSet) S.clone();
                newS.set(v);
                result |= decompose(newS, k);
                if (result) {
                    strategy.get(S).add(newS);
                    break;
                }
            }
        } else {
            // we have no free cop, we have to make an universal step and use S as separator
            List<BitSet> components = graph.separate(S);
            if (components.size() > 1) { // if we have not more then one component we lost
                result = true; // it has to work for all, so we invert the logic
                for (BitSet component : components) {
                    // "guess" the component the robber is in by marking everything else safe
                    BitSet mask = new BitSet();
                    mask.set(0,n);
                    mask.andNot(component);
                    result &= decompose(mask, k);
                    strategy.get(S).add(mask);
                    if (!result) break;
                }
                if (!result) strategy.get(S).clear();
            }
        }

        // done, store result in memory and return
        memory.put(S, result);
        if (result == false) strategy.put(S, null); // safe some space
        return result;
    }

    /**
     * Initial call method for @see jdrasil.algorithms.exact.CopsAndRobber2#decompose(java.util.BitSet, int)
     * @param k
     * @return
     */
    private boolean decompose(int k) {
        memory.clear();
        strategy.clear();
        return decompose(new BitSet(), k);
    }

    /**
     * Extract a tree decomposition from a winning strategy of the cops.
     * Should be called after a run of @see decompose that has returned true
     * @param S
     * @param td
     * @return
     */
    private Bag<T> extractTreeDecomposition(BitSet S, TreeDecomposition<T> td) {
        Bag<T> bag = td.createBag(graph.getVertexSet(graph.interiorBorder(S)));
        int n_childs = strategy.get(S).size();
        for (BitSet child : strategy.get(S)) {
            if (n_childs == 1) {
                // create an intermediate bag bag(S) -> bag(S cut child) -> bag(child)
                BitSet cut = (BitSet) child.clone();
                cut.andNot(S);
                cut.or(graph.interiorBorder(S));
                Bag<T> cutBag = td.createBag(graph.getVertexSet(cut));
                td.addTreeEdge(bag, cutBag);
                // create the child bag
                td.addTreeEdge(cutBag, extractTreeDecomposition(child, td));
            } else {
                // if this is a branch node, just add the children
                td.addTreeEdge(bag, extractTreeDecomposition(child, td));
            }
        }
        return bag;
    }

    @Override
    public TreeDecomposition<T> call() throws Exception {

        int k = 1;
        while (!decompose(k)) {
            LOG.info("tree width >= " + k);
            k++;
        }
        LOG.info("tree width == " + k);
        TreeDecomposition<T> td = new TreeDecomposition<T>(graph.getGraph());
        extractTreeDecomposition(new BitSet(), td);
        return td;
    }

    @Override
    public TreeDecomposition<T> getCurrentSolution() { return null; }

    @Override
    public TreeDecomposition.TreeDecompositionQuality decompositionQuality() { return TreeDecomposition.TreeDecompositionQuality.Exact; }
}
