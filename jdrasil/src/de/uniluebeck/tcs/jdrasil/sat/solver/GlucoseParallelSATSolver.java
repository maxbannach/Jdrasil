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

import glucp.JPGlucose;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.uniluebeck.tcs.jdrasil.sat.Formula;

/**
 * Glucose is a SAT-solver developed by Gilles Audemard and Laurent Simon, and can be obtained from http://www.labri.fr/perso/lsimon/glucose/.
 * This class capsules a Java-Native-Interface library (@see gluc.JPGlucose) for the SATSolver framework in a parallel version.
 * 
 * @author Max Bannach
 * @author Sebastian Berndt
 */
public class GlucoseParallelSATSolver extends SATSolver {

	private static final long serialVersionUID = -8183895047179827062L;

	/** Interface to Glucose. */
	private transient JPGlucose solver;

	/** A list of clauses */
    private List<List<Integer>> clauses;
	
	/** Store all used variables, in order to easily reconstruct a model. */
	private Set<Integer> variables;
	
	/** Default constructor without formula. */
	public GlucoseParallelSATSolver() {
		super();
        clauses = new ArrayList<>();
	}
	
	/** Default constructor with a start formula. */
	public GlucoseParallelSATSolver(Formula phi) {
		super(phi);
        clauses = new ArrayList<>();
        for(List<Integer> c: phi){
        	clauses.add(c);      	
        }
	}
	
	@Override
	void init() {
		variables = new HashSet<>();
	}
	
	@Override
	public void initSolver() {
	}

	@Override
	public void reset() {
        clauses.clear();
		init();
	};
	
	@Override
	void addClause(List<Integer> C) {
		for (Integer v : C) variables.add(Math.abs(v));
        clauses.add(C);
	}

	@Override
	public boolean solve() {
		solver = new JPGlucose();
        for(List<Integer> C : clauses){
        	solver.addClause(C);
        	
        }
       
        boolean b = solver.solve();
        return b;
	}

	@Override
	public Map<Integer, Boolean> getModel() {
		Map<Integer, Boolean> model = new HashMap<>();
		for (Integer v : variables) {
			model.put(v, solver.getValue(v) == 1);
		}
		return model;
	}

	@Override
	public SATSolver newInstance() {
		return new GlucoseParallelSATSolver();
	}

}
