package jdrasil.algorithms.postprocessing;

import jdrasil.graph.TreeDecomposition;

/**
 * A Postprocessor is a function that maps a given arbitrary tree decomposition to another tree decomposition of the same graph.
 * For instance a Postprocessor may ensure that the decomposition has certain properties (i.\,g., is a \emph{nice}
 * tree decomposition), but a Postprecessor may also \emph{improve} an non-optimal decomposition.
 *
 * @author Max Bannach
 */
public abstract class Postprocessor<T extends Comparable<T>> {

    /**
     * The tree decomposition that shall be processed, i.\,e., the input.
     */
    protected final TreeDecomposition<T> treeDecomposition;

    /**
     * The processed tree decomposition, i.\,e., the tree decomposition \emph{after} the postprocessing. This may be a
     * pointer to the same object as @see treeDecomposition (if the postprocessing was performed inplace), or may be a
     * new object.
     */
    protected TreeDecomposition<T> processedTreeDecomposition;

    /**
     * The constructor just initialize some internal data structures and stores the tree decomposition that should
     * be postprocessed.
     *
     * @param treeDecomposition The tree decomposition to be postprocessed.
     */
    public Postprocessor(TreeDecomposition<T> treeDecomposition) {
        this.treeDecomposition = treeDecomposition;
        this.processedTreeDecomposition = null;
    }

    /**
     * This method computes the actual postprocessing. It shall use @see treeDecomposition and shall return a postprocessed
     * version of it. This may the same reference if the postprocessing is performed inplace.
     *
     * @return A postprocessed version of the given tree decomposition.
     */
    protected abstract TreeDecomposition<T> postprocessTreeDecomposition();

    /**
     * Obtain the postprocessed tree decomposition. If this was not computed yet, this method will
     * call @see postprocessTreeDecomposition.
     *
     * @return A postprocessed version of the given tree decomposition.
     */
    public TreeDecomposition<T> getProcessedTreeDecomposition() {
        if (this.processedTreeDecomposition == null) this.processedTreeDecomposition = postprocessTreeDecomposition();
        return processedTreeDecomposition;
    }
}
