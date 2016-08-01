package de.uniluebeck.tcs.algorithms.lowerbounds;

/**
 * Lowerbound.java
 * @author bannach
 */

import java.util.concurrent.Callable;

/**
 * The Lowerbound Interface describes a class that models an algorithm for computing a lower bound of the tree-width of a graph.
 * By implementing this interface, a class has to implement the Callable interface, which computes a lower bound on the tree-width 
 * of vertex type T graph.
 * 
 * @param <T> the vertex type of the graph
 */
public interface Lowerbound<T extends Comparable<T>> extends Callable<Integer> {

	/**
	 * This calls the execution of the lower bound algorithm and returns the lower bound.
	 * The function is allowed to throw exceptions (as defined in the Callable interface), however, this should be specified
	 * more precisely.
	 * 
	 * @return The lower bound as Integer
	 */
	public Integer call() throws Exception;
	
	/**
	 * This method returns the best solution found so far. This method should return null if @see call() was not called,
	 * or the algorithm does not found a solution yet, or if the algorithm is not anytime.
	 * @return
	 */
	public Integer getCurrentSolution();
	
}
