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
package jdrasil.sat.encodings;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import jdrasil.sat.Formula;

/**
 * This class provides methods to encode cardinality constraints into a formula of propositional logic.
 * Cardinality constraints are constraints of the form At-Most-k (AMK) and At-Least-k (ALK), which force that a certain
 * number of variables in the formula is set to true. Such encodings are commonly used and, therefore, it is important to
 * choose efficient versions of the encoding.
 * 
 * The paper "Sat Encodings of the At-Most-k Constraint" from Alan M. Frisch and Paul A. Giannaros provides a nice
 * overview of possible encodings. This class orients itself on this paper.
 * 
 * @author Max Bannach
 */
public class BasicCardinalityEncoder {

	/** Hide the constructor. */
	private BasicCardinalityEncoder() {}
			
	//MARK: Binomial Encoding
	
	/** @see BasicCardinalityEncoder#binomialAMK(jdrasil.sat.Formula, java.util.Set, int) */
	private static void binomialAMK(Formula phi, List<Integer> variables, List<Integer> C, int k, int pos) {
		
		// construction of subset complete
		if (k == 0) {
			phi.addClause(new ArrayList<>(C));
			return;
		}
		
		// end of recursion, not enough variables to complete subset
		if (pos == variables.size()) return;
		
		// for each remaining variable, we may add it or do not add it
		for (int i = pos; i < variables.size(); i++) {
			C.add(-1 * variables.get(i));
			binomialAMK(phi, variables, C, k-1, i+1);
			C.remove(C.size()-1);
		}
	}
	
	/**
	 * Add an At-Most-k cardinality constraint for the given set of variables to the formula.
	 * This method encodes the constraint using the binomial encoding, which uses n^k new clauses, but
	 * does not introduce new variables.
     * @param phi The formula to which the constraint is added,
     * @param variables The variables to which the constraint is applied.
     * @param k The bound.
	 */
	public static void binomialAMK(Formula phi, Set<Integer> variables, int k) {
		binomialAMK(phi, new ArrayList<>(variables), new LinkedList<>(), k+1, 0);
	}
	
	/**
	 * Add an At-Least-k cardinality constraint for the given set of variables to the formula.
	 * This method encodes the constraint using the binomial At-Most-(n-k) constraint.
     * @param phi The formula to which the constraint is added,
     * @param variables The variables to which the constraint is applied.
     * @param k The bound.
	 */
	public static void binomialALK(Formula phi, Set<Integer> variables, int k) {
		List<Integer> neg = new ArrayList<>(variables);
		for (int i = 0; i < neg.size(); i++) neg.set(i, -1*neg.get(i));
		binomialAMK(phi, neg, new LinkedList<>(), variables.size()-k+1, 0);
	}

	//MARK: Binary Encoding
	
	/**
	 * Add an At-Most-k cardinality constraint to the given formula using the binary encoding.
	 * This encoding introduce O(kn log n) clauses and O(kn) new variables.
     * @param phi The formula to which the constraint is added,
     * @param variables The variables to which the constraint is applied.
     * @param k The bound.
	 */
	public static void binaryAMK(Formula phi, Set<Integer> variables, int k) {
		List<Integer> vars = new ArrayList<>(variables);
		int n = variables.size();
		int logn = (int) Math.ceil(Math.log(n)/Math.log(2));
		int currentVar = phi.getHighestVariable()+1;
		
		// variables for the binary strings
		int[][] B = new int[k+1][logn+1];
		for (int i = 1; i <= k; i++) {
			for (int j = 1; j <= logn; j++) {
				B[i][j] = currentVar;
				currentVar = currentVar + 1;
			}
		}
		
		// variables to encode the formula as CNF
		int[][] T = new int[k+1][n+1];
		for (int g = 1; g <= k; g++) {
			for (int i = 1; i <= n; i++) {
				T[g][i] = currentVar;
				currentVar = currentVar + 1;
			}
		}
		
		// actually add the encoding
		for (int i = 1; i <= n; i++) {
			
			// left clause
			List<Integer> C = new ArrayList<>();
			C.add(-1 * vars.get(i-1));
			for (int g = Math.max(1, k-n+i); g <= Math.min(i,k); g++) {
				C.add(T[g][i]);
			}
			phi.addClause(C);
			
			// right clause
			for (int g = Math.max(1, k-n+i); g <= Math.min(i,k); g++) {
				for (int j = 1; j <= logn; j++) {
					if (((i >> (j-1)) & 1) == 1) {
						phi.addClause(-1*T[g][i], B[g][j]);
					} else {
						phi.addClause(-1*T[g][i], -1*B[g][j]);
					}
				}
			}
		}
		
		// finally, mark auxiliary variables as such
		for (int i = 1; i <= k; i++) {
			for (int j = 1; j <= logn; j++) {
				phi.markAuxiliary(B[i][j]);
			}
		}
		for (int g = 1; g <= k; g++) {
			for (int i = 1; i <= n; i++) {
				phi.markAuxiliary(T[g][i]);
			}
		}
	}
	
