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
package jdrasil.algorithms.upperbounds;

import java.io.Serializable;
import java.util.List;
/**
 * StochasticMinFillDecomposer.java
 * @author bannach
 */
import java.util.Random;
import java.util.logging.Logger;


import jdrasil.algorithms.upperbounds.MinFillInDecomposer.Algo;
import jdrasil.graph.Graph;
import jdrasil.graph.TreeDecomposer;
import jdrasil.graph.TreeDecomposition;
import jdrasil.graph.TreeDecomposition.TreeDecompositionQuality;
import jdrasil.utilities.RandomNumberGenerator;
import jdrasil.utilities.logging.JdrasilLogger;

/**
 * The Min-Fill heuristic performs very well and can be seen as randomized algorithm as it breaks ties randomly.
 * Therefore, multiple runs of the algorithm produce different results and, hence, we can perform a stochastic search
 * by using the heuristic multiple times and reporting the best result.
 * 
 * @param <T> the vertex type
 * @author Max Bannach
 * @author Thorsten Ehlers
 */
public class StochasticMinFillDecomposer<T extends Comparable<T>> implements TreeDecomposer<T>, Serializable {

	private static final long serialVersionUID = -8256243005350278791L;

	/** Jdrasils Logger */
	private final static Logger LOG = Logger.getLogger(JdrasilLogger.getName());

	/** The graph to be decomposed. */
	private final Graph<T> graph;
	
	/** The decomposition we try to compute */
	private TreeDecomposition<T> decomposition;

	/** The best permutation that is computed. */
	public List<T> permutation;
	
	/**
	 * The algorithm is initialized with a graph that should be decomposed and a seed for randomness.
	 * @param graph to be decomposed
	 */
	public StochasticMinFillDecomposer(Graph<T> graph) {
		this.graph = graph;
		this.decomposition = new TreeDecomposition<T>(graph);
		this.decomposition.createBag(graph.getVertices());
	}
	

	@Override
	public TreeDecomposition<T> call() throws Exception {
		int lb = graph.getVertices().size();

		// iterating n times
		int itr = Math.min(lb,100);
		while (itr --> 0) {
			MinFillInDecomposer<T> mfid = new MinFillInDecomposer<T>(graph);
			// Switch between minFill and sparsestSubgraph
			if((itr % 2) == 0)
				mfid.setToRun(Algo.SparsestSubgraph);
			TreeDecomposition<T> newDec = mfid.call(lb);
			if (newDec != null && newDec.getWidth() < lb) {
				lb = newDec.getWidth();
				LOG.info("new bound: " + lb);
				decomposition = newDec;
				permutation = mfid.getPermutation();
			}
			
		}
		// Create a copy of the current decomposition, and try to improve it. 
		// Prevents race condition if signal handler is triggered while improve is running
		TreeDecomposition<T> tmp = decomposition.copy();
		tmp.improveDecomposition();
		if(tmp.getWidth() < decomposition.getWidth())
			decomposition = tmp;
		return decomposition;
	}
	

	/**
	 * Returns the elimination order computed by call().
	 * @return permutation as List
	 */
	public List<T> getPermutation() {
		return permutation;
	}

	@Override
	public TreeDecompositionQuality decompositionQuality() {
		return TreeDecompositionQuality.Heuristic;
	}
	
	@Override
	public TreeDecomposition<T> getCurrentSolution() {
		return decomposition;
	}
	
}
