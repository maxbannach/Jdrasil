package de.uniluebeck.tcs.jdrasil.upperbounds;

/**
 * MinWidthDecomposer.java
 * @author bannach
 */

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import de.uniluebeck.tcs.jdrasil.App;
import de.uniluebeck.tcs.jdrasil.algorithms.EliminationOrderDecomposer;
import de.uniluebeck.tcs.jdrasil.graph.Graph;
import de.uniluebeck.tcs.jdrasil.graph.TreeDecomposer;
import de.uniluebeck.tcs.jdrasil.graph.TreeDecomposition;
import de.uniluebeck.tcs.jdrasil.graph.TreeDecomposition.TreeDecompositionQuality;

/**
 * This class implements the mind-width heuristic. In this heuristic, the vertices of the graph are ordered from 1 to n
 * in the following way: Always choose the vertex of minimum degree, append it to the current permutation and delete it from the graph.
 * Break ties randomly.
 * 
 * @param <T>
 */
public class MinWidthDecomposer<T extends Comparable<T>> implements TreeDecomposer<T>, java.io.Serializable {

	private static final long serialVersionUID = 3934246227064656936L;

	/** The graph to be decomposed. */
	private final Graph<T> graph;
	
	/** A copy to be modified by the algorithm. */
	private final Graph<T> workingCopy;
	
	/** Source of randomness. */
	private final Random dice;
	
	/**
	 * The algorithm is initialized with a graph that should be decomposed and a seed for randomness.
	 * @param graph
	 * @param seed
	 */
	public MinWidthDecomposer(Graph<T> graph) {
		this.graph = graph;
		this.workingCopy = graph.copy();
		this.dice = App.getSourceOfRandomness();
	}
	
	@Override
	public TreeDecomposition<T> call() throws Exception {
		
		// catch the empty graph
		if (graph.getVertices().size() == 0) return new TreeDecomposition<T>(graph);
		
		List<T> permutation = new LinkedList<T>();
		
		// as long as the graph is not empty, we have to process vertices
		while (workingCopy.getVertices().size() > 0) {
			if (Thread.currentThread().isInterrupted()) throw new Exception();
			
			int minDegree = Integer.MAX_VALUE;
			List<T> nextV = new LinkedList<T>();
			
			// search vertex of minimum degree
			for (T v : workingCopy.getVertices()) {				
				int deg = workingCopy.getNeighborhood(v).size();
				if (deg < minDegree) {
					minDegree = deg;
					nextV.clear();
					nextV.add(v);
				} else if (deg == minDegree) { // break ties randomly
					nextV.add(v);		
				}				
			}
						
			// found the vertex -> delete it and add it to the permutation
			T v = nextV.get(dice.nextInt(nextV.size()));
			workingCopy.deleteVertex(v);
			permutation.add(v);
		}
		
		// done
		return new EliminationOrderDecomposer<T>(graph, permutation, TreeDecompositionQuality.Heuristic).call();
	}

	@Override
	public TreeDecompositionQuality decompositionQuality() {
		return TreeDecompositionQuality.Heuristic;
	}

	@Override
	public TreeDecomposition<T> getCurrentSolution() {
		return null;
	}
	
}
