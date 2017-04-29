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
package jdrasil.graph.invariants;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import jdrasil.graph.Dinic;
import jdrasil.graph.Graph;

/**
 * Computes a minimum separating vertex set, i.e. a vertex set of minimal size such 
 * that its removal splits the graph into at least two connected components.
 * 
 * This class uses the @see Dinic algorithm and should only be called upon small graphs, 
 * as its running time is roughly \(n^3 \cdot m\).
 * 
 * @author Sebastian Berndt
 */
public class MinimalSeparator<T extends Comparable<T>> extends Invariant<T, Integer, Boolean> {

	/** The actual seperator. */
	private Set<T> seperator;
	
	/**
	 * Calls the constructor of @see Invariant() and invokes the computation of the seperator
	 * @param graph that should be decomposed
	 */
	public MinimalSeparator(Graph<T> graph) {
		super(graph);
	}
	
	/* (non-Javadoc)
	 * @see jdrasil.graph.invariants.Invariant#computeModel()
	 */
	@Override
	protected Map<T, Boolean> computeModel() {
		int n = graph.getCopyOfVertices().size();
		// we need a mapping between the nodes of the graph and their indices
		HashMap<T, Integer> mapTI = new HashMap<>();
		HashMap<Integer, T> mapIT = new HashMap<>();

		int c = 0;
		for (T v : graph.getCopyOfVertices()) {
			mapTI.put(v, c);
			mapIT.put(c, v);
			c++;
		}

		// create the temporary graph. For every original node i,
		// it contains and input node i and an output node n+i
		int[][] g = new int[2 * n][2 * n];
		for (T u : graph.getCopyOfVertices()) {
			int mu = mapTI.get(u);
			// the capacities between and input node and its corresponding
			// output node is 1
			g[mu][mu + n] = 1;
			for (T v : graph.getCopyOfVertices()) {
				if (graph.isAdjacent(u, v)) {
					// two adjacent vertices have unbounded edge capacity
					int mv = mapTI.get(v);
					g[mu + n][mv] = Integer.MAX_VALUE;
					g[mv + n][mu] = Integer.MAX_VALUE;
				}
			}
		}

		// the first separator consists of all nodes
		HashSet<T> sep = new HashSet<>();
		for (T u : graph.getCopyOfVertices()) {
			sep.add(u);
		}
		// for every node i, compute a maximum flow that separates i from any
		// other node j
		for(int i = 0;  i < n ; i++)
		for (int j = i + 1; j < n; j++) {
			if (!graph.isAdjacent(mapIT.get(i), mapIT.get(j))) {
				boolean[][] res = new Dinic(g, i, j + n).start();

				// construct the minimimal i-j-separator
				HashSet<T> cand = new HashSet<>();
				for (int l = 0; l < n; l++) {
					if (l != i && l != j) {
						if (res[l][l + n]) {
							cand.add(mapIT.get(l));
						}
					}
				}
				if (cand.size() < sep.size()) {
					sep = cand;
				}
			}
		}

		this.seperator = sep;
		
		// extract model
		Map<T, Boolean> model = new HashMap<>();
		for (T v : graph) {
			if (sep.contains(v)) {
				model.put(v, true);
			} else {
				model.put(v, false);
			}			
		}		
		return model;
	}

	/* (non-Javadoc)
	 * @see jdrasil.graph.invariants.Invariant#computeValue()
	 */
	@Override
	protected Integer computeValue() {
		return seperator.size();
	}

	/* (non-Javadoc)
	 * @see jdrasil.graph.invariants.Invariant#isExact()
	 */
	@Override
	public boolean isExact() {
		return true;
	}
	
	/**
	 * Get the seperator, i.e., the model, as actual set.
	 * @return
	 */
	public Set<T> getSeperator() {
		if (getValue() == 0) return null;
		return seperator;
	}

}
