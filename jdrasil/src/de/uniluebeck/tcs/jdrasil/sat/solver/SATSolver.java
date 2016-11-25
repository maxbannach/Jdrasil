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
package de.uniluebeck.tcs.jdrasil.sat.solver;

import java.util.List;
import java.util.Map;

import de.uniluebeck.tcs.jdrasil.sat.Formula;

/**
 * This is an abstraction of an SAT solver, classes extending this
 * class should implement or use a specific SAT-solver to handle the methods of
 * this abstract class.
 * 
 * @author Max Bannach
 */
public abstract class SATSolver implements java.io.Serializable {

	private static final long serialVersionUID = -1709627316648034823L;
		
	/**
	 * Initialize the SAT-solver with an empty formula. 
	 */
	public SATSolver() {
		this.init();
	}
	
	/**
	 * Initialize the SAT-solver and provide a given formula phi to it.
	 * @param phi
	 */
	public SATSolver(Formula phi) {
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
	 * Creates and returns a new instance of the SATSolver. The returned solver
	 * has especially no formula stored or learned informations.
	 * @return
	 */
	public abstract SATSolver newInstance();
	
}
