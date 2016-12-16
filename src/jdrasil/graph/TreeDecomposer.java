/*
 * Copyright (c) 2016-present, Max Bannach, Sebastian Berndt, Thorsten Ehlers
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT
 * OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package jdrasil.graph;

import java.util.concurrent.Callable;

/**
 * The TreeDecomposer Interface describes a class that models an algorithm for computing a tree-decomposition.
 * By implementing this interface, a class has to implement the Callable interface, which computes a tree-decomposition of vertex type T and G
 * from a graph of vertex type G. 
 * 
 * @param <T> the vertex type of the graph
 * @author Max Bannach
 */
public interface TreeDecomposer<T extends Comparable<T>> extends Callable<TreeDecomposition<T>> {

	/**
	 * This calls the execution of the tree-decomposition algorithm and returns the decomposition.
	 * The function is allowed to throw exceptions (as defined in the Callable interface), however, this should be specified
	 * more precisely.
	 * 
	 * @return a tree-decomposition of the graph
	 */
	public TreeDecomposition<T> call() throws Exception;
	
	/**
	 * This method returns the best solution found so far. This method should return null if @see call() was not called,
	 * or the algorithm does not found a solution yet, or if the algorithm is not anytime.
	 * @return the current best solution
	 */
	public TreeDecomposition<T> getCurrentSolution();
	
	/**
	 * Specify the quality of the tree-decomposition provided by this algorithm
	 * @return the quality of the decomposition
	 */
	public TreeDecomposition.TreeDecompositionQuality decompositionQuality();
	
}
