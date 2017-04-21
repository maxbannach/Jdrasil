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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A cut vertex or articulation point is a vertex whose removal disconnects the graph. If the graph is biconnected, no
 * such vertex exists. This class computes an arbitrary cut vertex using the algorithm from Hopcroft and Tarjan.
 *
 * The class allows to forbid some vertices in the graph, this can be used to compute 2 or 3 connected components as follows:
 * If the graph has not cut vertex, but the graph without some vertex \(v\in V\) has a cut vertex \(c\), then the pair
 * \(v,c\) is a 2-cut.
 *
 * @author Max Bannach
 */
public class CutVertex<T extends Comparable<T>> extends Invariant<T, T, Boolean> {

    /** Helper variable for the DFS */
    private int count;

    /** The cut vertex to be computed. */
    private T cutVertex;

    /** A set of forbidden vertices, i.e., vertices that will neither be a cut vertex nor be in any biconnected component. */
    private Set<T> forbidden;

    /**
     * Standard constructor just gets the graph in which the cut vertex is searched.
     * @param graph
     */
    public CutVertex(Graph<T> graph) {
        super(graph);
        this.forbidden = new HashSet<T>();
    }

    /**
     * We may specify a set of forbidden vertices (the first set, if one is given). These vertices are considered as
     * deleted in the graph, they will not be a cutVertex and not be in any biconnected component.
     * @param graph
     */
    public CutVertex(Graph<T> graph, Set<T> forbidden) {
        super(graph);
        this.forbidden = forbidden;
    }

    /**
     * This method computes a cut vertex (aka articulation point) of the given graph using the algorithm of
     * Hopcroft and Tarjan. This implementation uses \(O(V+E)\) time, but will not output all components, but just
     * the first cut vertex it finds.
     *
     * @return
     */
    private T getCutVertex() {
        // search an arbitrary start vertex that is not forbidden
        T s = null;
        for (T v : graph.getCopyOfVertices()) {
            if (!forbidden.contains(v)) s = v;
        }
        if (s == null) return null; // empty graph

        // start algorithm
        count = 0;
        return getCutVertex(s, s, new HashMap<T, Integer>(), new HashMap<T, Integer>());
    }

    /**
     * Implementation of @see jdrasil.graph.invariants.CutVertex#getCutVertex()
     * @param u
     * @param v
     * @param low
     * @param depth
     * @return
     */
    private T getCutVertex(T u, T v, Map<T, Integer> low, Map<T, Integer> depth) {

        // previsit
        count++;
        int nChildren = 0;
        low.put(v, count);
        depth.put(v, count);

        // traversal
        for (T w : graph.getNeighborhood(v)) {
            if (forbidden.contains(w)) continue; // ignore forbidden vertices
            if (!depth.containsKey(w)) { // unvisited
                nChildren++;
                T tmp = getCutVertex(v, w, low, depth);
                if (tmp != null) return tmp; // already found one
                low.put(v, Math.min(low.get(v), low.get(w)));
                if (low.get(w) >= depth.get(v) && u != v) return v; // we found a cut vertex
            } else if (w != u && depth.get(w) < depth.get(v)) { // if we w is not the parent, update low
                low.put(v, Math.min(low.get(v), depth.get(w)));
            }
        }

        // root of getCutVertex tree is only cut vertices if it has more then 1 child
        if (u == v && nChildren > 1) return v;

        // not cut vertex found
        return null;
    }

    @Override
    protected Map<T, Boolean> computeModel() {
        Map<T, Boolean> model = new HashMap<T, Boolean>();
        cutVertex = getCutVertex();
        if (cutVertex != null) model.put(cutVertex, true);
        return model;
    }

    @Override
    protected T computeValue() {
        return cutVertex;
    }

    @Override
    public boolean isExact() { return true; }

}
