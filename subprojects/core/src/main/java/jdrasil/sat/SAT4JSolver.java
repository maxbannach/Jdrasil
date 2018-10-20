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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;



/**
 * This class implements the IPASIR interface defined in ISATSolver for SAT4J.
 * Since Jdrasil does not get shipped with SAT4J (and may not need it, for instance if no SAT solver or a native solver is used),
 * this class is complete build on reflections. Hence, it can be compiled and used even if SAT4J is not available in the current classpath
 * of Jdrasil. 
 * 
 * Due to the heavy usage of reflection, this class has to do a lot of exception handling. This is released as follows: On any exception at
 * point, the current state of the solver will be set to be undefined (null). This will lead to the situation that no method will be invoked,
 * unless init().
 * 
 * @author Max Bannach
 */
class SAT4JSolver implements ISATSolver {

	 /**
	  * Checks whether or not the SAT4J library is available.
	  * @return True if SAT4J is loaded.
	  */
	 protected static boolean isAvailable() {
		 try {
			 Class.forName("org.sat4j.minisat.SolverFactory");
			 return true;
		 } catch( ClassNotFoundException e ) {
			 return false;
		 }
	 }
	
	/** Just the name of the solver. */
	private final String SIGNATURE = "SAT4J";
	
	//MARK: SAT4J Classes
	
	/** Reflection for org.sat4j.specs.Solver */
	private Class<?> Solver;
	
	/** Reflection for org.sat4j.specs.IVecInt */
	private Class<?> IVecInt;
	
	/** Reflection for org.sat4j.core.VecInt */
	private Class<?> VecInt;
	
	/** Reflection for org.sat4j.minisat.SolverFactory */
	private Class<?> SolverFactory;
	
	//MARK: SAT4J Methods
	
	/** Reflection for org.sat4j.minisat.SolverFactory.newDefault() */
	private Method newDefault;
	
	/** Reflection for org.sat4j.specs.IVecInt.push(int) */
	private Method push;
	
	/** Reflection for org.sat4j.specs.IVecInt.contains(int) */
	private Method contains;
	
	/** Reflection for org.sat4j.specs.Solver.addClause(IVecInt) */
	private Method addClause;
	
	/** Reflection for org.sat4j.specs.Solver.isSatisfiable(IVecInt) */
	private Method isSatisfiable;
	
	/** Reflection for org.sat4j.specs.Solver.model(int) */
	private Method model;
	
	/** Reflection for org.sat4j.specs.Solver.unsatExplanation */
	private Method unsatExplanation;
	
	/** Reflection for org.sat4j.specs.Solver.stop */
	private Method stop;
	
	//MARK: class variables
	
	/**
	 * The state the solver is currently in.
	 */
	private State currentState;
	
	/**
	 * The actually SAT solver from SAT4J.
	 */
	private Object solver;
	
	/** 
	 * As defined by IPASIR, there is always a currently constructed clause. 
	 * This clause is added to the solver when the literal 0 is added, this object will then be rested.
	 */
	private Object currentClause;
	
	/**
	 * Each assumption will be added to this IVecInt, the assumption will be passed to the solver when
	 * @see SAT4JSolver#solve() is called. After this, this object will be rested.
	 */
	private Object assumption;
	
	/**
	 * SAT4J may notice that the formula is trivially not satisfiable while adding a clause.
	 * Tn this case, we do not have to actually solve the formula. 
	 */
	private boolean triviallyUnsatisfiable = false;
	
	/**
	 * The default constructor will just invoke @see init()
	 * @throws SATSolverNotAvailableException If SAT4J is not available.
	 */
	public SAT4JSolver() throws SATSolverNotAvailableException {
		if (!SAT4JSolver.isAvailable()) throw new ISATSolver.SATSolverNotAvailableException();
		init();
	}
	
	//Mark: Override ISATSolver
	
	/*
	 * (non-Javadoc)
	 * @see jdrasil.sat.ISATSolver#setCurrentState(jdrasil.sat.ISATSolver.State)
	 */
	@Override
	public void setCurrentState(State state) {
		this.currentState = state;
	}
	
	/*
	 * (non-Javadoc)
	 * @see jdrasil.sat.ISATSolver#getCurrentState()
	 */
	@Override
	public State getCurrentState() {
		return this.currentState;
	}
	
	/* (non-Javadoc)
	 * @see jdrasil.sat.ISATSolver#signature()
	 */
	@Override
	public String signature() {
		return SIGNATURE;
	}

