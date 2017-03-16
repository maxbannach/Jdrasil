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

package jdrasil.algorithms.lowerbounds;

import jdrasil.graph.Graph;
import jdrasil.graph.GraphFactory;
import jdrasil.utilities.RandomNumberGenerator;

import java.util.*;

/**
 * The {k-neighbor / k-path}-improved graph \(H\) of a given graph \(G\) is obtained by adding an edge between all
 * non adjacent vertices that have at least k {common neighbors / vertex disjoint paths}. A crucial lemma states that
 * adding these edges will not increase the tree width. Hence, we can compute a lower bound on the tree width of
 * \(G\) by computing lower bounds on increasing improved graphs of \(G\). In this way, improved graphs can be used
 * to improve the performance of any lower bound algorithm.
 *
 * This class implements the improve graph trick to improve some implemented lower bound algorithms. The implementation
 * is based on Bodlaender and Koster: Treewidth Computations II
 *
 * @author Max Bannach
 */
public class ImprovedGraphLowerbound<T extends Comparable<T>> implements Lowerbound<T> {

    /** The currently best lower bound */
    private int low;

    /** The graph for which the lower bound is computed */
    private Graph<T> graph;

    /** The Improved-Graph lower bound is based on other lower bounds */
    enum Algorithm {
        Degeneracy,
        MinorMinWidth;
    }

    /** The lower bound algorithm used during the computation. */
    private Algorithm toRun;

    /** The algorithm can be combined with contraction, this flag indicates if we do so. */
    private boolean contraction;

    /**
     * Standard constructor, just with the graph that should be decomposed.
     * This will set the used algorithm to MinorMinWidth.
     * @param graph
     */
    public ImprovedGraphLowerbound(Graph<T> graph) {
        this.graph = graph;
        setToRun(Algorithm.MinorMinWidth);
        setContraction(false);
        this.low = 0;
    }

    /**
     * Get a lowerbound for the given graph with the selected algorithm.
     * @param graph
     * @return a lowerbound on the given graph
     * @throws Exception
     */
    private int getLowerbound(Graph<T> graph) throws Exception {
        Lowerbound<T> lowerbound = null;
        switch(toRun) {
            case Degeneracy:
                lowerbound = new DegeneracyLowerbound<T>(graph);
                break;
            case MinorMinWidth:
                lowerbound = new MinorMinWidthLowerbound<T>(graph);
                break;
        }
        return lowerbound.call();
    }

    /**
     * Computes the k-neighbor-improved-graph of the given graph, that is, the graph that contains a new edge between
     * each pair of non adjacent vertices with at least k common neighbors. This method will modify the input graph.
     * @param G
     * @param k
     * @return the k-neighbors-imrpoved graph of G
     */
    private Graph<T> getNeighborImprovedGraph(Graph<T> G, int k) {

        // we store the edges that we are allowed to add
        List<T> edgesToAdd = new ArrayList<T>();

        // find all vertices that are non adjacent and have at least k common neighbors
        for (T v : G) {
            for (T w : G) {
                if (v.compareTo(w) >= 0) continue;
                if (G.isAdjacent(v, w)) continue;

                // compute common neighbors
                Set<T> common = new HashSet<>();
                common.addAll(G.getNeighborhood(v));
                common.retainAll(G.getNeighborhood(w));

                // if there at least k, we can add the edge
                if (common.size() >= k) {
                    edgesToAdd.add(v);
                    edgesToAdd.add(w);
                }
            }
        }

        // actually add all the edges that we found
        for (int i = 0; i < edgesToAdd.size()-1; i += 2) {
            G.addEdge(edgesToAdd.get(i), edgesToAdd.get(i+1));
        }

        // done
        return G;
    }

    @Override
    public Integer call() throws Exception {
        int tmp = getLowerbound(graph);
        if (tmp > low) low = tmp;

        // try to improve the lower bound via improved graphs
        Graph<T> H = GraphFactory.copy(graph);
        while (true) {
            H = getNeighborImprovedGraph(H, low+1);
            tmp = getLowerbound(H);

            // contract a safe minor, as in the minor-min-width heuristic
            if (contraction) {
                while (tmp <= low && H.getNumberOfEdges() >= 1) {
                    // search vertex of min degree
                    int min = Integer.MAX_VALUE;
                    List<T> nextV = new LinkedList<>();
                    for (T v : H) {
                        int deg = H.getNeighborhood(v).size();
                        if (deg < min) {
                            min = deg;
                            nextV.clear();
                            nextV.add(v);
                        } else if (deg == min) { // break ties randomly
                            nextV.add(v);
                        }
                    }
                    T v = nextV.size() > 0 ? nextV.get(RandomNumberGenerator.nextInt(nextV.size())) : null;

                    // search neighbor with minimum number of common neighbors
                    List<T> nextU = new LinkedList<T>();
                    min = Integer.MAX_VALUE;
                    for (T u : H.getNeighborhood(v)) {
                        Set<T> common = new HashSet<>();
                        common.addAll(H.getNeighborhood(v));
                        common.retainAll(H.getNeighborhood(u));
                        int k = common.size();
                        if (k < min) {
                            min = k;
                            nextU.clear();
                            nextU.add(u);
                        } else if (k == min) {
                            nextU.add(u);
                        }
                    }
                    T u = nextV.size() > 0 ? nextU.get(RandomNumberGenerator.nextInt(nextU.size())) : null;

                    // contract and update lower bound
                    H.contract(v, u);
                    tmp = getLowerbound(H);
                }
            }

            if (tmp <= low) break; // no improvement, we can stop
            low = low + 1;
        }

        // done
        return this.low;
    }

    @Override
    public Integer getCurrentSolution() {
        return low;
    }

    /**
     * Set the lower bound algorithm used by this class.
     * @param toRun
     */
    public void setToRun(Algorithm toRun) { this.toRun = toRun; }

    /**
     * Get the lower bound algorithm used by this class.
     * @param toRun
     * @return
     */
    public Algorithm getToRun(Algorithm toRun) { return this.toRun; }

    /**
     * The algorithm can alternate between the heuristic and contraction. Set this to true to enable contraction.
     * (Default: false)
     * @param contraction
     */
    public void setContraction(boolean contraction) { this.contraction = contraction; }
}
