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



/**
 * The interface to a SAT solver used by Jdrasil. 
 * It is based on the IPASIR interface used by the incremental SAT challenge in 2015 and 2016.
 * Using this interface, Jdrasil is able to use any C/C++ SAT solver that uses the IPASIR interface.
 * 
 * @author Max Bannach
 */
public interface ISATSolver {

	/** This value is returned by @see solve if the formula is satisfiable */
	int SATISFIABLE   = 10;
	
	/** This value is returned by @see solve if the formula is unsatisfiable */	
	int UNSATISFIABLE = 20;
	
	/** This value is returned by @see solve if the solving process was canceld */
	int UNKNOWN       = 0;
		
	/**
	 * The solver can be in three different states (after the IPASIR interface).
	 * The sate can be null, i.e., undefined.
	 */
	enum State {		
		INPUT, // the formula was modified since the last satisfiable check and the situation is unclear.
		SAT,   // the solver knows that the current formula is satisfiable
		UNSAT  // the solver knows that the current formula is not satisfiable
	}
		
	/**
	 * Set the state of the sat solver with respect to the IPASIR interface.
	 * When ever the state of the solver changes, an implementation of this interface should call this method.
	 * @param state The state in which the sat solver is.
	 */
	void setCurrentState(State state);

	/**
	 * Gets the current state of the solver.
	 * @see ISATSolver#setCurrentState(jdrasil.sat.ISATSolver.State) and @see State
	 * @return The current state of the solver.
	 */
	State getCurrentState();

	/**
	 * Gets a string representing the solver / the used library, i.e., its name.
	 * @return Return string representation of the solver.
	 */
	String signature();
	
	/**
	 * Initialize the SAT solver. 
	 * If this is called for a native solver, @see pointer can be set. The calling object in following
	 * calls will provide this pointer.
	 * 
	 * State: undefined to INPUT
	 */
	void init();
	
	/**
	 * Releases everything the SAT solver may have allocated.
	 * If called for a native solver, use the pointer stored in the callingObject to identify the solver.
	 * 
	 * For Solver written in Java this may not be needed, but when the interface is implemented on native side
	 * this method should be called (especially by an deconstructor on Java side).
	 * 
	 * State: {INPUT, SAT, UNSAT} to undefined
	 */
	void release();
	
	/**
	 * Adds an literal to the currently constructed clause (by definition, there is always a clause at construction).
	 * The literal is represented in the DIMACS format (a non-zero integer where \(x \gt 0\) is a positive literal and -x its negation).
	 * To close a clause and to add it to the solver, call this method with 0.
	 * 
	 * If called for a native solver, use the pointer stored in the callingObject to identify the solver.
	 * 
	 * @param literal
	 * 
	 * State: {INPUT, SAT, UNSAT} to INPUT
	 */
	void add(int literal);
	
	/**
	 * Assumes that the given literal (DIMACS format as in @see add(int literal)) is true, that is, adding a unit clause containg
	 * the literal to the formula. This clause (and hence, the assumption) will be removed after the next call of @see solve().
	 * 
	 * If called for a native solver, use the pointer stored in the callingObject to identify the solver.
	 * 
	 * @param literal
	 * 
	 * State: {INPUT, SAT, UNSAT} to INPUT
	 */
	void assume(int literal);
	
	/**
	 * Checks if there is a satisfying assignment for the formula.
	 * If there is a model, this method returns 10.
	 * If there is no model, this method returns 20.
	 * If the result is not known, for instance the solving process was interrupted, this method returns 0.
	 * 
	 * If called for a native solver, use the pointer stored in the callingObject to identify the solver.
	 * 
	 * @return
	 * 
	 * State: {INPUT, SAT, UNSAT} to {INPUT, SAT, UNSAT}
	 */
	int solve();
	
	/**
	 * Get the truth value of the given literal. If x is the requested literal, the method will return -x
	 * if the literal is set to false, x if it is set to true, and 0 if the literal is not important.
	 * 
	 * This method can only be called if the solver is in the state SAT, i.e., after a call of @see solve() returned 10.
	 * 
	 * @return
	 * 
	 * State: SAT to SAT
	 */
	int val(int literal);
	
	/**
	 * Checks if the given literal was required to proof the unsatisfiability of the formula.
	 * This method makes only sense (and hence, can only be called) if the last call of @see solve() returend 20, i.e.,
	 * if the formula is unsatisfiable, and if no new assumption was made since then, i.e., the solver is in the sate UNSAT.
	 * 
	 * If called for a native solver, use the pointer stored in the callingObject to identify the solver.
	 * 
	 * @param literal The literal to be checked.
	 * @return True if the literal was required to proof unsatisfiability.
	 * 
	 * State: UNSAT to UNSAT
	 */
	boolean failed(int literal);
	
	/**
	 * Terminate a run of @see solve() of the sat solver. The @see solve() method will then return 0.
	 * 
	 * State: {INPUT, SAT, UNSAT} to {INPUT, SAT, UNSAT}
	 */
	void terminate();
	
	/**
	 * This exception is thrown if a SAT solver is not available, i.e., not loaded by Jdrasil.
	 */
	class SATSolverNotAvailableException extends Exception {
		private static final long serialVersionUID = 1L;		
	}
}
