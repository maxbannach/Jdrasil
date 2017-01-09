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
package jdrasil.algorithms;

import jdrasil.algorithms.preprocessing.GraphReducer;
import jdrasil.algorithms.upperbounds.StochasticMinFillDecomposer;
import jdrasil.graph.Graph;
import jdrasil.graph.TreeDecomposer;
import jdrasil.graph.TreeDecomposition;
import jdrasil.graph.TreeDecomposition.TreeDecompositionQuality;

/**
 * A full algorithm to compute a tree-decomposition heuristically.
 * 
 * The class provides access to the min-fill algorithm.
 * In the first, the stochastic min-fill algorithm is run a certain amount of time and (as it is anytime) provides consecutive improving solutions.
 * 
 * @author Max Bannach
 * @author Sebastian Berndt
 * @author Thorsten Ehlers
 */
public class HeuristicDecomposer<T extends Comparable<T>> implements TreeDecomposer<T> {
	
	/** the graph we wish to decompose */
	private final Graph<T> graph;
		
	/** The stochastic min-fill decomposer used in the first phase */
	private StochasticMinFillDecomposer<T> minFillDecomposer;
    	
	/** The currently best known tree-decompostion. */
	private TreeDecomposition<T> currentDecomposition;
	
	/**
	 * Default constructor to initialize data structures. 
	 * @param graph
	 */
	public HeuristicDecomposer(Graph<T> graph) {
		this.graph = graph;	
	}
	
	@Override
	public TreeDecomposition<T> call() throws Exception {
		
		GraphReducer<T> reducer = new GraphReducer<>(graph);
		for (Graph<T> reduced : reducer) {
			minFillDecomposer = new StochasticMinFillDecomposer<T>(reduced);
			currentDecomposition = minFillDecomposer.call();
			reducer.addbackTreeDecomposition(currentDecomposition);
		}
		return reducer.getTreeDecomposition();
	}

	@Override
	public TreeDecomposition<T> getCurrentSolution() {
		return null;
	}

	@Override
	public TreeDecompositionQuality decompositionQuality() {
		return TreeDecompositionQuality.Heuristic;
	}

}
