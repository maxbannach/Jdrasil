package de.uniluebeck.tcs.algorithms.lowerbounds;

/**
 * DegeneracyLowerbound.java 
 * @author bannach
 */

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import de.uniluebeck.tcs.graph.Graph;

/**
 * We call a Graph G=(V,E) d-degenerated if each subgraph H of G contains a vertex of maximal degree d.
 * It is a well known fact that we have d <= tw(G) and, thus, we can use the degeneracy of a graph as lowerbound for the tree-width.
 * 
 * This class implements the linear time algorithm from Matula and Beck to compute the degeneracy of a graph.
 * 
 * @param <T>
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
		this.graph = graph.copy();
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
		while (!graph.getVertices().isEmpty()) {
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
			graph.deleteVertex(v);
		}
		
		// done
		return d;
	}

	@Override
	public Integer getCurrentSolution() {
		return null;
	}
	
}
