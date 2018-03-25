package jdrasil.algorithms;

import jdrasil.algorithms.exact.CatchAndGlue;
import jdrasil.algorithms.lowerbounds.MinorMinWidthLowerbound;
import jdrasil.algorithms.preprocessing.GraphReducer;
import jdrasil.algorithms.upperbounds.PaceGreedyDegreeDecomposer;
import jdrasil.algorithms.upperbounds.StochasticGreedyPermutationDecomposer;
import jdrasil.graph.Graph;
import jdrasil.graph.TreeDecomposer;
import jdrasil.graph.TreeDecomposition;
import jdrasil.utilities.logging.JdrasilLogger;

import java.util.logging.Logger;

/**
 * The SmartDecomposer class is supposed to make it easy to compute a tree-decomposition  ``without mind'' what is actually
 * happening. In more detail, this class will analyse the graph with some heuristics and, upon the result of this analysis,
 * decide if it should solve the tree-width problem exactly or heuristically. In the same way, the class will decide for
 * every graph individually if it should apply preprocessing or not.
 *
 * @author Max Bannach
 */
public class SmartDecomposer<T extends Comparable<T>> implements TreeDecomposer<T> {

    /** Jdrasils Logger */
    private final static Logger LOG = Logger.getLogger(JdrasilLogger.getName());

    /** Minimum number of vertices that have to be in the graph to make us use preprocessing. */
    private final int PREPROCESSING_MIN = 20;

    /** Maximum number of vertices that have to be in the graph to make us use preprocessing. */
    private final int PREPROCESSING_CAP = 1000;

    /** Maximum number of vertices that have to be in the graph to make us solve the problem exactly. */
    private final int EXACT_CAP = 600;

    /** Maximum number of vertices that an atom can have to solve it exactly. */
    private final int ATOM_EXACT_CAP = 40;

    /**
     * The graph that will be decomposed.
     */
    private final Graph<T> graph;

    /**
     * Initialize some internal data structures.
     * @param graph
     */
    public SmartDecomposer(Graph<T> graph) {
        this.graph = graph;
    }


    /**
     * Heuristically checks if it is worth to apply preprocessing to the graph.
     * @return
     */
    private boolean shouldPreprocess() {
        if (graph.getNumVertices() < PREPROCESSING_MIN) return false;
        if (graph.getNumVertices() > PREPROCESSING_CAP) return false;
        return true;
    }

    /**
     * Heuristically checks if we should be able to solve the problem exactly.
     * @return
     */
    private boolean shouldSolveExactly() {
        if (graph.getNumVertices() > EXACT_CAP) return false;
        return true;
    }

    @Override
    public TreeDecomposition<T> call() throws Exception {

        /* To it without preprocessing! */
        if (!shouldPreprocess()) {
            if (shouldSolveExactly()) return new CatchAndGlue<>(graph).call();
            return new StochasticGreedyPermutationDecomposer<>(graph).call();
        }

        /*  Solve with preprocessing. */
        GraphReducer<T> reducer = new GraphReducer<>(graph);
        Graph H = reducer.getProcessedGraph();
        if (H.getNumVertices() == 0) return reducer.getTreeDecomposition(); // already done
        int lb = Math.max(4, new MinorMinWidthLowerbound<>(H).call());
        GraphSplitter<T> splitter = new GraphSplitter<T>(H, atom -> {
            try {
                if (shouldSolveExactly() && atom.getNumVertices() < ATOM_EXACT_CAP) return new CatchAndGlue<T>(atom).call();
                return new StochasticGreedyPermutationDecomposer<>(atom).call();
            } catch (Exception e) {
                LOG.warning(e.getMessage());
                return null;
            }
        }, lb);
        splitter.setTargetConnectivity(GraphSplitter.Connectivity.ATOM);

        // glue to final decomposition
        reducer.addbackTreeDecomposition(splitter.call());
        return reducer.getTreeDecomposition();
    }

    @Override
    public TreeDecomposition<T> getCurrentSolution() {
        return null;
    }

    @Override
    public TreeDecomposition.TreeDecompositionQuality decompositionQuality() {
        return TreeDecomposition.TreeDecompositionQuality.Heuristic;
    }
}
