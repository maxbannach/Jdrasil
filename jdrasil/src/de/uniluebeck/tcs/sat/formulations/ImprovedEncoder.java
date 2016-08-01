package de.uniluebeck.tcs.sat.formulations;

/**
 * ImprovedEncoder.java
 * @author bannach
 */

import java.util.ArrayList;
import java.util.List;

import de.uniluebeck.tcs.graph.Graph;
import de.uniluebeck.tcs.sat.Formula;

/**
 * This class produces to a given graph G=(V,E) a formula phi that is satisfiable if, and only if,
 * G has a certain tree-width. The width can be set by adding cardinality constraints to the formula.
 * This encoding is an improvement of the encoding of @see BaseEncoder and was presented in
 * "SAT-Based Approaches to Treewidth Computation: An Evaluation" by Jeremias Berg and Matti Järvisalo.
 */
public class ImprovedEncoder<T extends Comparable<T>> extends BaseEncoder<T> {

	/**
	 * The default constructor that initializes all variables. @see BaseEncoder()
	 * @param graph
	 */
	public ImprovedEncoder(Graph<T> graph) {
		super(graph);	
	}

	@Override
	Formula computeBaseEncoding() {
		Formula phi = new Formula();
		
		// the order has to be transitive (3)
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
		
		// Edges {u,v} in E have to be in the triangluation (4)
		for (T u : graph) {
			for (T v : graph.getNeighborhood(u)) {
				if (u.compareTo(v) < 0) {
					int i = vertexToInt.get(u);
					int j = vertexToInt.get(v);
					phi.addClause(arc[i][j], arc[j][i]);
				}
			}
		}
		
		// if u and v have a common predecessor then the graph contains the edge {u,v} (5)
		for (int i = 1; i <= n; i++) {
			for (int j = 1; j <= n; j++) {
				if (i == j) continue;
				for (int k = 1; k <= n; k++) {
					if (i == k || j == k) continue;
					phi.addClause(-1*arc[k][i], -1*arc[k][j], arc[i][j], arc[j][i]);
				}
			}
		}
		
		// if {u,v} is an edge in the triangluated graph, then the choice of (u,v) or (v,u) depend on ord (6)
		for (int i = 1; i <= n; i++) {
			for (int j = 1; j <= n; j++) {
				if (i == j) continue;
				if (i < j) {
					phi.addClause(-1*ord[i][j], -1*arc[j][i]);
				} else {
					phi.addClause(ord[j][i], -1*arc[j][i]);
				}
			}
		}
		
		// domain specific redundant clauses (7)
		for (int i = 1; i <= n; i++) {
			for (int j = 1; j <= n; j++) {
				if (i == j) continue;
				phi.addClause(-1*arc[j][i], -1*arc[i][j]);
			}
		}
		
		// done
		return phi;
	}
	
}
