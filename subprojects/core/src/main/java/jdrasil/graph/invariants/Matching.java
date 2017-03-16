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
import java.util.Map;
import java.util.Set;

import jdrasil.graph.Graph;

/**
 * A matching in a graph \(G=(V,E)\) is a subset of the edges \(M\subseteq E\) such that for every vertex \(v\in V\) we have 
 * \(|\{\,e\mid e\in M\wedge v\in e\,\}|\leq 1\). A matching is maximal if we can not increase it by adding any edge. It is a maximum matching,
 * if there is no bigger matching in the graph, and it is perfect if every vertex is matched.
 * 
 * This class greedily computes a maximal matching, i.e., it is not guaranteed that it is a maximum matching.
 * The matching is represented as map from vertices to vertices, i.e., a vertex is mapped to the vertex it is matched with.
 *  
 * @author Max Bannach
 */
public class Matching<T extends Comparable<T>> extends Invariant<T, Integer, T> {

	/**
	 * @param graph
	 */
	public Matching(Graph<T> graph) {
		super(graph);
	}

	/* (non-Javadoc)
	 * @see jdrasil.graph.invariants.Invariant#computeModel()
	 */
	@Override
	protected Map<T, T> computeModel() {
		Map<T, T> matching = new HashMap<>();
		
		// greedy match vertices
		for (T v : graph) {
			if (matching.containsKey(v)) continue; // already matched
			for (T w : graph.getNeighborhood(v)) {
				if (matching.containsKey(w)) continue;
				// we can match
				matching.put(v, w);
				matching.put(w, v);
				break;
			}
		}
		
		// done
		return matching;
	}

	/* (non-Javadoc)
	 * @see jdrasil.graph.invariants.Invariant#computeValue()
	 */
	@Override
	protected Integer computeValue() {
		return getModel().size()/2;
	}

	/* (non-Javadoc)
	 * @see jdrasil.graph.invariants.Invariant#isExact()
	 */
	@Override
	public boolean isExact() {
		return false;
	}

}
