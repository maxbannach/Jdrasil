package jdrasil.algorithms.lowerbounds;

import jdrasil.algorithms.preprocessing.Preprocessor;
import jdrasil.graph.Graph;
import jdrasil.graph.GraphFactory;
import jdrasil.graph.TreeDecomposition;
import jdrasil.graph.invariants.MinimalVertexSeparator;

import java.util.HashSet;
import java.util.Set;

/**
 * Computes the k-path-improved-graph of the given graph, that is, the graph that is obtained by repeatedly adding
 * edges between paris of non-adjacent vertices that have at least \(k\) vertex disjoint paths between them.
 *
 * Note that this will not change the tree width, if \(k-1\) is a known lower bound on the tree width of the graph.
 * See "Treewidth computations II. Lower bounds" by Bodlaender and Koster for details.
 */
public class PathImprovedGraph<T extends Comparable<T>> extends Preprocessor<T> {

    /** The value for which the graph is improved. */
    private int k;

    /**
     * The constructor just stores the graph and k to prepare the computation of the k-path-improved graph.
     * @param graph
     * @param k
     */
    public PathImprovedGraph(Graph<T> graph, int k) {
        super(graph);
        this.k = k;
    }

    @Override
    protected Graph<T> preprocessGraph() {
        // copy the graph as we will modify it
        Graph<T> improved = GraphFactory.copy(graph);

        // repeat while the graph does change
        boolean changed = true;
        while (changed) {
            changed = false;
            // consider all pairs of vertices
            for (T v : graph) {
                for (T w : graph) {
                    if (v.compareTo(w) <= 0) continue;    // consider edges just onece
                    if (graph.isAdjacent(v, w)) continue; // if the edge exisits we cant improve

                    // compute minimal vertex separator between v and w
                    Set<T> SA = new HashSet<T>();
                    SA.add(v);
                    Set<T> SB = new HashSet<T>();
                    SB.add(w);
                    Set<T> vertexSeparator = new MinimalVertexSeparator<T>(graph, graph.getVertices(), SA, SB, k - 1).getSeparatorAsSet();

                    // if the separator is larger then k-1, there are at least k vertex disjoint paths
                    if (vertexSeparator == null) {
                        improved.addEdge(v, w);
                        changed = true;
                    }
                }
            }
        }

        // done
        return improved;
    }

    @Override
    protected TreeDecomposition<T> computeTreeDecomposition() {
        // nothing to do, as we assume the operation is safe
        return processedTreeDecomposition;
    }
}
