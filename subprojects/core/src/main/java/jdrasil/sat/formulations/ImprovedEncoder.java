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
import java.util.List;

import jdrasil.graph.Graph;
import jdrasil.sat.Formula;

/**
 * This class produces to a given graph G=(V,E) a formula phi that is satisfiable if, and only if,
 * G has a certain tree-width. The width can be set by adding cardinality constraints to the formula.
 * This encoding is an improvement of the encoding of @see BaseEncoder and was presented in
 * "SAT-Based Approaches to Treewidth Computation: An Evaluation" by Jeremias Berg and Matti Järvisalo.
 * 
 * @author Max Bannach
 */
public class ImprovedEncoder<T extends Comparable<T>> extends BaseEncoder<T> {

	/**
	 * The default constructor that initializes all variables. @see BaseEncoder()
	 * @param graph The graph that we encode.
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
				if (i == j) {
					phi.addClause(-1*arc[i][i]); // no self loops
				} else {
					phi.addClause(-1*arc[j][i], -1*arc[i][j]); // no double edge
				}
			}
		}
		
		// done
		return phi;
	}
	
}
