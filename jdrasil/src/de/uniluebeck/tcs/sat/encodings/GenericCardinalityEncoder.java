package de.uniluebeck.tcs.sat.encodings;

/**
 * GenericCardinalityEncoder.java
 * @author bannach
 */

import java.util.Set;

import de.uniluebeck.tcs.sat.Formula;

/**
 * This class provides a generic interface to different cardinality constraints of the type "at-most-k" 
 * that can globally be changed. 
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
			CardinalityEncoder.getInstance().binomialAMK(phi, variables, k);
			break;
		case BINARY:
			CardinalityEncoder.getInstance().binaryAMK(phi, variables, k);
			break;
		case SEQUENTIAL:
			CardinalityEncoder.getInstance().sequentialAMK(phi, variables, k);
			break;
		case COMMANDER:
			CardinalityEncoder.getInstance().commanderAMK(phi, variables, k);
			break;
		case PBLIB:
			CardinalityEncoder.getInstance().pbAMK(phi, variables, k);
			break;
		case PBLIB_INCREMENTAL:
			incrementalEncoder.incrementAMK(phi, k);
			break;
		}
	}
	
}
