package jdrasil.algorithms.preprocessing;

import jdrasil.graph.Graph;
import jdrasil.graph.GraphFactory;
import jdrasil.graph.TreeDecomposition;

import java.util.HashSet;
import java.util.Set;

/**
 * Computes the k-neighbor-improved-graph of the given graph, that is, the graph that is obtained by repeatedly adding
 * edges between paris of non-adjacent vertices that have at least \(k\) common neighbors.
 *
 * Note that this operation is not necessarily safe. We have, however, \(\mathrm{tw}(G)\leq k\Leftrightarrow\mathrm{tw}(H)\leq k\)
 * where \(H\) is the \((k+1)\)-improved graph of \(G\).
 * See "Treewidth computations II. Lower bounds" by Bodlaender and Koster for details.
 */
public class NeighborImprovedGraph<T extends Comparable<T>> extends Preprocessor<T> {

    /** The value for which the graph is improved. */
    private int k;

    /**
     * The constructor just stores the graph and k to prepare the computation of the k-neighbor-improved graph.
     * @param graph
     * @param k
     */
    public NeighborImprovedGraph(Graph<T> graph, int k) {
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
            for (T v : improved) {
                for (T w : improved) {
                    if (v.compareTo(w) >= 0) continue;    // consider edges just onece
                    if (improved.isAdjacent(v, w)) continue; // if the edge exisits we cant improve

                    // compute common neighbors
                    Set<T> common = new HashSet<>();
                    common.addAll(improved.getNeighborhood(v));
                    common.retainAll(improved.getNeighborhood(w));

                    // if there at least k, we can add the edge
                    if (common.size() >= k) {
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
