package de.uniluebeck.tcs.sat.solver;

/**
 * GlucoseSATSolver.java
 * @author bannach
 */

import gluc.JGlucose;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.uniluebeck.tcs.sat.Formula;

/**
 * Glucose is a SAT-solver developed by Gilles Audemard and Laurent Simon, and can be obtained from http://www.labri.fr/perso/lsimon/glucose/.
 * This class capsules a Java-Native-Interface library (@see gluc.JGlucose) for the SATSolver framework.
 */
public class GlucoseSATSolver extends SATSolver {

	private static final long serialVersionUID = -4351494881352403571L;

	/** Interface to Glucose. */
	private transient JGlucose solver;
	
	/** Store all used variables, in order to easily reconstruct a model. */
	private Set<Integer> variables;
	
	
	/** Default constructor without formula. */
	public GlucoseSATSolver() {
		super();
	}
	
	/** Default constructor with a start formula. */
	public GlucoseSATSolver(Formula phi) {
		super(phi);
	}
	
	@Override
	void init() {		
		variables = new HashSet<>();
	}
	
	@Override
	public void initSolver() {
		solver = new JGlucose();		
	}
	
	@Override
	public void reset() {
		init();
	};

	@Override
	void addClause(List<Integer> C) {
		for (Integer v : C) variables.add(Math.abs(v));
		solver.addClause(C);
	}

	@Override
	public boolean solve() {
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
		return new GlucoseSATSolver();
	}

}
