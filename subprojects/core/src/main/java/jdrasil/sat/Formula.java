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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jdrasil.sat.ISATSolver.SATSolverNotAvailableException;
import jdrasil.sat.encodings.BasicCardinalityEncoder;
import jdrasil.sat.encodings.DecreasingCardinalityEncoder;
import jdrasil.sat.encodings.IncrementalCardinalityEncoder;

/**
 * This class represents a formula of propositional logic in CNF.
 * It provides methods to build, modify, and concatenate formulas.
 * @author Max Bannach
 */
public class Formula implements Iterable<List<Integer>> {
		
	/**
	 * Checks if Jdrasil can register a SAT solver to the formula, that is, checks
	 * whether or not there is a SAT solver in Jdrasils class or library path.
	 * @return True if Jdrasil has found a SAT-Solver.
	 */
	public static boolean canRegisterSATSolver() {
		if (NativeSATSolver.isAvailable()) return true;
		if (SAT4JSolver.isAvailable()) return true;
		return false;
	}

	/**
	 * Get the expected signature of the SAT solver, that is, the name of the SAT solver we
	 * expect to register.
	 * @return expected signature
	 */
	public static String getExpectedSignature() {
		String signature = "No solver available";
		try {
			if (NativeSATSolver.isAvailable()) {
				ISATSolver tmp = new NativeSATSolver();
				signature = tmp.signature();
			} else if (SAT4JSolver.isAvailable()) {
				ISATSolver tmp = new SAT4JSolver();
				signature = tmp.signature();
			}
		} catch (Exception e) { /* we do not have to do anything here */ }
		return signature;
	}
	
	/**
	 * The formula (in CNF) is represented as list of clauses, a clause 
	 * is represented as list of integers. Each variable is represented by a positive integer, a
	 * literal is then then represented by the signed corresponding variable (DIMACS style).
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
	 * A mapping from variable sets to incremental cardinality encoder.
	 * If an cardinality constrained is added more then once, the exiting encoder will be used.
	 */
	private Map<Set<Integer>, IncrementalCardinalityEncoder> incrementalEncoder;
	
	/**
	 * A mapping from variable set to decreasing cardinality encoder.
	 * This works similar as @see IncrementalCardinalityEncoder, but does only support decreasing atMostK
	 * constraints.
	 */
	private Map<Set<Integer>, DecreasingCardinalityEncoder> decreasingEncoder;
	
	/**
	 * A model of the formula, i.e., a mapping from variables to its boolean values.
	 * A model will only be available if @see isSatisfiable() was invoked and has returned true.
	 */
	private Map<Integer, Boolean> model;
	
