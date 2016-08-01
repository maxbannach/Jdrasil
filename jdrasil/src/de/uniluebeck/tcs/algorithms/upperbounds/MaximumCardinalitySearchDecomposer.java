package de.uniluebeck.tcs.algorithms.upperbounds;

/**
 * MaximumCardinalitySearchDecomposer.java
 * @author bannach
 */

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import de.uniluebeck.tcs.App;
import de.uniluebeck.tcs.algorithms.EliminationOrderDecomposer;
import de.uniluebeck.tcs.graph.Graph;
import de.uniluebeck.tcs.graph.TreeDecomposer;
import de.uniluebeck.tcs.graph.TreeDecomposition;
import de.uniluebeck.tcs.graph.TreeDecomposition.TreeDecompositionQuality;

/**
 * This class implements the Maximum-Cardinality Search heuristic. The heuristic order the vertices of G
 * from 1 to n in the following ordering: We first put a random vertex v at position n. Then we choose the 
 * vertex v' with the most neighbors that are already placed at position n-1 and recurse this way. Ties are broken randomly.
 * 
 * @param <T>
 */
public class MaximumCardinalitySearchDecomposer<T extends Comparable<T>> implements TreeDecomposer<T> ,java.io.Serializable{

	private static final long serialVersionUID = -7833200444021178521L;

	/** The graph to be decomposed. */
	private final Graph<T> graph;
	
	/** Size of the graph that should be decomposed. */
	private final int n;
	
	/** Source of randomness. */
	private final Random dice;
	
	/** A list of vertices that still have to be added to permutation. */
	private final List<T> unlabeledVertices;
	
	/** Data structures storing the amount of labeled neighbors of each vertex. */
	private final Map<T, Integer> labeldNeighbors;
	
	/**
	 * The algorithm is initialized with a graph that should be decomposed and a seed for randomness.
	 * @param graph
	 * @param seed
	 */
	public MaximumCardinalitySearchDecomposer(Graph<T> graph) {
		this.graph = graph;
		this.n = graph.getVertices().size();
		this.dice = App.getSourceOfRandomness();
		this.unlabeledVertices = new LinkedList<>(graph.getVertices());
		this.labeldNeighbors = new HashMap<>();
	}
	
	/**
	 * Place a new vertex to the permutation, i.e., label it.
	 * This will increase the number of labeled neighbors of the neighbors of 
	 * the specified vertex by one and remove the given vertex from the list of vertices 
	 * that have to be labeled.
	 * @param vertex
	 * @param permutation
	 */
	private void addVertexToPermutation(T vertex, List<T> permutation) {
		permutation.add(0, vertex);
		for (T v : graph.getNeighborhood(vertex)) {
			labeldNeighbors.put(v, labeldNeighbors.get(v) + 1);			
		}
		unlabeledVertices.remove(vertex);
	}
	
	@Override
	public TreeDecomposition<T> call() throws Exception {
		
		// catch the empty graph
		if (graph.getVertices().size() == 0) return new TreeDecomposition<T>(graph);
		
		List<T> permutation = new LinkedList<T>();
		
		// each vertex has an entry
		for (T v : graph.getVertices()) {
			labeldNeighbors.put(v, 0);
		}
		
		// a random start vertex
		T s = unlabeledVertices.remove(dice.nextInt(n));
		addVertexToPermutation(s, permutation);
		
		// place the other vertices
		while (!unlabeledVertices.isEmpty()) {
			if (Thread.currentThread().isInterrupted()) throw new Exception();
			
			int max = Integer.MIN_VALUE;
			List<T> nextV = new LinkedList<T>();
			for (T v : unlabeledVertices) {
				if (labeldNeighbors.get(v) > max) {
					max = labeldNeighbors.get(v);
					nextV.clear();
					nextV.add(v);
				} else if (labeldNeighbors.get(v) == max) {
					nextV.add(v);
				}
			}
			addVertexToPermutation(nextV.get(dice.nextInt(nextV.size())), permutation);
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
