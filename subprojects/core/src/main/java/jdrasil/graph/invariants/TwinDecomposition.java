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

import jdrasil.graph.Graph;
import jdrasil.datastructures.PartitionRefinement;

/**
 *
 * Compute all twins of the graph in time O(n+m) using the partition refinement paradigm.
 * Two vertices v, w are true twins if N[v]=N[w], and they are false twins if N(v)=N(w).
 * 
 * This method can be used to compute both kinds of twins, regulated with the given boolean parameter.
 * 
 * @author Max Bannach
 */
public class TwinDecomposition<T extends Comparable<T>> extends Invariant<T, Integer, Set<T>> {

	/**
	 * @param graph
	 */
	public TwinDecomposition(Graph<T> graph) {
		super(graph);
	}

	/* (non-Javadoc)
	 * @see jdrasil.graph.invariants.Invariant#computeModel()
	 */
	@Override
	protected Map<T, Set<T>> computeModel() {
		Map<T, Set<T>> trueTwins = getTwinDecomposition(true);
		Map<T, Set<T>> falseTwins = getTwinDecomposition(false);
		Map<T, Set<T>> twins = new HashMap<T, Set<T>>();
		for (T v : graph) {
			if (trueTwins.get(v).size() > falseTwins.get(v).size()) {
				twins.put(v, trueTwins.get(v));
			} else {
				twins.put(v, falseTwins.get(v));
			}
		}
		return twins;
	}

	/* (non-Javadoc)
	 * @see jdrasil.graph.invariants.Invariant#computeValue()
	 */
	@Override
	protected Integer computeValue() {
		int max = -1;
		for (T v : graph) {
			if (getModel().get(v).size() > max)
				max = getModel().get(v).size();
		}
		return max;
	}

	/* (non-Javadoc)
	 * @see jdrasil.graph.invariants.Invariant#isExact()
	 */
	@Override
	public boolean isExact() {
		return true;
	}
	
	/**
	 * Compute all twins of the graph in time \(O(n+m)\) using the partition refinement paradigm.
	 * Two vertices v, w are true twins if \(N[v]=N[w]\), and they are false twins if \(N(v)=N(w)\).
	 * 
	 * This method can be used to compute both kinds of twins, regulated with the given boolean parameter.
	 * 
	 */
	private Map<T, Set<T>> getTwinDecomposition(boolean trueTwins) {
		PartitionRefinement<T> P = new PartitionRefinement<>(graph.getCopyOfVertices());
		for (T v : graph) {
			Set<T> Nv = new HashSet<T>(graph.getNeighborhood(v)); 
			if (trueTwins) Nv.add(v);
			P.refine(Nv);
		}
		return P.getPartition();
	}

}
