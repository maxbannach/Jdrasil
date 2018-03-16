package jdrasil.workontd;

import jdrasil.graph.Bag;

import java.util.Map;

/**
 * A state vector describes the state of a dynamic program working on a tree-decomposition at a specific bag. For instance,
 * in a program that checks if the graph is 3-colorable, the state vector of a bag could be the set of all possible
 * 3-colorings of that bag that are consistent to colorings of the subgraph of the subtree rooted at the bag.
 *
 * In \Jdrasil{} the whole dynamic program that works on a tree-decomposition is defined over the \JClass{StateVector} interface.
 * In more detail, the dynamic program will traverse the tree-decomposition in a bottom-up fashion and compute \JClass{StateVector}
 * objects for each bag, using the \JClass{StateVector} objects of the children of the bag. For doing so, it will,
 * depending on the type of the bag, call a method of the \JClass{StateVector} object of the child (for instance to indicate that a vertex
 * is \emph{introduced} in the next bag) that then produces the \JClass{StateVector} for its parent. How the \JClass{StateVector}
 * for the parent is produced is highly problem specific and can be seen as the \emph{actual program}.
 *
 * We shall note that all methods of this interface do not have to produce a completely new \JClass{StateVector} object, but
 * may also just modify itself and return a reference to itself. With this pattern, the dynamic program uses less space
 * while traversing the tree-decomposition.
 *
 * All methods of the interface are always called with the parent bag, eventually some special vertices (for instance
 * the introduced vertex), and a so called \emph{tree-index}. The later is a mapping $\phi\colon V\rightarrow\{0,\dots,\mathrm{tw}\}$
 * that maps an integer to every vertex such that no two vertices that appear in a common bag share an index.
 * With other words, the tree-index can be used, for instance, to index an array that stores information for the vertices
 * in a bag.
 *
 * @author Max Bannach
 */
public interface StateVector<T extends Comparable<T>> {

    /**
     * This method is called whenever the parent of the bag that corresponds to this \JClass{StateVector} is an
     * \emph{introduce bag}, which means it has the same content and exactly one additional vertex.
     *
     * @param bag The parent bag.
     * @param v The introduced vertex.
     * @param treeIndex A reference to the tree-index.
     * @return The \JClass{StateVector} for the parent.
     */
    public StateVector<T> introduce(Bag<T> bag, T v, Map<T, Integer> treeIndex);

    /**
     * This method is called whenever the parent of the bag that corresponds to this \JClass{StateVector} is an
     * \emph{forget bag}, which means it has the same content and exactly one vertex less.
     *
     * @param bag The parent bag.
     * @param v The forgotten vertex.
     * @param treeIndex A reference to the tree-index.
     * @return The \JClass{StateVector} for the parent.
     */
    public StateVector<T> forget(Bag<T> bag, T v, Map<T, Integer> treeIndex);

    /**
     * This method is called whenever the parent of the bag that corresponds to this \JClass{StateVector} is an
     * \emph{join bag}, which means it has to children with the same content.
     *
     * @param bag The parent bag.
     * @param o The \JClass{StateVector} of the other child.
     * @param treeIndex A reference to the tree-index.
     * @return The \JClass{StateVector} for the parent.
     */
    public StateVector<T> join(Bag<T> bag, StateVector<T> o,
                               Map<T, Integer> treeIndex);

    /**
     * This method is called whenever the parent of the bag that corresponds to this \JClass{StateVector} is an
     * \emph{edge bag}, which means it has the same content and is labeled with an edge.
     *
     * @param bag The parent bag.
     * @param v The first vertex of the introduced edge.
     * @param w The second vertex of the introduced edge.
     * @param treeIndex A reference to the tree-index.
     * @return The \JClass{StateVector} for the parent.
     */
    public StateVector<T> edge(Bag<T> bag, T v, T w,
                               Map<T, Integer> treeIndex);

}
