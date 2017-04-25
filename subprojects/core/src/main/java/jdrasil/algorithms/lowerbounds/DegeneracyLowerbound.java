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
import java.util.HashMap;
import java.util.Map;

import jdrasil.graph.Graph;
import jdrasil.graph.GraphFactory;

/**
 * We call a Graph G=(V,E) d-degenerated if each subgraph H of G contains a vertex of maximal degree d.
 * It is a well known fact that we have \(d \le tw(G)\) and, thus, we can use the degeneracy of a graph as lowerbound for the tree-width.
 * 
 * This class implements the linear time algorithm from Matula and Beck to compute the degeneracy of a graph.
 * 
 * @param <T>
 * @author Max Bannach
 */
public class DegeneracyLowerbound<T extends Comparable<T>> implements Lowerbound<T>, Serializable {

	private static final long serialVersionUID = 4890692495598672075L;
	
	/** The graph for which a lower bound is computed. */
	private final Graph<T> graph;
	
	/**
	 * Just initzialize the algorithm with a given graph.
	 * @param graph
	 */
	public DegeneracyLowerbound(Graph<T> graph) {
		this.graph = GraphFactory.copy(graph);
	}
	
	@Override
	public Integer call() throws Exception {
		int d = 0;
		
		// compute degrees of the graph
		Map<T, Integer> d_v = new HashMap<>();
		for (T v : graph) {
			d_v.put(v, graph.getNeighborhood(v).size());
		}
		
		// process the graph
		while (!graph.getCopyOfVertices().isEmpty()) {
			// find minimum degree
			T v = null;
			for (T w : graph) {
				if (v == null || d_v.get(w) < d_v.get(v)) {
					v = w;
				}
			}
			
			// update k
			d = Math.max(d, d_v.get(v));
			
			// updage degrees
			for (T w : graph.getNeighborhood(v)) {
				d_v.put(w, d_v.get(w)-1);
			}
			
			// delete vertex
			graph.removeVertex(v);
		}
		
		// done
		return d;
	}

	@Override
	public Integer getCurrentSolution() {
		return null;
	}
	
}
