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
package jdrasil.sat.formulations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jdrasil.graph.Graph;
import jdrasil.sat.Formula;
import jdrasil.sat.encodings.GenericCardinalityEncoder;

/**
 * This class produces to a given graph G=(V,E) a formula phi that is satisfiable if, and only if,
 * G has a certain tree-width. The width can be set by adding cardinality constraints to the formula.
 * This encoding is based on "Encoding Treewidth into SAT" by Marko Samer and Hemlut Veith and is, as far as we known,
 * the first encoding for tree-width into SAT.
 * 
 * @author Max Bannach
 */
public class BaseEncoder<T extends Comparable<T>> {

	/** The SAT formula defining the base encoding*/
	protected Formula phi;
	
	/** The graph for which we which to compute a tree-decomposition. s*/
	protected final Graph<T> graph;
	
	/** A bijection between the vertices and {1,...,n}. */
	protected final Map<T, Integer> vertexToInt = new HashMap<>();
	protected final Map<Integer, T> intToVertex = new HashMap<>();
	
	/** The size of the graph. */
	protected final int n;
	
	/** SAT-variables to store an ordering of the vertices. */
	protected final int[][] ord;
	
	/** SAT-variables to encode a successor relation on the vertices. */
	protected final int[][] arc;
	
	/** A single clique of the graph can be eliminated at the end*/
	protected Set<T> clique = new HashSet<>();
	
	/** AMK constraints that can iteratively be improved. */
	private final Map<T, GenericCardinalityEncoder> amkEncoder;
	
	/**
	 * Default constructor that initializes all the variables.
	 * @param graph
	 */
	public BaseEncoder(Graph<T> graph) {
		this.graph = graph;
		this.n = graph.getVertices().size();
		this.ord = new int[n+1][n+1];
		this.arc = new int[n+1][n+1];
		this.amkEncoder = new HashMap<>();
		
		// compute the bijection
		int varCount = 0;
		for (T v : graph) {
			varCount++;
			vertexToInt.put(v, varCount);
			intToVertex.put(varCount, v);
		}
		varCount = 0;
				
		// variables to encode order of vertices
		for (int i = 1; i <= n; i++) {
			for (int j = i+1; j <= n; j++) {
				varCount++;
				ord[i][j] = varCount;
			}
		}
		
		// variables to encode arcs (consider graph directed with respect to order)
		for (int i = 1; i <= n; i++) {
			for (int j = 1; j <= n; j++) {
				varCount++;
				arc[i][j] = varCount;
			}
		}
		
		// actually compute the encoding
		this.phi = computeBaseEncoding();	
		
		// add an clique
		encodeClique();
		
		// encode twins
		encodeTwins();
	}
	
	/**
	 * Compute the base encoding as defined in "Encoding Treewidth into SAT" by Marko Samer and Helmit Veith.
	 * @return phi
	 */
	Formula computeBaseEncoding() {
		
		Formula phi = new Formula();
		// the order has to be transitive
		for (int i = 1; i <= n; i++) {
			for (int j = 1; j <= n; j++) {
				if (i == j) continue;
				for (int l = 1; l <= n; l++) {
					if (i == l || j == l) continue;
					List<Integer> C = new ArrayList<>(3);
					
					C.add(i < j ? -1*ord[i][j] : ord[j][i]);
					C.add(j < l ? -1*ord[j][l] : ord[l][j]);
					C.add(i < l ? ord[i][l] : -1*ord[l][i]);
		
					phi.addClause(C);
				}
			}
		}
		
		// encode edges of the graph
		for (T u : graph) {
			for (T v : graph.getNeighborhood(u)) {
				int i = vertexToInt.get(u);
				int j = vertexToInt.get(v);
				if (i < j) {
					phi.addClause(-1*ord[i][j], arc[i][j]);
					phi.addClause(ord[i][j], arc[j][i]);
				}
			}
		}
		
		// encode edges that are added during elimination
		for (int i = 1; i <= n; i++) {
			for (int j = 1; j <= n; j++) {
				if (i == j) continue;
				for (int l = j+1; l <= n; l++) {	
					if (i == l) continue;
					phi.addClause(
						-1*arc[i][j], -1*arc[i][l], -1*ord[j][l], arc[j][l]	
					);
					phi.addClause(
							-1*arc[i][j], -1*arc[i][l], ord[j][l], arc[l][j]	
						);
					
					// redundant, but seep up solver
					phi.addClause(
							-1*arc[i][j], -1*arc[i][l], arc[j][l], arc[l][j]	
						);
				}
			}
		}
		
		// forbid self loops
		for (int i = 1; i <= n; i++) {
			phi.addClause(-1*arc[i][i]);
		}
		
		// done
		return phi;
	}
	