	/**
	 * Add an At-Least-k cardinality constraint for the given set of variables to the formula.
	 * This method encodes the constraint using the binary At-Most-(n-k) constraint.
     * @param phi The formula to which the constraint is added,
     * @param variables The variables to which the constraint is applied.
     * @param k The bound.
	 */
	public static void binaryALK(Formula phi, Set<Integer> variables, int k) {
		Set<Integer> neg = new HashSet<>();
		for (Integer v : variables) neg.add(-1*v); 
		binaryAMK(phi, neg, variables.size()-k);
	}
	
	//MARK: Sequential Counter Encoding
	
	/**
	 * Add an At-Most-k cardinality constraint to the given formula using the sequential counter encoding.
	 * This encoding introduce O(kn) clauses and O(kn) new variables.
     * @param phi The formula to which the constraint is added,
     * @param variables The variables to which the constraint is applied.
     * @param k The bound.
	 */
	public static void sequentialAMK(Formula phi, Set<Integer> variables, int k) {
		
		List<Integer> X = new ArrayList<>(variables);
		X.add(0,0);				
		int n = variables.size();
		int currentVar = phi.getHighestVariable() + 1;
		
		// new variables encoding the register (i.e., the counting circuit)
		int[][] R = new int[n+1][k+1];
		for (int i = 1; i <= n; i++) {
			for (int j = 1; j <= k; j++) {
				R[i][j] = currentVar;
				currentVar = currentVar + 1;
			}
		}
		
		// equation 1
		for (int i = 1; i <= n-1; i++) {
			phi.addClause(-1*X.get(i), R[i][1]);
		}
		
		// equation 2
		for (int j = 2; j <= k; j++) {
			phi.addClause(-1*R[1][j]);
		}
		
		// equation 3
		for (int i = 2; i <= n-1; i++) {
			for (int j = 1; j <= k; j++) {
				phi.addClause(-1*R[i-1][j], R[i][j]);
			}
		}
		
		// equation 4
		for (int i = 2; i <= n-1; i++) {
			for (int j = 2; j <= k; j++) {
				phi.addClause(-1*X.get(i), -1*R[i-1][j-1], R[i][j]);
			}
		}
		
		// equation 5
		for (int i = 2; i <= n; i++) {
			phi.addClause(-1*X.get(i), -1*R[i-1][k]);
		}
		
		// finally, mark auxiliary variables as such
		for (int i = 1; i <= n; i++) {
			for (int j = 1; j <= k; j++) {
				phi.markAuxiliary(R[i][j]);
			}
		}
	}
	
	/**
	 * Add an At-Least-k cardinality constraint for the given set of variables to the formula.
	 * This method encodes the constraint using the sequential counter At-Most-(n-k) constraint.
     * @param phi The formula to which the constraint is added,
     * @param variables The variables to which the constraint is applied.
     * @param k The bound.
	 */
	public static void sequentialALK(Formula phi, Set<Integer> variables, int k) {
		
		List<Integer> X = new ArrayList<>(variables);
		X.add(0,0);				
		int n = variables.size();
		int currentVar = phi.getHighestVariable() + 1;
		
		// new variables encoding the register (i.e., the counting circuit)
		int[][] R = new int[n+1][k+1];
		for (int i = 1; i <= n; i++) {
			for (int j = 1; j <= k; j++) {
				R[i][j] = currentVar;
				currentVar = currentVar + 1;
			}
		}
		
		// equation 1
		phi.addClause(-R[1][1], X.get(1));
		
		// equation 2
		for (int j = 2; j <= k; j++) {
			phi.addClause(-R[1][j]);
		}
		
		// equation 3
		for (int i = 2; i<= n; i++) {
			for (int j = 2; j <= k; j++) {
				phi.addClause(-R[i][j], R[i-1][j-1]);
			}
		}
		
		// equation 4
		for (int i = 2; i<= n; i++) {
			for (int j = 1; j <= k; j++) {
				phi.addClause(-R[i][j], R[i-1][j], X.get(i));
			}
		}
		
		// equation 5
		phi.addClause(R[n][k]);
		
	}
	
}
