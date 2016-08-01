package de.uniluebeck.tcs.sat;

/**
 * Formula.java
 * @author bannach
 */

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * This class represents a formula of propositional logic in CNF.
 * It provides methods to build, modify, and concatenate formulas.
 */
public class Formula implements Iterable<List<Integer>> {

	/**
	 * The formula (in CNF) is represented as list of clauses, a clause 
	 * is represented as list of integers. Each variable is represented by a positive integer, a
	 * literal is then then represented by the signed corresponding variable.
	 */
	private final List<List<Integer>> data;
	
	/**
	 * A set of all variables that occurs in this formula.
	 */
	private final Set<Integer> variables;
	
	/**
	 * Auxiliary variables that are introduced to encode certain properties, as cardinality constraints, but which
	 * are actually not part of "the original problem". This set stores such variables, and this set will be disjoint from
	 * the set @see variables.
	 */
	private final Set<Integer> auxiliaryVariables;
	
	/**
	 * This integer is used to store the highest variable used in this formula.
	 * High here reference to the name of the variable, i.e., the integer representing it.
	 * Using this value, one can determine a range of "fresh" variables.
	 */
	private Integer highestVariable;
	
	/**
	 * Initialize the formula and the stored data structures.
	 */
	public Formula() {
		data = new ArrayList<>();
		variables = new HashSet<>();
		auxiliaryVariables = new HashSet<Integer>();
		highestVariable = 0;
	}
	
	/**
	 * Initialize the formula and the stored data structures.
	 * The value of the expected number of clauses to preallocate memory.
	 * @param expectedClauses
	 */
	public Formula(int expectedClauses) {
		data = new ArrayList<>(expectedClauses);
		variables = new HashSet<>();
		auxiliaryVariables = new HashSet<Integer>();
		highestVariable = 0;
	}
	
	/**
	 * Add a clause to the formula.
	 * @param C
	 */
	public void addClause(List<Integer> C) {
		for (Integer literal : C) {
			Integer var = Math.abs(literal);
			variables.add(var);
			if (var > highestVariable) highestVariable = var;
		}
		data.add(C);
	}

	/**
	 * Add a clause to the formula in form of (non zero) integers.
	 * @param vars
	 */
	public void addClause(Integer... vars) {
		List<Integer> C = new ArrayList<>(vars.length);
		for (Integer v : vars) C.add(v);
		addClause(C);
	}
	
	/**
	 * Set the status of the variable to "auxiliary".
	 * @param var
	 */
	public void markAuxiliary(Integer var) {
		if (variables.contains(var)) {
			variables.remove(var);
		}
		auxiliaryVariables.add(var);
	}
	
	/**
	 * Set the status of the variable to be a "normal variable".
	 * @param var
	 */
	public void unmarkAuxiliary(Integer var) {
		if (auxiliaryVariables.contains(var)) {
			auxiliaryVariables.remove(var);
		}
		variables.add(var);
	}
	
	/**
	 * Get a set with the variables used in this formula.
	 * @return
	 */
	public Set<Integer> getVariables() {
		return variables;
	}
	
	/**
	 * Get a set with the auxiliary variables of this formula.
	 * @return
	 */
	public Set<Integer> getAuxiliaryVariables() {
		return auxiliaryVariables;
	}
	
	/**
	 * Return a set of all variables used in this formula.
	 * This method actually computes the set from the set of variables and the set of
	 * auxiliary variables, i.e., the time is not constant.
	 * @return
	 */
	public Set<Integer> getAllVariables() {
		Set<Integer> S = new HashSet<Integer>();
		S.addAll(getVariables());
		S.addAll(getAuxiliaryVariables());
		return S;
	}
	
	/**
	 * Return the number of clauses stored in this formula.
	 * @return
	 */
	public int numberOfClauses() {
		return data.size();
	}
	
	/**
	 * Return the number of variables used in this formula.
	 * @return
	 */
	public int numberOfVariables() {
		return variables.size();
	}
	
	/**
	 * Return the number of auxiliary variables used in this formula
	 * @return
	 */
	public int numberOfAuxiliaryVariables() {
		return auxiliaryVariables.size();
	}
	
	/**
	 * Return the total number of variables used in this formula.
	 * @return
	 */
	public int numberOfAllVariables() {
		return numberOfVariables() + numberOfAuxiliaryVariables();
	}
	
	/**
	 * Get the "name" of the highest variable used in this formula.
	 * This value should be used to identify an range of "fresh" variables.
	 * @return
	 */
	public Integer getHighestVariable() {
		return highestVariable;
	}
	
	/**
	 * Set the (name of the) highest variable, this may be needed if an algorithms wil ladd new variables,
	 * but diddent add them to the formula yet. 
	 * @param value
	 */
	public void setHighestVariable(Integer value) {
		this.highestVariable = value;
	}
	
	/**
	 * Compute a logic and of this formula and another one.
	 * This is done by simply adding the clauses of the other formula to this one.
	 * @param psi
	 */
	public void and(Formula psi) {
		for (List<Integer> C : psi) {
			addClause(C);
		}
	}
	
	@Override
	public Iterator<List<Integer>> iterator() {
		return new ClauseIterator(this);
	}
	
	/**
	 * An iterator that iterates over the clauses of the formula, i.e.,
	 * over the lists that represent such clauses.
	 */
	class ClauseIterator implements Iterator<List<Integer>> {
		private final Iterator<List<Integer>> itr;
		
		ClauseIterator(Formula phi) {
			itr = phi.data.iterator();
		}
		
		@Override
		public boolean hasNext() {
			return itr.hasNext();
		}

		@Override
		public List<Integer> next() {
			return itr.next();
		}	
	}
	
	@Override
	public String toString() {
		// present the formula in DIMACS format
		StringBuilder sb = new StringBuilder();
		
		// insert a default header
		sb.append("c formula created by treewidth\n");
		sb.append("p cnf " + this.numberOfAllVariables() + " " + this.numberOfClauses() + "\n");
				
		// output the clauses
		for (List<Integer> C : this) {
			for (Integer l : C) {
				sb.append(l + " ");
			}
			sb.append("0\n");
		}
		
		return sb.toString();
	}
	
}
