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
 * This class builds the interface to an native SAT solver that implements the IPASIR SAT solver interface.
 * Instances of this class can only be generated if a corresponding native library was found in the class path of Jdrasil.
 * 
 * @author Max Bannach
 */
class NativeSATSolver implements ISATSolver {
	
	/*
	 * The static constructor of the NativeSATSolver tries to load the native library (i.e., if it is available
	 * in JDrasils library.path). If this is the case, the library is loaded and @see isAvailable() will return true, otherwise
	 * the library is not loaded and @see isAvailable() will return false.
	 */
	 static {
		 boolean success = false;
		 try {
			 System.loadLibrary("jdrasil_sat_NativeSATSolver");
			 success = true;
		 } catch (UnsatisfiedLinkError e) {
			 // success = false;
		 }		
		 libraryLoaded = success;
	 }
	
	 /** This value is true if, and only if, the static constructor successfully loaded the native library. */
	 private final static boolean libraryLoaded;
	 
	 /**
	  * Checks whether or not a native SAT solver is available.
	  * @return true if the static constructor managed to load a native sat solver.
	  */
	 protected static boolean isAvailable() {
		 return libraryLoaded;
	 }
	 
	 /**
	  * A pointer to the C/C++ reference "object" of this solver.
	  */
	 private long pointer;
	 	 
	 /**
	  * The state the solver is currently in.
	  */
	 private State currentState;
	 
	 /**
	  * Creates a new native SAT solver. This constructor only works if a native solver is available in the library.path of Jdrasil,
	  * otherwise this method will generate an exception.
	  * You can check whether or not a native solver is available by calling @see isAvailable()
	  * 
	  * @throws SATSolverNotAvailableException if no SAT-Solver is present.
	  */
	 NativeSATSolver() throws SATSolverNotAvailableException {
		 if (!NativeSATSolver.isAvailable()) throw new ISATSolver.SATSolverNotAvailableException();
		 init();
	 }
	 
	 /**
	  * A pointer that can be used to map a native ISATSolver object to an solver "object" on native side.
	  */
	 public void setPointer(long pointer) {
		 this.pointer = pointer;
	 }
	 
	 /**
	  * Get the pointer defined by @see setPointer
	  */
	 public long getPointer() {
		 return this.pointer;
	 }
	 
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
	public native String signature();

	/* (non-Javadoc)
	 * @see jdrasil.sat.ISATSolver#init()
	 */
	@Override
	public native void init();

	/* (non-Javadoc)
	 * @see jdrasil.sat.ISATSolver#release()
	 */
	@Override
	public native void release();

	/* (non-Javadoc)
	 * @see jdrasil.sat.ISATSolver#add(int)
	 */
	@Override
	public native void add(int literal);

	/* (non-Javadoc)
	 * @see jdrasil.sat.ISATSolver#assume(int)
	 */
	@Override
	public native void assume(int literal);

	/* (non-Javadoc)
	 * @see jdrasil.sat.ISATSolver#solve()
	 */
	@Override
	public native int solve();

	/* (non-Javadoc)
	 * @see jdrasil.sat.ISATSolver#val(int)
	 */
	@Override
	public native int val(int literal);

	/* (non-Javadoc)
	 * @see jdrasil.sat.ISATSolver#failed(int)
	 */
	@Override
	public native boolean failed(int literal);

	/* (non-Javadoc)
	 * @see jdrasil.sat.ISATSolver#terminate()
	 */
	@Override
	public native void terminate();
	
}
