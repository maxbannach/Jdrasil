/*
 * Copyright (c) 2016-2017, Max Bannach, Sebastian Berndt, Thorsten Ehlers
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS
 * OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package jdrasil.algorithms.approximation;

import com.sun.net.httpserver.Authenticator;
import jdrasil.graph.*;
import jdrasil.graph.invariants.MinimalVertexSeparator;
import jdrasil.utilities.logging.JdrasilLogger;

import java.util.*;
import java.util.logging.Logger;

/**
 *
 * This class uses the standard FPT approximation algorithm for tree width based on the work of Robertson and Seymour.
 * The algorithm assumes that the graph is connected and computes in time \(O(8^k k^2 \cdot n^2)\) a tree decomposition
 * of width at most \(4k+4\).
 *
 * A detailed explanation of the algorithms can be found in many text books about FPT, for instance in
 * "Parameterized Algorithms" by Cygan et al., or in "Parameterized Complexity Theory" by Flum and Grohe.
 * *
 * @author Max Bannach
 */
public class RobertsonSeymourDecomposer<T extends Comparable<T>> implements TreeDecomposer<T> {

    /** Jdrasils Logger */
    private final static Logger LOG = Logger.getLogger(JdrasilLogger.getName());

    /** The graph that we wish to decompose. */
    private final Graph<T> graph;

    /** The tree decomposition that is computed. */
    private TreeDecomposition<T> decomposition;

    /** Bijection from V to {0,...,n-1} */
    private final Map<T, Integer> vertexToInt;
    private final Map<Integer, T> intToVertex;

    /** A lower bound on the tree width of the graph. */
    private int lb;

    /** An upper bound on the tree width of the graph. */
    private int ub;

    /** Number of vertices of the input graph. */
    private final int n;

    /** Number of edges of the input graph. */
    private final int m;

    /**
     * Default constructor to initialize the algorithm.
     * @param graph to be decomposed
     */
    public RobertsonSeymourDecomposer(Graph<T> graph) {
        this.graph = graph;
        this.n = graph.getCopyOfVertices().size();
        this.m = graph.getNumberOfEdges();
        this.lb = 1;
        this.ub = n;
        this.vertexToInt = new HashMap<>();
        this.intToVertex = new HashMap<>();
        int i = 0;
        for (T v : graph) {
            vertexToInt.put(v, i);
            intToVertex.put(i,v);
            i = i+1;
        }
    }

    /**
     * Default constructor to initialize the algorithm.
     * @param graph to be decomposed
     * @param lb known lower bound on the tree width
     */
    public RobertsonSeymourDecomposer(Graph<T> graph, int lb) {
        this(graph);
        this.lb = lb;
    }

    /**
     * The method decompose tries to decompose the the graph \(G[W]\) (which is a subgraph of the input graph),
     * such that the resulting tree decomposition has a bag that contains \(S\).
     *
     * The method assumes that \(S\subset W\subseteq V\), \(|S|\leq 3k+4\), \(W\setminus S=\noteq\emptyset\).
     *
     * @param W current subgraph
     * @param S bag in construction
     * @param k target tree width
     */
    private Bag<T> decompose(BitSet W, BitSet S, int k) {

        // single bag is a valid solution (and a leaf)
        if (W.cardinality() < k) {
            Set<T> bag = new HashSet<T>();
            for (int u = W.nextSetBit(0); u >= 0; u = W.nextSetBit(u+1))
                bag.add(intToVertex.get(u));
            return decomposition.createBag(bag);
        }

        // create a superset of S
        BitSet newS = (BitSet) S.clone();

        // if the current bag is small enough, just add a vertex
        if (S.cardinality() < 3*k+4) {

            // find a vertex u in W\S
            for (int u = W.nextSetBit(0); u >= 0; u = W.nextSetBit(u+1)) {
                if (S.get(u)) continue;
                newS.set(u);
                break;
            }

        } else { // otherwise we have to split the bag

            // store vertices of S as array
            int[] arrayS = new int[3*k+4];
            int i = 0;
            for (int v = S.nextSetBit(0); v >= 0; v = S.nextSetBit(v+1)) {
                arrayS[i++] = v;
            }

            // check all partitions of S
            BitSet partition = new BitSet();
            while (partition.cardinality() < 3*k+4) {
                // compute next partition
                int t = partition.nextClearBit(0);
                for (int j = 0; j <= t; j++) partition.flip(j);

                // check partion
                if (partition.cardinality() < k+2) continue;
                if (partition.cardinality() > 2*k+2) continue;

                // check if the partion can be seperated
                BitSet SA = new BitSet();
                BitSet SB = new BitSet();
                for (int j = 0; j < arrayS.length; j++) {
                    if (partition.get(j)) {
                        SA.set(arrayS[j]);
                    } else {
                        SB.set(arrayS[j]);
                    }
                }

                // try to find a separator
                BitSet separator = new MinimalVertexSeparator<T>(graph, vertexToInt, intToVertex, W, SA, SB, k+1).getSeparatorAsBitSet();
                if (separator == null) continue;

                // found a separator, we can stop searching
                newS.or(separator);
                break;
            }

            // we found no separator for S, that means S is a well-linked set
            // and, hence, an obstacle for tree width k
            if (S.equals(newS)) return null;
        }

        // create the root bag (containing the new S)
        Set<T> bagVertices = new HashSet<T>();
        for (int v = newS.nextSetBit(0); v >= 0; v = newS.nextSetBit(v+1)) {
            bagVertices.add(intToVertex.get(v));
        }
        Bag<T> rootBag = decomposition.createBag(bagVertices);

        // go into the recursion
        for (BitSet D : getConnectedComponents(W, newS)) {
            BitSet DN = (BitSet) newS.clone();
            DN.and(D);
            Bag<T> childBag = decompose(D, DN, k);
            if (childBag == null) return null;
            decomposition.addTreeEdge(rootBag, childBag);
        }

        // done
        return rootBag;
    }

