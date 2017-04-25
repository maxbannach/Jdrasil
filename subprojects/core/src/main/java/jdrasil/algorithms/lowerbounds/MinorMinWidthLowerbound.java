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
package jdrasil.algorithms.lowerbounds;

import java.io.Serializable;
import java.util.*;


import jdrasil.graph.Graph;
import jdrasil.graph.GraphFactory;
import jdrasil.utilities.RandomNumberGenerator;

/**
 * It is a well known fact that for every minor H of G the following holds: \(tw(H) \le tw(G)\). To obtain
 * a lowerbound on the tree-width of G it is thus sufficient to find good lowerbounds for minors of G.
 * The minor-min-width heuristic devoloped by Gogate and Dechter for the QuickBB algorithm 
 * (see "A complete Anytime Algorithm for Treewidth") does exactly this. It computes a lowerbound for 
 * a minor of G and tries heuristically to find a good minor for this task.
 * 
 * @param <T>
 * @author Max Bannach
 */
public class MinorMinWidthLowerbound<T extends Comparable<T>> implements Lowerbound<T>, Serializable {

	private static final long serialVersionUID = -7729782858493633708L;

	/** The graph for which we wish to find a lowerbound. */
	private final Graph<T> graph;

	/** Current best lower bound */
	private int low;

	/**
	 * The algorithm can use different strategies to select vertices
	 * (see Bodlaender and Koster: Treewidth Computations II).
	 */
	public enum Algorithm {
		minD,
		maxD,
		leastC;
	}

	/** Strategy selected. */
	private Algorithm toRun;

	/**
	 * The algorithm is initialized with a graph that should be decomposed and a seed for randomness.
	 * @param graph
	 */
	public MinorMinWidthLowerbound(Graph<T> graph) {
		this.graph = GraphFactory.copy(graph);
		this.low = 0;
		setToRun(Algorithm.leastC);
	}

	/**
	 * Get a neighbor of the given vertex \(v\) in the given graph \(G\) which is suitable for being contracted.
	 * @param G
	 * @param v
	 * @return a neighbor of v that should be contracted into v
	 */
	private T getNeighbor(Graph<T> G, T v) {
		// store all optimal vertices and pick one at random
		List<T> nextU = new LinkedList<T>();

		// select the neighbor depending on the selected strategy
		switch (toRun) {
			case minD: // select neighbor of minimum degree
				int min = Integer.MAX_VALUE;
				for (T u : G.getNeighborhood(v)) {
					int deg = G.getNeighborhood(u).size();
					if (deg < min) {
						min = deg;
						nextU.clear();
						nextU.add(u);
					} else if (deg == min) {
						nextU.add(u);
					}
				}
				break;
			case maxD: // select neighbor of maximum degree
				int max = Integer.MIN_VALUE;
				for (T u : G.getNeighborhood(v)) {
					int deg = G.getNeighborhood(u).size();
					if (deg > max) {
						max = deg;
						nextU.clear();
						nextU.add(u);
					} else if (deg == max) {
						nextU.add(u);
					}
				}
				break;
			case leastC: // select neighbor with minimum amount of common neighbors
				int minN = Integer.MAX_VALUE;
				for (T u : G.getNeighborhood(v)) {
					Set<T> common = new HashSet<>();
					common.addAll(G.getNeighborhood(v));
					common.retainAll(G.getNeighborhood(u));
					int k = common.size();
					if (k < minN) {
						minN = k;
						nextU.clear();
						nextU.add(u);
					} else if (k == minN) {
						nextU.add(u);
					}
				}
				break;
		}

		// done
		return nextU.size() > 0 ? nextU.get(RandomNumberGenerator.nextInt(nextU.size())) : null;
	}

	@Override
	public Integer call() throws Exception {	 

		// as long as the graph has vertices we can still contract some
		while (graph.getCopyOfVertices().size() > 0) {
			
			// search vertex of min degree
			int min = Integer.MAX_VALUE;
			List<T> nextV = new LinkedList<>();
			for (T v : graph) {
				int deg = graph.getNeighborhood(v).size();
				if (deg == 0) continue; // ignore isolated vertices
				if (deg < min) {
					min = deg;
					nextV.clear();
					nextV.add(v);
				} else if (deg == min) { // break ties randomly
					nextV.add(v);
				}
			}
			T v = nextV.size() > 0 ? nextV.get(RandomNumberGenerator.nextInt(nextV.size())) : null;
			if (v == null) break;

			// update lowerbound
			low = Math.max(low, graph.getNeighborhood(v).size());

			// search suitable neighbor for contraction
			T u = getNeighbor(graph, v);
			if (u == null) break;

			// contract edge
			graph.contract(v, u);
		}

		// done
		return low;
	}

	@Override
	public Integer getCurrentSolution() {
		return low;
	}

	/**
	 * Set the strategy used by this class.
	 * @param toRun
	 */
	public void setToRun(Algorithm toRun) { this.toRun = toRun; }

	/**
	 * Get the strategy used by this class.
	 * @param toRun
	 * @return
	 */
	public Algorithm getToRun(Algorithm toRun) { return this.toRun; }

}
