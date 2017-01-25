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


import jdrasil.algorithms.EliminationOrderDecomposer;
import jdrasil.graph.Graph;
import jdrasil.graph.GraphFactory;
import jdrasil.graph.TreeDecomposer;
import jdrasil.graph.TreeDecomposition;
import jdrasil.graph.TreeDecomposition.TreeDecompositionQuality;
import jdrasil.utilities.RandomNumberGenerator;
import sun.security.provider.certpath.Vertex;

/**
 * This class implements greedy permutation heuristics to compute a tree-decomposition. The heuristic eliminates
 * the vertex v that minimises some function f(v), ties are broken randomly.
 * See "Bodlaender and Koster: Treewidth computations I. Upper bounds" for an overview of possible functions f.
 * 
 * @param <T>
 * @author Max Bannach
 * @author Thorsten Ehlers
 */
public class GreedyPermutationDecomposer<T extends Comparable<T>> implements TreeDecomposer<T>, Serializable {

	private static final long serialVersionUID = 1L;

	/** The graph to be decomposed. */
	private final Graph<T> graph;

	/** The permutation that is computed. */
	private List<T> permutation;

	/** The value k used by the heuristic. */
	private int k;
	
	/** Which algorithm to use? Choices: GreedyFillIn and SparsestSubgraph (c.f. Bodlaender, Upper Bounds) */
	public enum Algorithm {
		Degree,
		FillIn,
		DegreePlusFillIn,
		SparsestSubgraph,
		FillInDegree,
		DegreeFillIn
	}

	/** Algorithm that is used. */
	private Algorithm toRun;

	/**
	 * The algorithm is initialized with a graph that should be decomposed.
	 * The default algorithm to run is FillIn.
	 * @param graph
	 */
	public GreedyPermutationDecomposer(Graph<T> graph) {
		this.graph = graph;
		setToRun(Algorithm.FillIn); // default is FillIn, this heuristic shows solid results on most tests
		setLookAhead(1); // default mode is no look-ahead, this is the fastest version
	}

	/** A Tuple storing a vertex and its value (with respect to the heuristic of the current algorithm). */
	private class VertexValue {
		T vertex;
		int value;
	}

	/**
	 * Given a graph and a vertex of it, this method computes the value (with respect to the choosen heuristic)
	 * of the given vertex in this graph.
	 * @param G the graph which contains the vertex
	 * @param v the vertex for which the value should be computed
	 * @return the heuristic value of the given vertex as VertexTuple
	 */
	private VertexValue getValue(Graph<T> G, T v) {
		VertexValue tuple = new VertexValue();
		tuple.vertex = v;

		// fill-in value and degree of the vertex
		int phi = G.getFillInValue(v);
		int delta = G.getNeighborhood(v).size();
		int n = G.getVertices().size();

		// compute the value of the vertex with respect to the current algorithm
		int value = 0;
		switch (toRun) {
			case Degree:
				value = delta;
				break;
			case FillIn:
				value = phi;
				break;
			case DegreePlusFillIn:
				value = delta + phi;
				break;
			case SparsestSubgraph:
				value = phi - delta;
				break;
			case FillInDegree:
				value = delta + (1/(n*n))*phi;
				break;
			case DegreeFillIn:
				value = phi + (1/n)*delta;
				break;
		}
		tuple.value = value;

		// done
		return tuple;
	}

	/**
	 * Computes the next vertex to be eliminated with respect to the selected heuristic.
	 * The vertex is returned as tuple, containing the vertex and its value
	 *
	 * The parameter \(k\) defines the look-ahead of the method, i.e., if k = 1 the vertex with the smallest value
	 * is returned; if k = 2 the vertex that minimises the sum of its value and the next best vertex (in the then contracted
	 * graph) is returned. Each layer of \(k\) improves the heuristic, but increases the running time by \(O(n)\).
	 *
	 * @param G
	 * @param k the number of vertices that should be considered in the future
	 * @return A VertexValue tuple storing the best vertex and its value
	 */
	private VertexValue nextVertex(Graph<T> G, int k) {
		// min value that any vertex has, and a list of vertices with this value
		int min = Integer.MAX_VALUE;
		List<VertexValue> best = new LinkedList<>();

		// search for the best vertex
		for (T v : G.getVertices()) {

			// the vertex-value-tuple of the current vertex
			VertexValue tuple = getValue(G, v);

			// eliminate vertex and look for further value
			if (k > 1 && G.getVertices().size() > 1) {
				System.out.println("test");
				Graph.EliminationInformation info = G.eliminateVertex(v);
				VertexValue next = nextVertex(G, k - 1);
				G.deEliminateVertex(info);
				tuple.value += next.value;
			}

			// update data
			if (tuple.value < min) {
				min = tuple.value;
				best.clear();
				best.add(tuple);
			} else if (tuple.value == min) {
				best.add(tuple);
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

	/**
	 * If the algorithm is used with a good upper bound, it can aboard whenever the width of the greedily constructed permutation
	 * becomes to big.
	 * @param upper_bound
	 * @return a tree decomposition or null, if the width of the constructed permutation exceeds the upper bound
	 * @throws Exception
	 */
	public TreeDecomposition<T> call(int upper_bound) throws Exception {
		
		// catch the empty graph
		if (graph.getVertices().size() == 0) return new TreeDecomposition<T>(graph);

		// the permutation that we wish to compute and a copy of the graph, which will be modified
		List<T> permutation = new LinkedList<T>();
		Graph<T> workingCopy = GraphFactory.copy(graph);

		// compute the permutation
		for (int i = 0; i < graph.getVertices().size(); i++) {
			if (Thread.currentThread().isInterrupted()) throw new Exception();

			// obtain next vertex with respect to the current algorithm and check if this is a reasonable choice
			T v = nextVertex(workingCopy, this.k).vertex;
			if(workingCopy.getNeighborhood(v).size() >= upper_bound){
				// Okay, this creates a clique of size >= upper_bound + 1, I can abort!
				return null;
			}

			// add it to the permutation and eliminate it in the current subgraph
			permutation.add(v);
			workingCopy.eliminateVertex(v);
		}

		// done
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

	/**
	 * Returns the algorithm that is currently used.
	 * @return
	 */
	public Algorithm getToRun() {
		return toRun;
	}

	/**
	 * Sets the algorithm that should be used.
	 * @param toRun
	 */
	public void setToRun(Algorithm toRun) {
		this.toRun = toRun;
	}

	/**
	 * Defines how far the heuristic looks into the future while selecting a good vertex.
	 * @param k the number of vertices considered while selected a good vertex
	 */
	public void setLookAhead (int k) { this.k = k; }

	/**
	 * Get the look-ahead value.
	 * @return the look-ahead value.
	 */
	public int getLookAhead() { return k; }

}