    /**
     * Compute the connected components of \(G[W\setminus X]\) and returns them as list of BitSets.
     * The connected components will contain the neighbors of \(G[W\setminus X]\) in \(X\).
     *
     * @param W the subgraph we are in
     * @param X the separator used
     * @return the connected components of \(G[W\setminus X]\)
     */
    private List<BitSet> getConnectedComponents(BitSet W, BitSet X) {

        // components of the graph
        List<BitSet> components = new LinkedList<>();

        // start dfs at each vertex in the subgraph
        BitSet visited = new BitSet();
        for (int s = W.nextSetBit(0); s >= 0; s = W.nextSetBit(s+1)) {
            if (visited.get(s)) continue;
            if (X.get(s)) continue;
            visited.set(s);
            BitSet cc = new BitSet();
            cc.set(s);
            Stack<Integer> S = new Stack<>();
            S.push(s);

            // single dfs
            while (!S.isEmpty()) {
                int v = S.pop();
                for (T x : graph.getNeighborhood(intToVertex.get(v))) {
                    int w = vertexToInt.get(x);
                    if (visited.get(w)) continue;
                    if (!W.get(w)) continue;
                    cc.set(w);
                    if (X.get(w)) continue;
                    visited.set(w);
                    S.push(w);
                }
            }
            components.add(cc);
        }

        // done
        return components;
    }

    /**
     * Returns a lower bound on the tree width of the graph.
     * After initialisation, this value will be 1 until
     * @see RobertsonSeymourDecomposer#call() was invoked. After this, the lower bound found by the algorithms is returned.
     * @return a lower bound on the tree width.
     */
    public int getLowerbound() {
        return lb;
    }

    /**
     * Returns a upper bound on the tree width of the graph.
     * After initialisation, this value will be n until
     * @see RobertsonSeymourDecomposer#call() was invoked. After this, the upper bound found by the algorithms is returned.
     * @return a lower bound on the tree width.
     */
    public int getUpperbound() {
        return ub;
    }

    @Override
    public TreeDecomposition<T> call() throws Exception {

        // vertices of the graph
        BitSet W = new BitSet();
        W.set(0, n);

        // increase k till we find a decomposition
        while (true) {
            LOG.info("Try to compute a tree decomposition of size: " + lb);

            if (m <= lb * n) { // for m > k*n we can safely increase the lb
                decomposition = new TreeDecomposition<T>(graph);
                if (decompose(W, new BitSet(), lb) != null) break;
            }

            lb = lb + 1;
        }
        ub = decomposition.getWidth();

        // done, decomposition stores the first successfully computed tree decomposition
        return decomposition;
    }

    @Override
    public TreeDecomposition<T> getCurrentSolution() {
        return decomposition;
    }

    @Override
    public TreeDecomposition.TreeDecompositionQuality decompositionQuality() {
        return TreeDecomposition.TreeDecompositionQuality.Approximation;
    }
}
