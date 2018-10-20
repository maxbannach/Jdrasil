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
import java.util.stream.Collectors;

import jdrasil.graph.Graph;
import jdrasil.sat.Formula;

/**
 * A clique in a graph is a subset of its vertices such that all vertices within the clique are pairwise adjacent.
 * If a SAT solver is available, this class computes the maximum clique in the graph.
 * Otherwise, a heuristic is used to compute some clique.
 * 
 * @author Max Bannach
 */
public class Clique<T extends Comparable<T>> extends Invariant<T, Integer, Boolean> {

	/** This is set to true if it is guaranteed that the clique is the maximum clique. */
	private boolean maximum;
	
	/**
	 * @param graph The graph in which we search the clique.
	 */
	public Clique(Graph<T> graph) {
		super(graph);
	}

	/**
	 * Compute the maximum clique of the graph using the SAT solver.
	 * This method can return null if no SAT solver is available or the SAT solver fails.
	 * @return The model of the clique.
	 */
	private Map<T, Boolean> computeModelWithSAT() {

		// compute a bijection from the vertices to {1,...,n}
		Map<T, Integer> vertToInt = new HashMap<>();
		Map<Integer, T> intToVert = new HashMap<>();
		int i = 1;
		for (T v : graph) {
			vertToInt.put(v, i);
			intToVert.put(i++, v);
		}

		// create the base formula
		Formula phi = new Formula();
		for (T v : graph) {
			for (T u : graph) {
				if (u.compareTo(v) >= 0 || graph.isAdjacent(u, v)) continue;
				phi.addClause(-1*vertToInt.get(v), -1*vertToInt.get(u));
			}
		}
		if (phi.numberOfClauses() == 0) return null; // no edges

		// solve the formula
		int k = 1;
		phi.addCardinalityConstraint(k, graph.getCopyOfVertices().size(), intToVert.keySet());

		try {
			phi.registerSATSolver();
			while (phi.isSatisfiable()) {
				phi.addCardinalityConstraint(++k, graph.getCopyOfVertices().size(), intToVert.keySet());
			}
			
			// compute the model
			Map<T, Boolean> clique = new HashMap<>();
			Map<Integer, Boolean> model;
			model = phi.getModel();
			for (T v : graph) clique.put(v, model.get(vertToInt.get(v)));

			// clean up
			phi.unregisterSATSolver();

			// done
			return clique;

		} catch (Exception e) {
			return null;
		}
	}
	
	/* (non-Javadoc)
	 * @see jdrasil.graph.invariants.Invariant#computeModel()
	 */
	@Override
	protected Map<T, Boolean> computeModel() {
		Map<T, Boolean> clique = null;
		
		// if we have a SAT solver -> use it
		if (Formula.canRegisterSATSolver()) {
			this.maximum = true;
			clique = computeModelWithSAT();
		}
		
		// otherwise find a greedy clique in O(n^2)
		if (clique == null) {
			this.maximum = false;
			clique = new HashMap<>();
			for (T v : graph) clique.put(v, false);
			
			Set<T> V = new HashSet<>(graph.getCopyOfVertices()); // vertices that we can consider
			Map<T, Integer> d = new HashMap<>(); // degree vector of remaining vertices
			for (T v : graph) d.put(v, graph.getNeighborhood(v).size());
			while (!V.isEmpty()) {
				
				// find vertex of max degree
				T maxV = null;
				int maxD = -1;
				for (T v : V) {
					if (d.get(v) > maxD) {
						maxV = v;
						maxD = d.get(v);
					}
				}
				
				// add him to the clique
				clique.put(maxV, true); 
				
				// update V and the degree vector
				V.retainAll(graph.getNeighborhood(maxV));				
				for (T v : V) {
					d.put(v, (int) graph.getNeighborhood(v).stream().filter(x -> V.contains(v)).count() );
				}
			}			
		}
				
		// done
		return clique;
	}

	/* (non-Javadoc)
	 * @see jdrasil.graph.invariants.Invariant#computeValue()
	 */
	@Override
	protected Integer computeValue() {
		return (int) getModel().entrySet().stream().filter( x -> x.getValue() ).count();
	}

	/* (non-Javadoc)
	 * @see jdrasil.graph.invariants.Invariant#isExact()
	 */
	@Override
	public boolean isExact() {
		return this.maximum;
	}

	/**
	 * Returns the model as set, i.e., the clique as subset of the vertices.
	 * @return The clique as set.
	 */
	public Set<T> getClique() {
		return getModel().entrySet().stream().filter( x -> x.getValue() ).map( x -> x.getKey()).collect(Collectors.toSet());
	}
	
}
