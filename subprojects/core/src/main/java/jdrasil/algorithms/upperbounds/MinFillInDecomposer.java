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
import java.util.LinkedList;
import java.util.List;
import java.util.Random;


import jdrasil.algorithms.EliminationOrderDecomposer;
import jdrasil.graph.Graph;
import jdrasil.graph.GraphFactory;
import jdrasil.graph.TreeDecomposer;
import jdrasil.graph.TreeDecomposition;
import jdrasil.graph.TreeDecomposition.TreeDecompositionQuality;
import jdrasil.utilities.RandomNumberGenerator;

/**
 * This class implements the min fill-in heuristic to compute a tree-decomposition. The heuristic eliminates the vertex that adds
 * the least amount of edges when eliminated next. Ties are broken randomly.
 * 
 * @param <T>
 * @author Max Bannach
 * @author Thorsten Ehlers
 */
public class MinFillInDecomposer<T extends Comparable<T>> implements TreeDecomposer<T>, Serializable {

	private static final long serialVersionUID = 1L;

	/** The graph to be decomposed. */
	private final Graph<T> graph;

	/** The permutation that is computed. */
	private List<T> permutation;
	
	/** Which algorithm to use? Choices: GreedyFillIn and SparsestSubgraph (c.f. Bodlaender, Upper Bounds) */
	public enum Algo{
		GreedyFillIn, 
		SparsestSubgraph;
	}
	
	private Algo toRun;
	/**
	 * The algorithm is initialized with a graph that should be decomposed.
	 * @param graph
	 */
	public MinFillInDecomposer(Graph<T> graph) {
		this.graph = graph;
		setToRun(Algo.GreedyFillIn);
	}

	/**
	 * Computes the next vertex to be eliminated with respect to the min fill-in heuristic.
	 * The vertex that adds the least amount of vertices when eliminates is choose, ties are broken randomly.
	 * @param G
	 * @return
	 */
	private T nextVertex(Graph<T> G) {
		int min = Integer.MAX_VALUE;
		List<T> best = new LinkedList<>();
		
		// search for the best vertex
		for (T v : G.getVertices()) {
			int value = G.getFillInValue(v);
			if(toRun == Algo.SparsestSubgraph)
				value -= G.getNeighborhood(v).size();
			// update data
			if (value < min) {
				min = value;
				best.clear();;
				best.add(v);
			} else if (value == min) {
				best.add(v);				
			}
		}
		
		// done
		return best.get(RandomNumberGenerator.nextInt(best.size()));
	}
	
	/**
	 * Returns the elimination order computed by call().
	 * @return
	 */
	public List<T> getPermutation() {
		return permutation;
	}
	
	@Override
	public TreeDecomposition<T> call() throws Exception {
		return call(graph.getVertices().size()+1);
	}
	
	public TreeDecomposition<T> call(int upper_bound) throws Exception {
		
		// catch the empty graph
		if (graph.getVertices().size() == 0) return new TreeDecomposition<T>(graph);

		List<T> permutation = new LinkedList<T>();
		Graph<T> workingCopy = GraphFactory.copy(graph);

		// compute the permutation
		for (int i = 0; i < graph.getVertices().size(); i++) {
			if (Thread.currentThread().isInterrupted()) throw new Exception();
			
			T v = nextVertex(workingCopy);
			if(workingCopy.getNeighborhood(v).size() >= upper_bound){
				// Okay, this creates a clique of size >= upper_bound + 1, I can abort!
				return null;
			}
			permutation.add(v);
			workingCopy.eliminateVertex(v);
		}
		
		this.permutation = permutation;
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

	public Algo getToRun() {
		return toRun;
	}

	public void setToRun(Algo toRun) {
		this.toRun = toRun;
	}
	
}
