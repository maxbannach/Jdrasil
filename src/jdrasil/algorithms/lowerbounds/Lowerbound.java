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
package jdrasil.algorithms.lowerbounds;

import java.util.concurrent.Callable;

/**
 * The Lowerbound Interface describes a class that models an algorithm for computing a lower bound of the tree-width of a graph.
 * By implementing this interface, a class has to implement the Callable interface, which computes a lower bound on the tree-width 
 * of vertex type T graph.
 * 
 * @param <T> the vertex type of the graph
 * @author Max Bannach
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
