package jdrasil.workontd;

/**
 * The \JClass{StateVectorFactory} is used to produce \JClass{StateVector} objects for \emph{leafs} of the
 * tree-decomposition. From there on, other \JClass{StateVector} objects are created directly by calling
 * methods of the \JClass{StateVector} interface.
 *
 * Use an initialization of this interface to provide a dynamic program (i.\,e., a implementation of @see \JClass{StateVector})
 * to @see DynammicProgrammingOnTreeDecomposition.
 *
 * @author Max Bannach
 */
public interface StateVectorFactory<T extends Comparable<T>> {

    /**
     * Generate a \JClass{StateVector} object for a leaf of the tree-decomposition. These are usually empty.
     * @return A \JClass{StateVector} object.
     */
    public StateVector<T> createStateVectorForLeaf();

}
