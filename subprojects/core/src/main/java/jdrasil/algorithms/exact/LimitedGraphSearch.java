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
 * The tree width of a graph can be characterized from a game theoretic point of view with the graph search game where
 * a team of searchers tries to catch a fugitive, who has unlimited speed. Depending on the visibility of the fugitive, a winning strategy of
 * the searchers corresponds to a path- or tree-decomposition of the graph.
 * Fomin, Fraigniaud and Nisse described a mixed version of the game in "Nondeterministic Graph Searching: From Pathwidth to Treewidth".
 * Here, the fugitive is invisible, but he searchers may reveal her from time to time. By fixing the number \(q\) of reveals
 * that are performed by the searchers, the game covers path decompositions (\(q=0\)) and tree decompositions (\(q=\infty\)),
 * and, in particular, the area between these two extrema. In the cited paper such decompositions are called q-branched
 * tree decompositions and they have, loosely speaking, the property that on each path from a root to the leafs there are at
 * most \(q\) nodes with more then one children, i.e., at most \(q\) branching nodes.
 *
 *
 * @param <T> vertex type of the graph
 * @author Max Bannach
 */
public class LimitedGraphSearch<T extends Comparable<T>> implements TreeDecomposer<T> {

    /** Jdrasils Logger */
    private final static Logger LOG = Logger.getLogger(JdrasilLogger.getName());

    /** a good approximation of infinity ... */
    private final int INFINITY = Integer.MAX_VALUE/2;

    /** The graph we wish to decomposed as array of BitSets */
    private BitSetGraph<T> graph;

    /** Number of vertices of the graph we decompose. */
    private int n;

    /** Hash map used for dynamic programming over subgraphs. */
    private Map<BitSet, Integer> label;

    /** Winning strategy of the searchers, used to extract the tree decomposition. */
    private Map<BitSet, List<BitSet>> strategy;

    /** The number of reveals we wish to use (we will compute smallest k that can use no more reveals) */
    private int reveals;

    /**
     * The constructor will initialize data structures and translate the given graph into a BitSetGraph.
     * @param graph
     */
    public LimitedGraphSearch(Graph<T> graph) {
        this(graph, graph.getCopyOfVertices().size()+1);
    }

    /**
     * The constructor will initialize data structures and translate the given graph into a BitSetGraph.
     * The q is the number of reveals the searcher are allowed to berform in the q-limited graph search game, i.e.,
     * a q-branched tree decomposition is computed.
     * @param graph
     * @param q
     */
    public LimitedGraphSearch(Graph<T> graph, int q) {
        this.graph = new BitSetGraph<T>(graph);
        this.n = this.graph.getN();
        this.label = new HashMap<>();
        this.strategy = new HashMap<>();
        this.reveals = q;
    }

    /**
     * Tries to decompose the graph from the configuration in which S is already cleaned.
     * In particular, it is assumed that the searchers stand on the interior border of S.
     * @param S
     * @param k
     * @return
     */
    private int decompose(BitSet S, int k) {
        if (label.containsKey(S)) return label.get(S); // label of S was already computed
        strategy.put(S, new LinkedList<>());
        if (S.cardinality() == n) { // end configuration is labeled with 0
            label.put(S, 0);
            return 0;
        }
        int value = INFINITY; // default label is (almost) infinity

        // the next move depends on the number of free searchers we have
        BitSet delta = graph.interiorBorder(S);
        if (delta.cardinality() < k) {
            // we have a free searcher, make an existential step
            for (int v = 0; v < n; v++) {
                if (S.get(v)) continue; // already cleared
                BitSet newS = (BitSet) S.clone();
                newS.set(v);
                int tmp = decompose(newS, k);
                if (tmp < value) {
                    value = tmp;
                    strategy.get(S).clear();
                    strategy.get(S).add(newS);
                }
            }
        } else {
            // we have no free searcher, we have to make an universal step and use S as separator
            List<BitSet> components = graph.separate(S);
            if (components.size() > 1) { // if we have not more then one component we lost
                int tmp = 0; // it has to work for all, so we invert the logic
                for (BitSet component : components) {
                    // reveal the component the searcher is in by marking everything else safe
                    BitSet mask = new BitSet();
                    mask.set(0,n);
                    mask.andNot(component);
                    tmp = Math.max(tmp, decompose(mask, k));
                    strategy.get(S).add(mask);
                    if (tmp == INFINITY) break;
                }
                tmp = tmp + 1;
                if (tmp < value) value = tmp;
            }
        }

        // done, store result in label and return
        label.put(S, value);
        if (value == INFINITY) strategy.put(S, null); // safe some space
        return value;
    }

    /**
     * Initial call method for @see jdrasil.algorithms.exact.LimitedGraphSearch#decompose(java.util.BitSet, int)
     * @param k
     * @return
     */
    private boolean decompose(int k) {
        label.clear();
        strategy.clear();
        return decompose(new BitSet(), k) <= reveals;
    }

    /**
     * Extract a tree decomposition from a winning strategy of the searchers.
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
            LOG.info("limited search number >= " + k);
            k++;
        }
        LOG.info("limited search number == " + k);
        TreeDecomposition<T> td = new TreeDecomposition<T>(graph.getGraph());
        extractTreeDecomposition(new BitSet(), td);
        return td;
    }

    @Override
    public TreeDecomposition<T> getCurrentSolution() { return null; }

    @Override
    public TreeDecomposition.TreeDecompositionQuality decompositionQuality() { return TreeDecomposition.TreeDecompositionQuality.Exact; }
}
