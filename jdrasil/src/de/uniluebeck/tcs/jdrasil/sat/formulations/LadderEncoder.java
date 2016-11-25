package de.uniluebeck.tcs.jdrasil.sat.formulations;

/**
 * LadderEncoder.java
 * @author bannach
 */

import de.uniluebeck.tcs.jdrasil.graph.Graph;

/**
 * This encoding enriches the base encoding (or the improvement encoding) by adding a unary representation of the the position of a vertex
 * in the elimination order to the formula. This representation allows fast evaluation of properties of the encoding and let the SAT-solver
 * detect symmetric permutation long before all ord_i,j are set.
 * 
 * @param <T>
 */
public class LadderEncoder<T extends Comparable<T>> extends ImprovedEncoder<T>  {

	/** The ladder encoding. */
	private final int[][] l;
	
	/**
	 * The default constructor that initializes all variables. @see BaseEncoder()
	 * @param graph
	 */
	public LadderEncoder(Graph<T> graph) {
		super(graph);
		
		// constitute the new variables
		int currentVar = phi.getHighestVariable() + 1;
		l = new int[n+1][n+1];
		for (int i = 1; i <= n; i++) {
			for (int k = 1; k <= n; k++) {
				l[i][k] = currentVar;
				phi.markAuxiliary(currentVar);
				currentVar = currentVar + 1;
			}
		}
		
		// add the encoding
		addLadderEncoding();
	}

	/**
	 * This method add the subformulas for the ladder encoding.
	 */
	private void addLadderEncoding() {
		
		// ensure that l_i^k encodes an unary number
		for (int i = 1; i <= n; i++) {
			for (int k = 1; k <= n-1; k++) {
				phi.addClause(l[i][k], -1*l[i][k+1]);
			}
		}
		
		// ensure that all unary numbers are unique (i.e., constitute a permutation)
		for (int i = 1; i <= n; i++) {
			for (int j = 1; j <= n; j++) {
				if (i == j) continue;
				for (int k = 1; k <= n-1; k++) {
					phi.addClause(-1*l[i][k], l[i][k+1], -1*l[j][k], l[j][k+1]);
				}
			}
		}
		
		// add a lexicographic ordering to vertices that are adjacent in the ordering
		for (int i = 1; i <= n; i++) {
			for (int j = i+1; j <= n; j++) {
				for (int k = 2; k <= n-1; k++) {
					phi.addClause(-1*l[i][k], l[i][k+1], -1*l[j][k-1], l[j][k]);
				}
			}
		}
		
		// imply the ord_i,j from the ladder encoding
		for (int i = 1; i <= n; i++) {
			for (int j = 1; j <= n; j++) {
				if (i == j) continue;
				for (int k = 1; k <= n; k++) {
					if (i < j) {
						phi.addClause(-1*l[i][k],l[j][k], ord[i][j]);
					} else {
						phi.addClause(-1*l[i][k],l[j][k], -1*ord[j][i]);
					}
				}
			}
		}	
	}
	
}
