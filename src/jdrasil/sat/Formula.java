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
package jdrasil.sat;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This class represents a formula of propositional logic in CNF.
 * It provides methods to build, modify, and concatenate formulas.
 * @author Max Bannach
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
	 * This SATSolver is used by methods as @see isSatisfiable().
	 * The solver must be registered by a call to @see registerSATSolver().
	 * Once a solver is registered, clauses of the formula will be send to the solver on the fly.
	 * This allows incremental solving, but forbids the deletion of clauses.
	 */
	private SATSolver solver;
	
	/**
	 * A model of the formula assigning to every variable a boolean value.
	 * A model is only available if the formula is 
	 * a) satisfiable
	 * b) @see isSatisfiable() was called before
	 * 
	 * Note that this value will always store the _last available_ model, i.e., if @see isSatisfiable() was
	 * called while the formula had a model, and the formula was modified after this call, the model will correspond
	 * to the formula at the moment of the call to @see isSatisfiable()
	 */
	private Map<Integer, Boolean> model;
	
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
		solver = null;
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
		solver = null;
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
	 * Set the (name of the) highest variable, this may be needed if an algorithms will add new variables,
	 * but did not add them to the formula yet. 
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
	
	/**
	 * Adds an at-most-k constraint to the formula with respect to the given set of variables,
	 * i.e., only models that set not more the k of the given variables of true can satisfy the formula.
	 * @param k
	 * @param variables
	 */
	public void addAtMost(int k, Set<Integer> variables) {
		//TODO: implement
	}
	
	/**
	 * Adds an at-least-k constraint to the formula with respect to the given set of variables,
	 * i.e., only models that set k ore more of the given variables of true can satisfy the formula.
	 * @param k
	 * @param variables
	 */
	public void addAtLeast(int k, Set<Integer> variables) {
		//TODO: implement
	}
	
	/**
	 * Adds an cardinality constraint to the formula with respect to the given set of variables,
	 * i.e., only models that set a least lb and at most ub of the variables to true can satisfy the formula.
	 * 
	 * This function uses an encoding based on sorting networks and is well suited for iterative calls, i.e.,
	 * a second call that increases lb or decreases ub (or both) will add much less new clauses.
	 * 
	 * In contrast @see addAtMost() and @see addAtLeast() are better for single calls.
	 *  
	 * @param lb
	 * @param ub
	 * @param variables
	 */
	public void addCardinalityConstraint(int lb, int ub, Set<Integer> variables) {
		//TODO: implement
	}
	
	/**
	 * Register a SAT-solver to the formula. Registering a solver means the following:
	 * 
	 * 1) the formula will be send to the solver
	 * 2) any newly added clause will be send to the solver (i.e., they will be in sync)
	 * 3) deletion or modification of clauses is not possible until the solver was unregistered
	 * 4) @see isSatisfiable() can be used to check (using the registered solver) if the formula is satisfiable
	 * 5) the last point can be used incrementally
	 * 
	 * The user has no influence on which kind of solver will be registered. 
	 * 
	 * @throws SATSolverNotAvailableException if Jdrasil has no access to any SATSolver
	 */
	public void registerSATSolver() throws SATSolver.SATSolverNotAvailableException {
		if (!SATSolver.isAvailable()) throw new SATSolver.SATSolverNotAvailableException();
		this.solver = new SATSolver();
	}
	
	/**
	 * Unregisters the SAT-Solver, if set. This means:
	 * 
	 *  1) The Formula and the solver will not be in sync anymore
	 *  2) Clauses can be modified again
	 *  3) @see isSatisfiable() will throw an Exception until a new solver is registerd.
	 *  
	 */
	public void unregisterSATSolver() {
		this.solver = null;
	}
	
	/**
	 * Use a SAT-Solver to check if there is a satisfying model for the formula.
	 * If this method returns true, i.e., if the formula is satisfiable, a satisfying model will be stored, 
	 * i.e., @see getModel() can be called afterwards.
	 * 
	 * @return true if the formula is satisfiable 
	 * @throws NoSATSolverRegisteredException if no SAT-Solver was registered for this formula
	 */
	public boolean isSatisfiable() throws NoSATSolverRegisteredException {
		if (this.solver == null) throw new NoSATSolverRegisteredException();
		//TODO: implement

		return false;
	}
	
	/**
	 * If the formula is satisfiable, this method can be used to obtain a model.
	 * A model will only be available if @see isSatisfiable() was called and has returned true.
	 * The model will correspond to the last call of @see isSatisfiable(), even if the formula was modifed after this call.
	 * @return
	 * @throws NoModelAvailableException
	 */
	public Map<Integer, Boolean> getModel() throws NoModelAvailableException {
		if (model == null) throw new NoModelAvailableException();
		return model;
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

	/**
	 * This exception indicates that there is no SATSolver registered for this Formula.
	 * @see registerSATSolver()
	 */
	class NoSATSolverRegisteredException extends Exception {
		private static final long serialVersionUID = 1L;		
	}
	
	/**
	 * This exception is used when one tries to get a model of the formula, but there is non.
	 * For instance, because @see isSatisfiable() was not called, or because there is no model at all.
	 */
	class NoModelAvailableException extends Exception {
		private static final long serialVersionUID = 1L;		
	}
	
}
