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

import java.util.ArrayList;
import java.util.Set;

import jdrasil.sat.Formula;
import pseudo.PBLib;

/**
 * In incremental SAT-solving, a SAT-solver is invoked multiple times with similar formulas.
 * The advantage is, that the solver can keep learned information about the formula and, therefore, can
 * hopefully solve later formulas faster. This is especially interesting if we wish to solve an optimization problem:
 * We build a formula that bound the solution to, say, k and as long as the formula is satisfiable we add constraints that reduce k.
 * However, since cardinality constraints are expensive, it is a resource waste to add a new cardinality constraint each time.
 * Instead, we use cardinality constraints that can be modified only by adding new clauses.
 * 
 * This class provides incremental cardinality constraints using the C++ library PBLib of Peter Steinke 
 * @url http://tools.computational-logic.org/content/pblib.php
 * 
 * @author Max Bannach
 */
public class IncrementalCardinalityEncoder {

	/** An interface to PBLib */
	private PBLib p;
	
	/**
	 * Default constructor, initialize a JNI bridge to PBlib.
	 */
	public IncrementalCardinalityEncoder() {
		p = new PBLib();
	}

    /**
	 * Initialize and add an incremental At-Most-k cardinality constraint for the given set of
	 * variables to the formula.
	 * @param phi
	 * @param variables
	 * @param k
	 */
	public void initAMK(Formula phi, Set<Integer> variables,  int k) {
        ArrayList<ArrayList<Integer>> clauses = p.initIterAtMostK(new ArrayList<>(variables), k, phi.getHighestVariable());
        for(ArrayList<Integer> c: clauses) {
            phi.addClause(c);
        }
    }

    /**
	 * Initialize and add an incremental At-Least-k cardinality constraint for the given set of
	 * variables to the formula.
	 * @param phi
	 * @param variables
	 * @param k
	 */
	public void initALK(Formula phi, Set<Integer> variables,  int k) {
        ArrayList<ArrayList<Integer>> clauses = p.initIterAtLeastK(new ArrayList<>(variables), k, phi.getHighestVariable());
        for(ArrayList<Integer> c: clauses) {
            phi.addClause(c);
        }    
    }

    /**
	 * Improve the upper-bound of @see pbIninitIncrementalAMK. Note that this does not take any variables.
	 * @param phi
	 * @param k
	 */
	public void incrementAMK(Formula phi,  int k) {
		ArrayList<ArrayList<Integer>> clauses = p.iterAtMostK(k, phi.getHighestVariable());
        for(ArrayList<Integer> c: clauses) {
        	phi.addClause(c);        	
        }
    }
	
    /**
	 * Improve the lower-bound of @see pbIninitIncrementalALK. Note that this does not take any variables.
	 * @param phi
	 * @param k
	 */
	public void incrementALK(Formula phi,  int k) {
		ArrayList<ArrayList<Integer>> clauses = p.iterAtLeastK(k, phi.getHighestVariable());
        for(ArrayList<Integer> c: clauses) {
        	phi.addClause(c);
        }
    }

}
