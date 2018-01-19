package jdrasil.algorithms.postprocessing;

import jdrasil.graph.TreeDecomposition;

/**
 * A \emph{very nice} tree-decomposition is a nice tree-decomposition that has additionally introduce-edge bags. Such
 * a bag is labeled with an edge and has exactly one child with the same content. The interpretation is, that introduce
 * bag only introduce the isolated vertex, while the edge first arises at the inroduce edge bag.
 *
 * Furthermore, every edge is introduced exactly once.
 *
 * @author Max Bannach
 */
public class VeryNiceTreeDecomposition<T extends Comparable<T>> extends NiceTreeDecomposition<T> {

    /**
     * The constructor just initialize some internal data structures and stores the tree-decomposition that should
     * be postprocessed.
     *
     * @param treeDecomposition The tree-decomposition to be postprocessed.
     */
    public VeryNiceTreeDecomposition(TreeDecomposition<T> treeDecomposition) {
        super(treeDecomposition);
    }

    @Override
    protected TreeDecomposition<T> postprocessTreeDecomposition() {
        super.postprocessTreeDecomposition(); // make it nice in the first place
        computeIntroduceEdgeBags();
        return treeDecomposition;
    }

    /**
     * Compute all introduce edge bags for the decomposition.
     * This will change the tree-decomposition and the labeling of bags function.
     * However, it will not change the tree-index.
     */
    private void computeIntroduceEdgeBags() {
    }

}
