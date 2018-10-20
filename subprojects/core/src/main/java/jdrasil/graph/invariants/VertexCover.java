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
import java.util.stream.Collectors;

import jdrasil.graph.Graph;
import jdrasil.sat.Formula;

/**
 * A vertex-cover of a graph is a subset of its vertices such that every edge is incident to at least one of these vertices.
 * If a SAT solver is available, this class will compute the minimal vertex-cover. Otherwise, a simple 2-approximation is computed.
 * 
 * @author Max Bannach
 */
public class VertexCover<T extends Comparable<T>> extends Invariant<T, Integer, Boolean> {

	/** Is true when it is guaranteed that the vertex-cover is minimal. */
	private boolean minimal;
	
	/**
	 * Calls the constructor of @see Invariant() and invokes the computation of the vertex-cover
	 * @param graph that should be decomposed
	 */
	public VertexCover(Graph<T> graph) {
		super(graph);
		this.minimal = false;
	}
	
	/**
	 * Formulate the vertex-cover problem as SAT instance and solve it with a SAT solver (if possible).
	 * Can return null, if there is no SAT solver or the SAT solver fails.
	 * @return The model as map.
	 */
	private Map<T, Boolean> computeWithSAT() {
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
			for (T u : graph.getNeighborhood(v)) {
				if (u.compareTo(v) >= 0) continue;
				phi.addClause(vertToInt.get(v), vertToInt.get(u));
			}
		}

		// solve the formula
		int k = graph.getCopyOfVertices().size();
		phi.addCardinalityConstraint(0, k, intToVert.keySet());

		try {
			phi.registerSATSolver();
			while (phi.isSatisfiable()) {
				phi.addCardinalityConstraint(0, --k, intToVert.keySet());
			}
			
			// compute the model
			Map<T, Boolean> vertexCover = new HashMap<>();
			Map<Integer, Boolean> model;
			model = phi.getModel();
			for (T v : graph) vertexCover.put(v, model.get(vertToInt.get(v)));

			// clean up
			phi.unregisterSATSolver();

			// done
			return vertexCover;

		} catch (Exception e) {
			return null;
		}
	}

	/* (non-Javadoc)
	 * @see jdrasil.graph.invariants.Invariant#computeModel()
	 */
	@Override
	protected Map<T, Boolean> computeModel() {
		Map<T, Boolean> vertexCover = null;
		
		// if we have a SAT solver, solve the problem exactly
		if (Formula.canRegisterSATSolver()) {
			vertexCover = computeWithSAT();
			this.minimal = true;
		}
		
		// if we do not, or if the SAT solver failed, compute a simple approximation
		if (vertexCover == null) {
			this.minimal = false;
			// compute simple 2-approximation
			vertexCover = new HashMap<>();
			for (T v : graph) vertexCover.put(v, false);
			for (T v : graph) {				
				for (T u : graph) {
					if (u.compareTo(v) >= 0) continue;
					if (!vertexCover.get(v) && !vertexCover.get(u)) vertexCover.put(v, true);
				}
			}
		}
		
		// done
		return vertexCover;
	}

	/* (non-Javadoc)
	 * @see jdrasil.graph.invariants.Invariant#computeValue()
	 */
	@Override
	protected Integer computeValue() {
		return (int) this.getModel().values().stream().filter(x -> x).count();
	}

	/* (non-Javadoc)
	 * @see jdrasil.graph.invariants.Invariant#isExact()
	 */
	@Override
	public boolean isExact() { return minimal; }
	
	/**
	 * Compute a set representation of the model. I.e., a set containing all vertices in the vertex-cover.
	 * @return The vertex-cover as set of vertices.
	 */
	public Set<T> getCover() {		
		return getModel().entrySet().stream().filter(x -> x.getValue()).map(x -> x.getKey()).collect(Collectors.toSet());		
	}

}
