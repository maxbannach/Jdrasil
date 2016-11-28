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

/**
 * HeuristicDecomposer.java
 * @author Max Bannach
 */

import jdrasil.App;
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

	/** Preprocessor to reduce the graph size. */
	private final ReductionRuleDecomposer<T> preprocessor;
	
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
		this.preprocessor = new ReductionRuleDecomposer<T>(graph);		
	}
	
	@Override
	public TreeDecomposition<T> call() throws Exception {
		
		/* start preprocessing phase */
		App.log("starting preprocessing");
		if (this.preprocessor.reduce()) return preprocessor.getTreeDecomposition();
		App.log("reduced to from " + graph.getVertices().size() + " to " + this.preprocessor.getReducedGraph().getVertices().size() + " vertices");
		
		/* start the first phase */
		App.log("starting minFill phase");
		minFillDecomposer = new StochasticMinFillDecomposer<T>(preprocessor.getReducedGraph());
		currentDecomposition = minFillDecomposer.call();
		
		// done
		preprocessor.glueTreeDecomposition(currentDecomposition);
		return currentDecomposition;
	}

	@Override
	public TreeDecomposition<T> getCurrentSolution() {
		TreeDecomposition<T> newDecomposition = null;
		newDecomposition = minFillDecomposer.getCurrentSolution();

		// update current solution only, if the current phase has found a new one
		if (newDecomposition != null && newDecomposition.getWidth() < currentDecomposition.getWidth()){
		    currentDecomposition = newDecomposition;
		}
		return currentDecomposition;
	}

	@Override
	public TreeDecompositionQuality decompositionQuality() {
		return TreeDecompositionQuality.Heuristic;
	}

}