	/**
	 * Given a fixed clique C of the graph, there is an optimal elimination order that deletes the vertices of C at last.
	 * Hence, we can hard-code the order of a (maximal) clique into the formula in order to break symmetries and to reduce the search space.
	 * 
	 * This method computes a maximum clique using a SAT-solver and encode it into phi.
	 */
	protected void encodeClique() {
		Set<T> clique = graph.getMaximumClique(5);
		if (clique == null) return;
		
		// All vertices not in the clique are ordered before the clique
		for (T v : graph) {
			if (clique.contains(v)) continue;
			for (T u : clique) {
				int i = vertexToInt.get(v);
				int j = vertexToInt.get(u);
				if (i < j) {
					phi.addClause(ord[i][j]);
				} else {
					phi.addClause(-1*ord[j][i]);
				}
			}
		}
		
		// vertices of the clique are order lexicographically
		for (T u : clique) {
			for (T v : clique) {
				if (u.compareTo(v) < 0) {
					int i = vertexToInt.get(u);
					int j = vertexToInt.get(v);
					if (i < j) {
						phi.addClause(ord[i][j]);
					} else {
						phi.addClause(-1*ord[j][i]);
					}
				}
			}
		}
		
		// store the clique
		this.clique = clique;
	}
	
	/**
	 * For any two twin vertices, say u and v, the order in any optimal elimination is not important.
	 * Thus, we can hard code the ordering of u and v.
	 * 
	 * Since a clique is also hard coded, this method has to be called after @see encodeClique() to avoide conflicts.
	 */
	protected void encodeTwins() {
		Map<T, Set<T>> twins = graph.getTwinDecomposition();
		for (Set<T> group : twins.values()) {
			if (group.size() <= 1) continue;
			
			// vertices of a twin class are order lexicographically
			for (T u : group) {
				if (clique.contains(u)) continue;
				for (T v : group) {
					if (clique.contains(v)) continue;
					if (u.compareTo(v) < 0) {
						int i = vertexToInt.get(u);
						int j = vertexToInt.get(v);
						if (i < j) {
							phi.addClause(ord[i][j]);
						} else {
							phi.addClause(-1*ord[j][i]);
						}
					}
				}
			}			
		}
		
	}
	
	/**
	 * Initialize the cardinality constraint (a sequential counter that is used multiple times)
	 * @param lb
	 * @param ub
	 */
	public void initCardinality(int lb, int ub) {
		// outgoing edges during elimination define tree-width, thus, all vertices can have at most k outgoing edges
		for (T u : graph) {
			int i = vertexToInt.get(u);
			Set<Integer> C = new HashSet<>();
			for (int j = 1; j <= n; j++) {
				C.add(arc[i][j]);
			}
			GenericCardinalityEncoder encoder = new GenericCardinalityEncoder(phi, C, lb, ub);
			amkEncoder.put(u, encoder);
		}
	}
	
	/**
	 * Add constraints to phi, such that phi is satisfiable if, and only if, the initial graph has tree-width at most k.
	 * @param k
	 */
	public Formula addAtMost(int k) {
		Formula psi = new Formula();
		psi.setHighestVariable(phi.getHighestVariable());
		
		// just update constraint using existent encoder
		for (T u : graph) {
			amkEncoder.get(u).addAMK(psi, k);
		}

		phi.setHighestVariable(psi.getHighestVariable());
		return psi;
	}
	
	/**
	 * Getter for the actual, current formula that encodes that the initial graph has tree-width at most k.
	 * @return
	 */
	public Formula getFormula() {
		return phi;
	}
	
	/**
	 * Given a model for the formula, this method computes an actual permutation of the vertices such that the elimination
	 * of this permutation yields to an tree-decomposition of size at most k.
	 * @param model
	 * @return
	 */
	public List<T> getPermutation(Map<Integer, Boolean> model) {
		List<T> permutation = new ArrayList<>(n);
		for (T v : graph) permutation.add(v);
		
		// the ord variables encoder a total order, i.e. we can use them to sort
		permutation.sort((u,v) -> {			
			int i = vertexToInt.get(u);
			int j = vertexToInt.get(v);
			if (i < j) {
				return model.get(ord[i][j]) ? -1 : 1;
			} else {
				return model.get(ord[j][i]) ? 1 : -1;
			}
		});
		
		// done
		return permutation;
	}
	
}
