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

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import jdrasil.App;
import jdrasil.algorithms.EliminationOrderDecomposer;
import jdrasil.graph.Graph;
import jdrasil.graph.GraphFactory;
import jdrasil.graph.TreeDecomposer;
import jdrasil.graph.TreeDecomposition;
import jdrasil.graph.TreeDecomposition.TreeDecompositionQuality;
import jdrasil.utilities.RandomNumberGenerator;

/**
 * This class implements the mind-width heuristic. In this heuristic, the vertices of the graph are ordered from 1 to n
 * in the following way: Always choose the vertex of minimum degree, append it to the current permutation and delete it from the graph.
 * Break ties randomly.
 * 
 * @param <T>
 * @author Max Bannach
 */
public class MinWidthDecomposer<T extends Comparable<T>> implements TreeDecomposer<T>, java.io.Serializable {

	private static final long serialVersionUID = 3934246227064656936L;

	/** The graph to be decomposed. */
	private final Graph<T> graph;
	
	/** A copy to be modified by the algorithm. */
	private final Graph<T> workingCopy;

	/**
	 * The algorithm is initialized with a graph that should be decomposed and a seed for randomness.
	 * @param graph
	 */
	public MinWidthDecomposer(Graph<T> graph) {
		this.graph = graph;
		this.workingCopy = GraphFactory.copy(graph);
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
			T v = nextV.get(RandomNumberGenerator.nextInt(nextV.size()));
			workingCopy.removeVertex(v);
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
