package de.uniluebeck.tcs.jdrasil.upperbounds;

import java.io.Serializable;

import de.uniluebeck.tcs.jdrasil.App;
import de.uniluebeck.tcs.jdrasil.graph.Graph;
import de.uniluebeck.tcs.jdrasil.graph.TreeDecomposer;
import de.uniluebeck.tcs.jdrasil.graph.TreeDecomposition;
import de.uniluebeck.tcs.jdrasil.graph.TreeDecomposition.TreeDecompositionQuality;
import de.uniluebeck.tcs.jdrasil.upperbounds.MinFillInDecomposer.Algo;

import java.util.List;
/**
 * StochasticMinFillDecomposer.java
 * @author bannach
 */
import java.util.Random;

/**
 * The Min-Fill heuristic performs very well and can be seen as randomized algorithm as it breaks ties randomly.
 * Therefore, multiple runs of the algorithm produce different results and, hence, we can perform a stochastic search
 * by using the heuristic multiple times and reporting the best result.
 * 
 * @param <T>
 */
public class StochasticMinFillDecomposer<T extends Comparable<T>> implements TreeDecomposer<T>, Serializable {

	private static final long serialVersionUID = -8256243005350278791L;

	/** The graph to be decomposed. */
	private final Graph<T> graph;
	
	/** The decomposition we try to compute */
	private TreeDecomposition<T> decomposition;
	
	/** Source of randomness. */
	private final Random dice;
	
	/** The best permutation that is computed. */
	public List<T> permutation;
	
	/**
	 * The algorithm is initialized with a graph that should be decomposed and a seed for randomness.
	 * @param graph
	 * @param seed
	 */
	public StochasticMinFillDecomposer(Graph<T> graph) {
		this.graph = graph;
		this.decomposition = new TreeDecomposition<T>(graph);
		this.decomposition.createBag(graph.getVertices());
		this.dice = App.getSourceOfRandomness();
	}
	

	@Override
	public TreeDecomposition<T> call() throws Exception {
		int lb = graph.getVertices().size();

		// iterating n times
		int itr = Math.min(lb,100);
//		App.log("LB: "+lb);
		while (itr --> 0) {
			MinFillInDecomposer<T> mfid = new MinFillInDecomposer<T>(graph, dice);
			// Switch between minFill and sparsestSubgraph
			if((itr % 2) == 0)
				mfid.setToRun(Algo.SparsestSubgraph);
			TreeDecomposition<T> newDec = mfid.call(lb);
			if (newDec != null && newDec.getWidth() < lb) {
				lb = newDec.getWidth();
				App.reportNewSolution(lb);
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
//		decomposition.improveDecomposition();
		return decomposition;
	}
	

	/**
	 * Returns the elimination order computed by call().
	 * @return
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
