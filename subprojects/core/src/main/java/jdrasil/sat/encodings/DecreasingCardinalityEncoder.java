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
import java.util.List;
import java.util.Set;

import jdrasil.sat.Formula;

/**
 * An decreasing cardinality constraint is an "atMostK" constraint that is reduced between multiple calls 
 * of the SAT solver. In this sense, it is an incremental cardinality constraint as implemented by @see IncrementalCardinaltiyEncoder.
 * 
 * This class implements an sequential counter that can be reduced. The advantage (and different) to  @see IncrementalCardinaltiyEncoder
 * is that this class will introduce O(k*n) variables (vs O(n* log^2 n) of @see IncrementalCardinaltiyEncoder), which may be prefarable
 * for small k. Note that k in Jdrasil is often the tree width of the target graph and, thus, a small parameter.
 * 
 * @author Max Bannach
 */
public class DecreasingCardinalityEncoder {

	/** The formula that is constrained by this class. */
	private Formula phi;
	
	/** The currently encoded k. */
	private int k;
	
	/** The variables used by the sequential counter. */
	private List<Integer> inputVariables;
	
	/** The variables encoding the sequential counter */
	private int[][] sequentialCounter;
	
	/**
	 * The constructor will encode the sequential counter (i.e., modify the formula), but will not yet
	 * add an constraint to it.
	 * @param phi The formula to which the constraint is added,
	 * @param k The bound.
	 * @param variables The variables to which the constraint is applied.
	 */
	public DecreasingCardinalityEncoder(Formula phi, int k, Set<Integer> variables) {
		this.phi = phi;
		this.k = k+1;
		addSequentialCounter(variables);
		addAtMost(k);
	}
	
	/**
	 * Encodes the base sequential counter into the formula.
	 * After this, k can only be decreased.
	 * @param variables The variables to which the constraint is applied.
	 */
	private void addSequentialCounter(Set<Integer> variables) {
		inputVariables = new ArrayList<>(variables);
		inputVariables.add(0,0);				
		int n = variables.size();
		int currentVar = phi.getHighestVariable() + 1;
		
		// new variables encoding the register (i.e., the counting circuit)
		sequentialCounter = new int[n+1][k+1];
		for (int i = 1; i <= n; i++) {
			for (int j = 1; j <= k; j++) {
				sequentialCounter[i][j] = currentVar;
				currentVar = currentVar + 1;
			}
		}
		
		// equation 1
		for (int i = 1; i <= n-1; i++) {
			phi.addClause(-1*inputVariables.get(i), sequentialCounter[i][1]);
		}
		
		// equation 2
		for (int j = 2; j <= k; j++) {
			phi.addClause(-1*sequentialCounter[1][j]);
		}
		
		// equation 3
		for (int i = 2; i <= n-1; i++) {
			for (int j = 1; j <= k; j++) {
				phi.addClause(-1*sequentialCounter[i-1][j], sequentialCounter[i][j]);
			}
		}
		
		// equation 4
		for (int i = 2; i <= n-1; i++) {
			for (int j = 2; j <= k; j++) {
				phi.addClause(-1*inputVariables.get(i), -1*sequentialCounter[i-1][j-1], sequentialCounter[i][j]);
			}
		}
		
		// equation 5
		for (int i = 2; i <= n; i++) {
			phi.addClause(-1*inputVariables.get(i), -1*sequentialCounter[i-1][k]);
		}
		
		// finally, mark auxiliary variables as such
		for (int i = 1; i <= n; i++) {
			for (int j = 1; j <= k; j++) {
				phi.markAuxiliary(sequentialCounter[i][j]);
			}
		}
	}
	
	/**
	 * Adds an atMostK constraint to the formula. This will only add new clauses, not new variables.
	 * This method has only an effect if k is smaller then the last k and is not negative.
	 * @param k The bound.
	 */
	public void addAtMost(int k) {
		if (k < 0 || k >= this.k) return;
		this.k = k;
		for (int i = 2; i < inputVariables.size(); i++) {
			phi.addClause(-1*inputVariables.get(i), -1*sequentialCounter[i-1][k]);
		}
	}
	
}
