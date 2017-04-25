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
package jdrasil.graph.invariants;

import jdrasil.graph.Graph;
import jdrasil.graph.GraphFactory;

import java.util.*;

/**
 * Computes a clique minimal separator of the graph, that is, a set \(S\subseteq V\) such that \(G[S]\) is a clique,
 * \(G[V\setminus S]\) has more components then G, and such that \(S\) is a minimal separator for some vertices \(a,b\in V\).
 *
 * This class takes \(O(nm)\) time and implements the algorithm described in "An Introduction to Clique Minimal Separator Decomposition"
 * by Berry et al.
 * In short, it does the following:
 * a) compute a minimal triangulation of the graph
 * b) find minimal separators of the triangulation
 * c) check which of these separators are cliques in the graph
 *
 * The class allows to specify a set of forbidden vertex, which are assumed to be not part of the graph then. In this way,
 * the invariant can be used to compute almost clique minimal separators: take a vertex \(v\in V\) and remove it, if the
 * resulting graph has a clique minimal separator \(S\), then \(S\cup\{v\}\) is an almost clique minimal separator.
 *
 * @author Max Bannach
 */
public class CliqueMinimalSeparator<T extends Comparable<T>> extends Invariant<T, Integer, Boolean> {

    /** The clique minimal separator we wish to compute */
    private Set<T> cliqueMinimalSeparator;

    /** Set of forbidden vertices, i.e., vertices which are ignored in the graph. */
    private Set<T> forbidden;

    /**
     * Standard constructor just gets the graph in which the separator is searched.
     * @param graph
     */
    public CliqueMinimalSeparator(Graph<T> graph) {
        super(graph);
        this.forbidden = new HashSet<T>();
    }

    /**
     * We may specify a set of forbidden vertices (the first set, if one is given). These vertices are considered as
     * deleted in the graph, they will not be part of the separator nor in any component.
     * @param graph
     */
    public CliqueMinimalSeparator(Graph<T> graph, Set<T> forbidden) {
        super(graph);
        this.forbidden = forbidden;
    }

    /**
     * Implementation of the algorithm described in Berry et al.
     * @return
     */
    private Set<T> getCliqueMinimalSeparator() {

        // create a copy of the graph (will be modifed)
        Graph<T> G = GraphFactory.copy(graph);
        for (T v : forbidden) G.removeVertex(v);
        // triangulation of G
        Graph<T> H = GraphFactory.copy(graph);

        // minimal elimination order
        List<T> alpha = new ArrayList<T>(graph.getCopyOfVertices().size());

        // set of generators of minimal separators in H
        Set<T> X = new HashSet<T>();

        // initialize labels for all vertices
        Map<T, Integer> label = new HashMap<T, Integer>();
        for (T v : G) label.put(v, 0);
        int s = -1;

        // compute the elimination order
        for (int n = graph.getCopyOfVertices().size()-forbidden.size(), i = 0; i < n; i++) {
            // choose a vertex with minimal label
            T x = G.getCopyOfVertices().stream().max( (a,b) -> Integer.compare(label.get(a), label.get(b))).get();
            Set<T> Y = new HashSet<T>(G.getNeighborhood(x));

            // may add x to the generators
            if (label.get(x) <= s) {
                X.add(x);
            }
            s = label.get(x);

            // mark x as reached and all other vertices as unreached
            Set<T> reached = new HashSet<T>();
            reached.add(x);
            Map<Integer, Set<T>> reach = new HashMap<>();
            for (int j = 0; j < n; j++) reach.put(j, new HashSet<T>());

            // reach N(x) in G'
            for (T y : G.getNeighborhood(x)) {
                reached.add(y);
                reach.get(label.get(y)).add(y);
            }

            // compute reached vertices
            for (int j = 0; j < n; j++) {
                while (!reach.get(j).isEmpty()) {
                    // remove a vertex from reach(j)
                    T y = reach.get(j).iterator().next();
                    reach.get(j).remove(y);
                    for (T z : G.getNeighborhood(y)) {
                        if (reached.contains(z)) continue; // only consider unreached vertices
                        reached.add(z);
                        if (label.get(z) > j) {
                            Y.add(z);
                            reach.get(label.get(z)).add(z);
                        } else {
                            reach.get(j).add(z);
                        }
                    }
                }
            }

            // add triangulation edges to H
            for (T y : Y) {
                H.addEdge(x, y);
                label.put(y, label.get(y) + 1);
            }

            // update elimination order
            alpha.add(0, x);
            G.removeVertex(x);
        }

        /* H now stores a triangulation of the graph and alpha a minimal elimination order.
         * Furthermore, X stores the set of vertices which generate a minimal separator of H.
         * We can, finally, use this data to find a clique minimal separator in the original graph
         */
        for (T x : alpha) {
            // x is in X it is a minimal separator in H and it may be a clique minimal separator in G
            if (X.contains(x)) {
                // separator in H
                Set<T> S = new HashSet<T>(H.getNeighborhood(x));
                // check if it is a clique in G
                boolean isClique = true;
                testClique: for (T a : S) {
                    for (T b : S) {
                        if (a.compareTo(b) >= 0) continue;
                        if (!graph.isAdjacent(a,b)) {
                            isClique = false;
                            break testClique;
                        }
                    }
                }
                // if S is a clique in G, it is a clique minimal separator in G -> return it
                if (isClique) return S;
            }
            // remove the vertex from H
            H.removeVertex(x);
        }

        // no clique minimal separator found
        return null;
    }

    @Override
    protected Map<T, Boolean> computeModel() {
        Map<T, Boolean> model = new HashMap<T, Boolean>();
        cliqueMinimalSeparator = getCliqueMinimalSeparator();
        for (T v : graph) model.put(v, (cliqueMinimalSeparator != null && cliqueMinimalSeparator.contains(v)) );
        return model;
    }

    @Override
    protected Integer computeValue() {
        return cliqueMinimalSeparator != null ? cliqueMinimalSeparator.size() : 0;
    }

    @Override
    public boolean isExact() { return false; }

    /**
     * Getter for the actual clique minimal separator.
     * @return
     */
    public Set<T> getSeparator() {
        if (getValue() == 0) return null; // also invokes eventual computation
        return cliqueMinimalSeparator;
    }
}
