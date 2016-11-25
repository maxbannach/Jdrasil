package de.uniluebeck.tcs.jdrasil.graph;

/**
 * TreeDecomposer.java
 * @author bannach
 */

import java.util.concurrent.Callable;

/**
 * The TreeDecomposer Interface describes a class that models an algorithm for computing a tree-decomposition.
 * By implementing this interface, a class has to implement the Callable interface, which computes a tree-decomposition of vertex type T and G
 * from a graph of vertex type G. 
 * 
 * @param <T> the vertex type of the graph
 */
public interface TreeDecomposer<T extends Comparable<T>> extends Callable<TreeDecomposition<T>> {

	/**
	 * This calls the execution of the tree-decomposition algorithm and returns the decomposition.
	 * The function is allowed to throw exceptions (as defined in the Callable interface), however, this should be specified
	 * more precisely.
	 * 
	 * @return a <T,G> tree-decomposition of the graph
	 */
	public TreeDecomposition<T> call() throws Exception;
	
	/**
	 * This method returns the best solution found so far. This method should return null if @see call() was not called,
	 * or the algorithm does not found a solution yet, or if the algorithm is not anytime.
	 * @return
	 */
	public TreeDecomposition<T> getCurrentSolution();
	
	/**
	 * Specify the quality of the tree-decomposition provided by this algorithm
	 * @return
	 */
	public TreeDecomposition.TreeDecompositionQuality decompositionQuality();
	
}
