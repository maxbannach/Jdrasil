package jdrasil.workontd;

/**
 * The \JClass{StateVectorFactory} is used to produce \JClass{StateVector} objects for \emph{leafs} of the
 * tree decomposition. From there on, other \JClass{StateVector} objects are created directly by calling
 * methods of the \JClass{StateVector} interface.
 *
 * Use an initialization of this interface to provide a dynamic program (i.\,e., a implementation of @see \JClass{StateVector})
 * to @see DynamicProgrammingOnTreeDecomposition.
 *
 * @author Max Bannach
 */
public interface StateVectorFactory<T extends Comparable<T>> {

    /**
     * Generate a \JClass{StateVector} object for a leaf of the tree-decomposition. These are usually empty, but may store the
     * tree width of the graph or initialize some data structures.
     *
     * @param tw The tree width of the decomposition on which the dynamic program is executed.
     * @return A \JClass{StateVector} object.
     */
    public StateVector<T> createStateVectorForLeaf(int tw);

}
