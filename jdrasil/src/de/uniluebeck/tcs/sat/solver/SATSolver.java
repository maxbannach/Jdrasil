package de.uniluebeck.tcs.sat.solver;

/**
 * SATSolver.java
 * @author bannach
 */

import java.util.List;
import java.util.Map;

import de.uniluebeck.tcs.sat.Formula;

/**
 * This is an abstraction of an SAT solver, classes extending this
 * class should implement or use a specific SAT-solver to handle the methods of
 * this abstract class.
 */
public abstract class SATSolver implements java.io.Serializable {

	private static final long serialVersionUID = -1709627316648034823L;
	
	/**
	 * A timeout, after which the the @see solve() method should be canceled.
	 * If a implementation does not use the timeout, this value should be set to -1;
	 */
	final int timeout;
		
	/**
	 * Initialize the SAT-solver with an empty formula. 
	 */
	public SATSolver(int timeout) {
		this.timeout = timeout;
		this.init();
	}
	
	/**
	 * Initialize the SAT-solver and provide a given formula phi to it.
	 * @param phi
	 */
	public SATSolver(Formula phi, int timeout) {
		this.timeout = timeout;
		this.init();
		this.addFormula(phi);
	}
	
	/**
	 * This method should initialize the SAT-solver or engine. The init() method is called directly after
	 * the constructor is called.
	 */
	abstract void init();
	
	/**
	 * This method should initialize the SAT-solver or engine. The init() method is called directly after
	 * the constructor is called.
	 */
	public abstract void initSolver();
	
	/**
	 * This method resets the SAT-solver, i.e., clears the state, the known formula, and learned information.
	 */
	public abstract void reset();
	
	/**
	 * Add the formula to the SAT-solver. This method depends on the behavior of @see addClause(),
	 * as it will call this method for every clause in the given formula.
	 * @param phi
	 */
	public void addFormula(Formula phi) {
		for (List<Integer> C : phi) {
			addClause(C);
		}
	}
	
	/**
	 * Add a clause to the solver (i.e., to the formula hold by the solver)
	 * @param C
	 */
	abstract void addClause(List<Integer> C);
	
	/**
	 * Solve the satisfiability problem for the currently holden formula.
	 * This method returns true if, and only if, the solver found a satisfying assignment.
	 * If the solver reports that the formula has no solution, or if the solver fails due to a time out
	 * or an error, this method should return false.
	 * @return
	 */
	public abstract boolean solve();
	
	/**
	 * Compute a model, i.e., a satisfying assignment of the formula.
	 * This method should only be called after solve() was called and returned true.
	 * @return
	 */
	public abstract Map<Integer, Boolean> getModel();
	
	/**
	 * Checks if the SAT-solver uses a timeout.
	 * @return
	 */
	public boolean usesTimeout() {
		return timeout != -1;
	}
	
	/**
	 * Creates and returns a new instance of the SATSolver. The returned solver
	 * has especially no formula stored or learned informations.
	 * @return
	 */
	public abstract SATSolver newInstance();
	
}