	/**
	 * This ISATSolver is used by methods as @see isSatisfiable().
	 * The solver must be registered by a call to @see registerSATSolver().
	 * Once a solver is registered, clauses of the formula will be send to the solver on the fly.
	 * This allows incremental solving, but forbids the deletion of clauses.
	 */
	private ISATSolver solver;
	
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
		auxiliaryVariables = new HashSet<>();
		this.incrementalEncoder = new HashMap<>();
		this.decreasingEncoder = new HashMap<>();
		highestVariable = 0;
		solver = null;
	}
	
	/**
	 * Initialize the formula and the stored data structures.
	 * The value of the expected number of clauses to preallocate memory.
	 * @param expectedClauses The expected number of clauses.
	 */
	public Formula(int expectedClauses) {
		data = new ArrayList<>(expectedClauses);
		variables = new HashSet<>();
		auxiliaryVariables = new HashSet<>();
		highestVariable = 0;
		solver = null;
	}
	
	/**
	 * Add the given Clause to the ISATSolver by calling add(literal) for each literal in C
	 * as well as calling add(0);
	 * A ISATSolver must be registered in order to use this method.
	 * 
	 * @param C The clause that will be added to the formula.
	 */
	private void transferClauseToSolver(List<Integer> C) {
		for (int literal : C) {
			solver.add(literal);
		}
		solver.add(0);
	}
	
	/**
	 * This method adds a variable the formula and updates the range of available variables.
	 * 
	 * The method is automatically called when a clause is added, so it usually does not has to be called
	 * manually. However, sometimes one wishes to "register" and variable before we use it. In this case
	 * this method is useful.
	 * 
	 * @param var The variable that will be added to the formula.
	 */
	public void addVariable(int var) {
		if (variables.contains(var) || auxiliaryVariables.contains(var)) return;
		variables.add(var);
		if (var > highestVariable) highestVariable = var;
	}
	
	/**
	 * Works as @see addVariable, but finds a safe variable number by it self and returns the new variable.
	 * @return A freshly added variable.
	 */
	public int newVariable() {
		int newVar = this.highestVariable+1;
		addVariable(newVar);
		return newVar;
	}
	
	/**
	 * Works as @see addVariable, but finds a safe variable number by it self and returns the new variable.
	 * Marks the variable as auxiliary was well.
	 * @return A freshly added variable.
	 */
	public int newAuxillaryVariable() {
		int newVar = this.highestVariable + 1;
		addVariable(newVar);
		markAuxiliary(newVar);
		return newVar;
	}
	
	/**
	 * Add a clause to the formula.
	 * If a SATSolver is registered, this will also transfer the clause directly to the solver.
	 * @param C The clause that will be added to the Formula.
	 */
	public void addClause(List<Integer> C) {
		for (Integer literal : C) {
			Integer var = Math.abs(literal);
			addVariable(var);
		}
		data.add(C);
		if (solver != null) transferClauseToSolver(C);
	}

	/**
	 * Add a clause to the formula in form of a set of (non zero) integers.
	 * @param vars Variables that form the clause.
	 */
	public void addClause(Set<Integer> vars) {
		List<Integer> C = new ArrayList<>(vars.size());
		for (Integer v : vars) C.add(v);
		addClause(C);
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
	 * Removes the given clause from the formula. If this will remove all occurrences of a variable in the clause,
	 * the variable will be removed from the formula as well.
	 * 
	 * This method may increase the solution space of the formula and can only be called if no SAT solver is registered
	 * already. If there is SAT solver registered an exception will be thrown.
	 * 
	 * @param C
	 * @throws SATSolverRegisteredException
	 */
	public void removeClause(List<Integer> C) throws SATSolverRegisteredException {
		
		// this only supported if we have not sat solver
		if (solver != null) throw new SATSolverRegisteredException();
		
		// first remove the clause
		for (List<Integer> clause : data) {
			if (C.equals(clause)) {
				data.remove(clause);				
				break;
			}
		}
		
		// remove the variable, if it is not present anymore
		for (Integer literal : C) {
			boolean available = false;
			for (List<Integer> clause : data) {
				if (clause.contains(literal) || clause.contains(-1*literal)) {
					available = true;
					break;
				}
			}
			if (!available) {
				this.variables.remove(Math.abs(literal));
				this.auxiliaryVariables.remove(Math.abs(literal));
			}
		}		
		
	}
	
	/**
	 * Abbreviation for @see removeClause(List C)
	 * @param vars The clause to be removed.
	 * @throws SATSolverRegisteredException If there is an error with the solver.
	 */
	public void removeClause(Integer... vars) throws SATSolverRegisteredException {
		List<Integer> C = new ArrayList<>(vars.length);
		for (Integer v : vars) C.add(v);
		removeClause(C);
	}
	
	/**
	 * Set the status of the variable to "auxiliary".
	 * @param var The variable to be marked as auxiliary.
	 */
	public void markAuxiliary(Integer var) {
        variables.remove(var);
		auxiliaryVariables.add(var);
	}
	
	/**
	 * Set the status of the variable to be a "normal variable".
	 * @param var The variable to be unmarked.
	 */
	public void unmarkAuxiliary(Integer var) {
        auxiliaryVariables.remove(var);
		variables.add(var);
	}
	
	/**
	 * Get a set with the variables used in this formula.
	 * @return The variables in the formula
	 */
	public Set<Integer> getVariables() {
		return variables;
	}
	
	/**
	 * Get a set with the auxiliary variables of this formula.
	 * @return The auxiliary variables in the formula
	 */
	public Set<Integer> getAuxiliaryVariables() {
		return auxiliaryVariables;
	}
	
	/**
	 * Return a set of all variables used in this formula.
	 * This method actually computes the set from the set of variables and the set of
	 * auxiliary variables, i.e., the time is not constant.
	 * @return The set of all variables.
	 */
	public Set<Integer> getAllVariables() {
		Set<Integer> S = new HashSet<>();
		S.addAll(getVariables());
		S.addAll(getAuxiliaryVariables());
		return S;
	}
	
	/**
	 * Return the number of clauses stored in this formula.
	 * @return The number of clauses.
	 */
	public int numberOfClauses() {
		return data.size();
	}
	
	/**
	 * Return the number of variables used in this formula.
	 * @return The number of variables.
	 */
	public int numberOfVariables() {
		return variables.size();
	}
	
	/**
	 * Return the number of auxiliary variables used in this formula
	 * @return The number of auxiliary variables.
	 */
	public int numberOfAuxiliaryVariables() {
		return auxiliaryVariables.size();
	}
	
	/**
	 * Return the total number of variables used in this formula.
	 * @return Number of variables + number of uxiliary variables.
	 */
	public int numberOfAllVariables() {
		return numberOfVariables() + numberOfAuxiliaryVariables();
	}
	
	/**
	 * Get the "name" of the highest variable used in this formula.
	 * This value should be used to identify an range of "fresh" variables.
	 * @return The name of the highest variable used.
	 */
	public Integer getHighestVariable() {
		return highestVariable;
	}
	
	/**
	 * Set the (name of the) highest variable, this may be needed if an algorithms will add new variables,
	 * but did not add them to the formula yet. 
	 * @param value Set name of highest variable.
	 */
	public void setHighestVariable(Integer value) {
		this.highestVariable = value;
	}
	
	/**
	 * Compute a logic and of this formula and another one.
	 * This is done by simply adding the clauses of the other formula to this one.
	 * @param psi The formula that is combined to this formula.
	 */
	public void and(Formula psi) {
		for (List<Integer> C : psi) {
			addClause(C);
		}
	}
	
	/**
	 * Adds an at-most-k constraint to the formula with respect to the given set of variables,
	 * i.e., only models that set not more the k of the given variables of true can satisfy the formula.
	 * @param k The bound.
	 * @param variables The variables for which the constraint shall hold.
	 */
	public void addAtMost(int k, Set<Integer> variables) {
		
		// small instances are better handled by binomial
		if (variables.size() < 7 || k <= 1) {			
			BasicCardinalityEncoder.binomialAMK(this, variables, k);
			return;
		}
		
		// if we are allowed to set all variables, do nothing
		if (k == variables.size()) return;
		
		// if k is large, the inverted encoding is faster
		if (k > variables.size()/2) {
			Set<Integer> neg = new HashSet<>();
			for (Integer v : variables) neg.add(-1*v); 
			BasicCardinalityEncoder.sequentialALK(this, neg, variables.size()-k);
			return;
		}
		
		// use sequential counter
		BasicCardinalityEncoder.sequentialAMK(this, variables, k);
	}
	
	/**
	 * Adds an at-least-k constraint to the formula with respect to the given set of variables,
	 * i.e., only models that set k ore more of the given variables of true can satisfy the formula.
     * @param k The bound.
     * @param variables The variables for which the constraint shall hold.
	 */
	public void addAtLeast(int k, Set<Integer> variables) {
		
		// nothing to do
		if (k == 0) return;
		
		// at least one is a simple clause
		if (k == 1) {
			this.addClause(new LinkedList<>(variables));
			return;
		}
		
		// we have to set all variables, simply add unit clauses
		if (k == variables.size()) {
			for (Integer v : variables) this.addClause(v);
			return;
		}
		
		// if k is large, the inverted encoding is faster
		if (k > variables.size()/2) {
			Set<Integer> neg = new HashSet<>();
			for (Integer v : variables) neg.add(-1*v); 
			BasicCardinalityEncoder.sequentialAMK(this, neg, variables.size()-k);
			return;
		}
		
		// use sequential counter
		BasicCardinalityEncoder.sequentialALK(this, variables, k);	
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
	 * This methods adds O(n*log^2 n) variables to the formula, so it is also worth for big k.
	 *  
	 * @param lb The lowerbound.
	 * @param ub The upperbound
	 * @param variables The variables for which the constraint shall hold.
	 */
	public void addCardinalityConstraint(int lb, int ub, Set<Integer> variables) {
		if (!incrementalEncoder.containsKey(variables)) {
			incrementalEncoder.put(variables, new IncrementalCardinalityEncoder(this, variables));
		}
		incrementalEncoder.get(variables).addAtLeast(lb);
		incrementalEncoder.get(variables).addAtMost(ub);
	}
	
	/**
	 * At an atMostK constraint that can be decreased between calls to the SAT solver.
	 * This method works similar as @see addCardinalityConstraint(), but does only support atMostK-constraints.
	 * 
	 * It adds O(k*n) variables to the formula, so it is only valuable for small k.
	 *
     * @param k The bound.
     * @param variables The variables for which the constraint shall hold.
	 */
	public void addDecreasingAtMost(int k, Set<Integer> variables) {
		if (!decreasingEncoder.containsKey(variables)) {
			decreasingEncoder.put(variables, new DecreasingCardinalityEncoder(this, k, variables));			 
		} else {
			decreasingEncoder.get(variables).addAtMost(k);
		}
	}
	
	/**
	 * Works as @see addCardinalityConstraint, but does add the lb and ub only as assumption (auxillary variables and clauses are added normaly,
	 * so that this method can be used incrementally as well).
	 * 
	 * As this method does make an assumption, it only works if a SAT solver is registered (other wise a @see NoSATSolverRegisteredException will be thrown).
	 *
     * @param lb The lowerbound.
     * @param ub The upperbound
     * @param variables The variables for which the constraint shall hold.
	 * @throws NoSATSolverRegisteredException If something went wrong with the solver.
	 */
	public void assumeCardinalityConstraint(int lb, int ub, Set<Integer> variables) throws NoSATSolverRegisteredException {
		if (this.solver == null) throw new NoSATSolverRegisteredException();
		if (!incrementalEncoder.containsKey(variables)) {
			incrementalEncoder.put(variables, new IncrementalCardinalityEncoder(this, variables));
		}
		int lbV = incrementalEncoder.get(variables).literalForAtLeast(lb);
		int ubV = incrementalEncoder.get(variables).literalForAtMost(lb);
		if (lbV != 0) this.solver.assume(lbV);
		if (ubV != 0) this.solver.assume(ubV);
	}
	
	/**
	 * Register a SAT solver to the formula. Registering a solver means the following:
	 * 
	 * 1) the formula will be send to the solver
	 * 2) any newly added clause will be send to the solver (i.e., they will be in sync)
	 * 3) deletion or modification of clauses is not possible until the solver was unregistered
	 * 4) @see isSatisfiable() can be used to check (using the registered solver) if the formula is satisfiable
	 * 5) the last point can be used incrementally
	 * 
	 * The user has no influence on which kind of solver will be registered. 
	 * 
	 * You can check whether or not this method will throw an exception with @see canRegisterSATSolver()
	 * 
	 * @return String the signature of the loaded solver.
	 * @throws SATSolverNotAvailableException if Jdrasil has no access to any SATSolver.
	 */
	public String registerSATSolver() throws ISATSolver.SATSolverNotAvailableException {
		
		// try to load a solver	
		if (NativeSATSolver.isAvailable()) {
			this.solver = new NativeSATSolver();
		} else if (SAT4JSolver.isAvailable()) {
			this.solver = new SAT4JSolver();
		} else {
			throw new ISATSolver.SATSolverNotAvailableException();
		}
		
		// transfer all previous clauses to the solver
		for (List<Integer> clause : data) {
			transferClauseToSolver(clause);
		}
		
		// return the signature of the solver
		return solver.signature();
	}
	
	/**
	 * Unregisters the SAT solver, if set. This means:
	 * 
	 *  1) The Formula and the solver will not be in sync anymore
	 *  2) Clauses can be modified again
	 *  3) @see isSatisfiable() will throw an Exception until a new solver is registerd.
	 *  
	 */
	public void unregisterSATSolver() {
		if (this.solver != null) this.solver.release();
		this.solver = null;
	}
	
	/**
	 * Use a SAT solver to check if there is a satisfying model for the formula.
	 * If this method returns true, i.e., if the formula is satisfiable, a satisfying model will be stored, 
	 * i.e., @see getModel() can be called afterwards.
	 * 
	 * @return true if the formula is satisfiable 
	 * @throws NoSATSolverRegisteredException if no SAT solver was registered for this formula
	 */
	public boolean isSatisfiable(Integer... assumption) throws NoSATSolverRegisteredException {
		if (this.solver == null) throw new NoSATSolverRegisteredException();
		
		// add assumption to the solver
		for (int i = 0; i < assumption.length; i++) {
			solver.assume(assumption[i]);
		}
		
		// solve the formula
		if (solver.solve() != ISATSolver.SATISFIABLE) return false;
		
		// extract the model from the solver
		model = new HashMap<>();
		for (Integer var : getVariables()) {
			model.put(var, solver.val(var) == var);
		}
		
		return true;
	}
	
	/**
	 * If the formula is satisfiable, this method can be used to obtain a model.
	 * A model will only be available if @see isSatisfiable() was called and has returned true.
	 * The model will correspond to the last (successful) call of @see isSatisfiable().
	 * @return The model of the formula.
	 * @throws NoModelAvailableException If the formula was not solved yet.
	 */
	public Map<Integer, Boolean> getModel() throws NoModelAvailableException {
		if (this.model == null) {
			throw new NoModelAvailableException();
		}
		return this.model;
	}
		
	@Override
	public Iterator<List<Integer>> iterator() {
		return new ClauseIterator(this);
	}
	
	/**
	 * An iterator that iterates over the clauses of the formula, i.e.,
	 * over the lists that represent such clauses.
     * @return An iterator over the clauses.
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
		sb.append("c formula created by Jdrasil\n");
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
	 * @see Formula#registerSATSolver()
	 */
	public class NoSATSolverRegisteredException extends Exception {
		private static final long serialVersionUID = 1L;		
	}
	
	/**
	 * This exception that there is a SATSolver registered for this Formula, and it was tried to
	 * modify the formula in an unsupported way, i.e., deleting or modifying a clause.
	 * @see Formula#registerSATSolver()
	 */
	public class SATSolverRegisteredException extends Exception {
		private static final long serialVersionUID = 1L;		
	}
	
	/**
	 * This exception is used when one tries to get a model of the formula, but there is non.
	 * For instance, because @see isSatisfiable() was not called, or because there is no model at all.
	 */
	public class NoModelAvailableException extends Exception {
		private static final long serialVersionUID = 1L;		
	}
	
}