	/* (non-Javadoc)
	 * @see jdrasil.sat.ISATSolver#init()
	 */
	@Override
	public void init() {
		this.currentState = State.INPUT;
		
		// load SAT4J classes by reflection
		try {
			Solver       = Class.forName("org.sat4j.minisat.core.Solver");
			IVecInt       = Class.forName("org.sat4j.specs.IVecInt");
			VecInt        = Class.forName("org.sat4j.core.VecInt");
			SolverFactory = Class.forName("org.sat4j.minisat.SolverFactory");
		} catch (Exception e) { 
			this.currentState = null;
		}
		
		// load methods of SAT4J classes by reflection
		try {
			newDefault       = SolverFactory.getDeclaredMethod ("newDefault");
			push             = IVecInt.getDeclaredMethod("push", int.class);
			contains         = IVecInt.getDeclaredMethod("contains", int.class);
			addClause        = Solver.getDeclaredMethod("addClause", IVecInt);
			isSatisfiable    = Solver.getMethod("isSatisfiable", IVecInt);
			model            = Solver.getMethod("model", int.class);
			unsatExplanation = Solver.getMethod("unsatExplanation");
			stop = Solver.getMethod("stop");
		} catch (Exception e) {
			this.currentState = null;
		}

		// initialize class objects
		try {
			this.currentClause = VecInt.newInstance();
			this.assumption    = VecInt.newInstance();
			this.solver        = newDefault.invoke(null);
		} catch (Exception e) {
			this.currentState = null;
		}
		
	}

	/* (non-Javadoc)
	 * @see jdrasil.sat.ISATSolver#release()
	 */
	@Override
	public void release() {
		// not needed
	}

	/* (non-Javadoc)
	 * @see jdrasil.sat.ISATSolver#add(int)
	 */
	@Override
	public void add(int literal) {
		if (this.currentState == null) return;
		this.currentState = State.INPUT;
		

		try {
			if (literal == 0) {
				addClause.invoke(this.solver, this.currentClause);
				this.currentClause = VecInt.newInstance();
			} else {
				push.invoke(this.currentClause, literal);
			}
		} catch (InvocationTargetException e) {
			this.triviallyUnsatisfiable = true;
		} catch (Exception e) {
			this.currentState = null;
		} 
		
	}

	/* (non-Javadoc)
	 * @see jdrasil.sat.ISATSolver#assume(int)
	 */
	@Override
	public void assume(int literal) {
		if (this.currentState == null) return;
		this.currentState = State.INPUT;
		
		try {
			push.invoke(this.assumption, literal);
		} catch (Exception e) {
			this.currentState = null;
		} 
	}

	/* (non-Javadoc)
	 * @see jdrasil.sat.ISATSolver#solve()
	 */
	@Override
	public int solve() {
		if (this.triviallyUnsatisfiable) return UNSATISFIABLE;
		if (this.currentState == null) return UNKNOWN;
		
		// try to solve the formula
		try {
			if ((boolean) isSatisfiable.invoke(solver, this.assumption)) {
				this.currentState = State.SAT;
			} else {
				this.currentState = State.UNSAT;
			}
		} catch (Exception e) {
			this.currentState = State.INPUT;
		}
		
		// remove assumptions
		try {
			this.assumption  = VecInt.newInstance();
		} catch (Exception e) {
			this.currentState = null;
		} 
		
		// check result
		if (this.currentState == State.SAT) return SATISFIABLE;
		if (this.currentState == State.UNSAT) return UNSATISFIABLE;
		return UNKNOWN;
		
	}

	/* (non-Javadoc)
	 * @see jdrasil.sat.ISATSolver#val(int)
	 */
	@Override
	public int val(int literal) {
		if (this.currentState != State.SAT) return UNKNOWN;
		
		Boolean value = null;
		try {
			value = (boolean) model.invoke(this.solver, Math.abs(literal));
		} catch (Exception e) {
			this.currentState = null;
		}
		
		if (value != null) return (value ? literal : -1*literal);
		return UNKNOWN;
	}

	/* (non-Javadoc)
	 * @see jdrasil.sat.ISATSolver#failed(int)
	 */
	@Override
	public boolean failed(int literal) {
		if (this.currentState != State.UNSAT) return false;
		
		try {
			Object unsat = unsatExplanation.invoke(this.solver);
			if (unsat == null) return false;
			return (boolean) contains.invoke(unsat, literal);
		} catch (Exception e) {
				this.currentState = null;
			
		}
		
		return false;
	}

	/* (non-Javadoc)
	 * @see jdrasil.sat.ISATSolver#terminate()
	 */
	@Override
	public void terminate() {
		try {
			stop.invoke(this.solver);
		} catch (Exception e) {
			this.currentState = null;
		}
	}
	
}
