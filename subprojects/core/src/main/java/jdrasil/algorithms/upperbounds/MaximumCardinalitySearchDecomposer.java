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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;


import jdrasil.algorithms.EliminationOrderDecomposer;
import jdrasil.graph.Graph;
import jdrasil.graph.TreeDecomposer;
import jdrasil.graph.TreeDecomposition;
import jdrasil.graph.TreeDecomposition.TreeDecompositionQuality;
import jdrasil.utilities.RandomNumberGenerator;

/**
 * This class implements the Maximum-Cardinality Search heuristic. The heuristic order the vertices of G
 * from 1 to n in the following ordering: We first put a random vertex v at position n. Then we choose the 
 * vertex v' with the most neighbors that are already placed at position n-1 and recurse this way. Ties are broken randomly.
 * 
 * @param <T>
 * @author Max Bannach
 */
public class MaximumCardinalitySearchDecomposer<T extends Comparable<T>> implements TreeDecomposer<T> ,java.io.Serializable{

	private static final long serialVersionUID = -7833200444021178521L;

	/** The graph to be decomposed. */
	private final Graph<T> graph;
	
	/** Size of the graph that should be decomposed. */
	private final int n;

	/** A list of vertices that still have to be added to permutation. */
	private final List<T> unlabeledVertices;
	
	/** Data structures storing the amount of labeled neighbors of each vertex. */
	private final Map<T, Integer> labeldNeighbors;
	
	/**
	 * The algorithm is initialized with a graph that should be decomposed and a seed for randomness.
	 * @param graph
	 */
	public MaximumCardinalitySearchDecomposer(Graph<T> graph) {
		this.graph = graph;
		this.n = graph.getCopyOfVertices().size();
		this.unlabeledVertices = new LinkedList<>(graph.getCopyOfVertices());
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
		if (graph.getCopyOfVertices().size() == 0) return new TreeDecomposition<T>(graph);
		
		List<T> permutation = new LinkedList<T>();
		
		// each vertex has an entry
		for (T v : graph.getCopyOfVertices()) {
			labeldNeighbors.put(v, 0);
		}
		
		// a random start vertex
		T s = unlabeledVertices.remove(RandomNumberGenerator.nextInt(n));
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
			addVertexToPermutation(nextV.get(RandomNumberGenerator.nextInt(nextV.size())), permutation);
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
