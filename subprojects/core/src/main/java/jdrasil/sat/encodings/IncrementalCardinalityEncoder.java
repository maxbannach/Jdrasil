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

import java.util.Set;

import jdrasil.sat.Formula;

/**
 * This class implements an incremental cardinality constraint for a given (fixed) formula 
 * and a given (fixed) set of variables.
 * A cardinality constraint is a constraint that states that at least lb and at most ub many variables from
 * the given set are allowed to be set to true.
 * 
 * This class is based on a sorting network (Batchers Odd-Even Mergesort): The idea is to sort the given set of variables,
 * a atLeast lb constraint then simply states that the n-lb'th element of the sorted output must be set to true, an atMost ub
 * constraint states that the n-ub-1'th element must be false.
 * 
 * Example: Say the variables we wish to constrain are 1 2 3 4, the network will generate auxiliary variables, say 8 10 11 9
 * (the model of these variables will be sorted, not the number of variables of course; many other variables and clauses will be added as well). * 
 * A model could now look like:
 * 
 * <p>1 2 3 4 \(\rightarrow\) 8 10 11 9</p>
 * <p>+ - + - \(\rightarrow\) -  -  + +</p>
 * <p>or</p>
 * <p>1 2 3 4 \(\rightarrow\) 8 10 11 9</p>
 * <p>+ + + - \(\rightarrow\) -  +  + +</p>
 * 
 * During initialization, the network will be computed and added to the formula. After this happened (once), one can add cardinality constraints
 * by adding unit clauses for the output gates, either over this class or directly. Note that, since these are unit clauses, cardinality constraints
 * can easily be used as assumptions, for instances for an binary search.
 * 
 * Note that a sorting network introduces a lot of auxiliary variables and clauses. This may not be worth it, if a single cardinality constraint is 
 * generated. In such cases, the @see BasicCardinalityEncoder should be used. If, however, many cardianlity constraints are generted incrementally, a
 * sorting network is worth its weight. 
 * Furthermore, a sorting network may outperform a more direct counter for "huge cardinality constraints", i.e., if k has a magnitude of n.
 * 
 * 
 * @author Max Bannach
 */
public class IncrementalCardinalityEncoder {

	/** The formula that is constrained by this class. */
	private Formula phi;
	
	/** The output gates (in sorted model order) of the network. */
	private int[] output;

	/** Number of real (not dummy) variables (input / output gates) of the network. */
	private int n;
	
	/** Number of variables (including dummies) of the network. */
	private int size;
	
	/**
	 * The constructor will compute the sorting network and add it to the formula.
	 * I.e., this method _will modify_ phi already. 
	 * The constructor will not add any cardinality constraints what so ever, just the sorting network.
	 * @param phi The formula.
	 * @param variables The variables affected by the constraint.
	 */
	public IncrementalCardinalityEncoder(Formula phi, Set<Integer> variables) {
		this.phi = phi;
		this.n = variables.size();
	
		// compute number of I/O gates (next power of two of n)
		if ( n > 0 && (n & (n-1)) == 0 ) { // n is power of 2
			this.size = this.n;
		} else { // we need dummies
			this.size = 1;
			while (this.size < this.n) this.size = this.size << 1; // size to next power of 2
		}

		// the output gates are just the variables, this will be substituted during the computation of the network.
		this.output = new int[this.size];		
		int i = 0;
		for (Integer v : variables) output[i++] = v;
		while (i < this.size) { // will with new auxiliary variables that have to be true
			output[i] = phi.newAuxillaryVariable();
			phi.addClause(output[i]);
			i++;
		}
		
		// add the sorting network to the formula
		computeNetwork(0, this.size-1);
	}
	
	//MARK: Methods to compute the network
	
	/**
	 * Computes the sorting network for the output gates in the given range (i.e., "sorts" the given range).
	 * This method will call its save recursively and will change the variables in @see output to variables that represent the output
	 * of comparators. 
	 * @param low The left index.
	 * @param high The right index.
	 */
	private void computeNetwork(int low, int high) {
		if (low >= high) return;
		int mid = low + ((high-low) / 2);
		computeNetwork(low, mid);
		computeNetwork(mid+1, high);
		merge(low, high, 1);
	}
	
	/**
	 * Merge to "sorted" lists to a new one, i.e., replaces variables by the result of comparators.
	 * @param low The left index.
	 * @param high The right index.
	 * @param r The depth of the network.
	 */
	private void merge(int low, int high, int r) {
		int step = 2 * r;
		if (step < high - low) {
			merge(low, high, step);
			merge(low + r, high, step);
			for (int i = low + r; i < high - r; i += step) addComparator(i, i + r);
		} else {
			addComparator(low, low + r);
		}
	}

	/**
	 * Replaces the variables output[i] and output[j] with new auxiliary variables, that represent
	 * the result of a comparison of the two values (of a corresponding model, not the variable names).
	 * @param i The first index.
	 * @param j The second index.
	 */
	private void addComparator(int i, int j) {

		// output variables for the comparator
		int x = phi.newAuxillaryVariable();
		int y = phi.newAuxillaryVariable();

		// x = min(a[i], a[j]) = a[i] ^ a[j]
		phi.addClause(-x, output[i]);
		phi.addClause(-x, output[j]);
		phi.addClause(-output[i], -output[j], x);

		// y = max(a[i], a[j]) = a[i] v a[j]
		phi.addClause(-y, output[i], output[j]);
		phi.addClause(-output[i], y);
		phi.addClause(-output[j], y);

		// replace variables
		output[i] = x;
		output[j] = y;
	}
	
	//MARK: Methods to add cardinality constraints
	
	/**
	 * Adds an at least k constraint (for the initial variables) to the formula.
	 * This will add a single unit clause and is equivalent to @see literalForAtLeast followed by @see Formula.addClause.
	 * @param k The bound.
	 */
	public void addAtLeast(int k) {
		int var = literalForAtLeast(k);
		if (var == 0) return;
		phi.addClause(var);
	}
	
	/**
	 * Adds an at most k constraint (for the initial variables) to the formula.
	 * This will add a single unit clause and is equivalent to @see literalForAtMost followed by @see Formula.addClause.
	 * @param k The bound.
	 */
	public void addAtMost(int k) {
		int var = literalForAtMost(k);
		if (var == 0) return;
		phi.addClause(var);
	}
	
	/**
	 * Returns an literal, which, when added to the formula, will force that at least k variables are set to true.
	 * This literal can also be used for assumptions.
	 * 
	 * This method assumes \(k \gt 0\) and \(k \leq n\), since at least 0 does would have no effect; and at least most then n has no meaning.
	 * In case of \(k \le 0\) or \(k \gt n\), this method will return 0;
	 * 
	 * @param k The bound.
	 * @return The literal.
	 */
	public int literalForAtLeast(int k) {
		if (k <= 0 || k > n) return 0;
		return output[n-k];
	}
	
	/**
	 * Returns an literal, which, when added to the formula, will force that at most k variables are set to true.
	 * This literal can also be used for assumptions.
	 * 
	 * This method assumes \(k \lt n\), since at most everything would not have an effect.
	 * For \(k \ge n\), this method returns 0;
	 * On the other hand, k is not allowed to be negative, since this makes no sense. In this case the method also returns 0.
	 * 
	 * @param k The bound.
	 * @return The literal.
	 */
	public int literalForAtMost(int k) {
		if (k < 0 || k >= n) return 0;
		return -output[n-k-1];
	}
}
