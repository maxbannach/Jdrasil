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
 * This class provides a generic interface to different cardinality constraints of the type "at-most-k" 
 * that can globally be changed. 
 * 
 * @author Max Bannach
 */
public class GenericCardinalityEncoder {

	/**
	 * This enum represents all cardinality and pseudo boolean encoding avoidable 
	 * in this software.
	 */
	public enum Encoding {
		BINOMIAL,
		BINARY,
		SEQUENTIAL,
		COMMANDER,
		PBLIB,
		PBLIB_INCREMENTAL,
	}
	
	/**
	 * The encoding used to encode cardinality constraints. This variable should be set to
	 * change the used encoding for all SAT-based solvers.
	 */
	public static Encoding usedEncoding = Encoding.SEQUENTIAL;
	
	/**
	 * The variables for which this constraint is.
	 */
	private Set<Integer> variables;
		
	/**
	 * An incremental cardinality encoder used if @see usedEncoding is PBLIB_INCREMENTAL.
	 */
	private IncrementalCardinalityEncoder incrementalEncoder;
	
	/**
	 * Create a new generic encoder. Depending on the used encoding @see usedEncoding this method may
	 * add clauses to phi that are later used (for instance a sorting network). However, there are also encoding for
	 * which this method does nothing.
	 * @param phi
	 * @param lb
	 * @param ub
	 */
	public GenericCardinalityEncoder(Formula phi, Set<Integer> variables, int lb, int ub) {
		this.variables = variables;
		if (usedEncoding == Encoding.PBLIB_INCREMENTAL) {
			incrementalEncoder = new IncrementalCardinalityEncoder();
			incrementalEncoder.initAMK(phi, variables, ub);
		}
	}
	
	/**
	 * Adds an AMK constraint to the formula based on the encoding stored in @see usedEncoding.
	 * Some encoding will automatically make multiple calls of this method incremental. 
	 * Between multiple calls of this method, @see usedEncoding should not be changed.
	 * @param phi
	 * @param variables
	 * @param k
	 */
	public void addAMK(Formula phi, int k) {
		switch (usedEncoding) {
		case BINOMIAL:
			BasicCardinalityEncoder.getInstance().binomialAMK(phi, variables, k);
			break;
		case BINARY:
			BasicCardinalityEncoder.getInstance().binaryAMK(phi, variables, k);
			break;
		case SEQUENTIAL:
			BasicCardinalityEncoder.getInstance().sequentialAMK(phi, variables, k);
			break;
		case COMMANDER:
			BasicCardinalityEncoder.getInstance().commanderAMK(phi, variables, k);
			break;
		case PBLIB:
			BasicCardinalityEncoder.getInstance().pbAMK(phi, variables, k);
			break;
		case PBLIB_INCREMENTAL:
			incrementalEncoder.incrementAMK(phi, k);
			break;
		}
	}
	
}
