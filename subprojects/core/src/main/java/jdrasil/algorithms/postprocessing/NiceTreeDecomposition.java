package jdrasil.algorithms.postprocessing;

import jdrasil.graph.Bag;
import jdrasil.graph.TreeDecomposition;

import java.util.Map;

/**
 * A \emph{nice} tree-decomposition is \emph{rooted} tree-decomposition in which each bag has one of the following types:
 *
 * \begin{enumerate}
 *  \item There is exactly one \emph{root} bag that stores an empty set.
 *  \item A \emph{leaf} bag has no children and stores and empty set.
 *  \item An \emph{introduce} bag has exactly one child bag and has the same content as its child up to one
 *    addional vertex (which was ``introduced'' at this bag.
 *  \item A \emph{forget} bag has exactly one child bag and contains all but one vertex of its child (it ``forgets''
 *    this vertex).
 *  \item A \emph{join} bag has two children which both have the same content as the join bag.
 * \end{enumerate}
 *
 * It is well known that to every tree-decomposition there can be found a nice tree-decomposition of the same width.
 * This class will perform this transformation for a given tree-decomposition. The modifiction will be performed
 * \emph{inplace}, i.\,e., the given tree-decomposition will be modified.
 *
 * @author Max Bannach
 */
public class NiceTreeDecomposition<T extends Comparable<T>> extends Postprocessor<T> {

    /**
     * The tree-index is a mapping $\phi\colon V\rightarrow\{0,\dots,\mathrm{tw}\}$ that maps every vertex of the graph
     * to an index such that no two vertex appearing in a bag share the same index. Therefore, the tree-index can be used
     * to index data structures when working on the tree-decomposition.
     */
    private Map<T, Integer> treeIndex;

    /**
     * The root bag of the nice tree-decomposition.
     */
    private Bag<T> root;

    /**
     * The constructor just initialize some internal data structures and stores the tree-decomposition that should
     * be postprocessed.
     *
     * @param treeDecomposition The tree-decomposition to be postprocessed.
     */
    public NiceTreeDecomposition(TreeDecomposition<T> treeDecomposition) {
        super(treeDecomposition);
    }

    @Override
    protected TreeDecomposition<T> postprocessTreeDecomposition() {
        Bag<T> suitableRoot = findSuitableRoot();
        this.root = makeNice(suitableRoot);
        computeTreeIndex();
        return treeDecomposition;
    }

    /**
     * A nice tree-decomposition is computed from a chosen root bag down to the leafs. The choose of this root bag
     * may dramatically change the structure of the nice tree-decomposition which may have an impact on the performance
     * of algorithms that later work on the decomposition.
     *
     * This method implements an heurictic that tries to find a ``good'' candidate for such an root.
     *
     * @return A suitable root bag from which on we can build a nice tree-decomposition.
     */
    private Bag<T> findSuitableRoot() {
        return null;
    }

    /**
     * Make the tree-decomposition \emph{nice} starting at the given root. Note that the root bag may not be the given bag,
     * but will lead by a path of forget-bags to it.
     * @param root A bag at which we shall root the tree-decomposition.
     * @return The actual root bag of the constructed nice tree-decomposition.
     */
    private Bag<T> makeNice(Bag<T> root) {
        return null;
    }

    /**
     * Computes the tree-index for the decomposition.
     */
    private void computeTreeIndex() {
    }

    /**
     * Returns the tree-index for this decomposition
     * @return A mapping $\phi\colon V\rightarrow\{0,\dots,\mathrm{tw}\}$.
     */
    public Map<T, Integer> getTreeIndex() {
        return treeIndex;
    }

    /**
     * Returns the root bag of the nice tree-decomposition.
     * @return The unique root bag.
     */
    public Bag<T> getRoot() {
        return root;
    }
}
