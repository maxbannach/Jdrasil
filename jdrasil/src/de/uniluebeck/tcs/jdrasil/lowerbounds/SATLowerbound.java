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
package de.uniluebeck.tcs.jdrasil.lowerbounds;

import java.io.Serializable;

import de.uniluebeck.tcs.jdrasil.graph.Graph;
import de.uniluebeck.tcs.jdrasil.sat.Formula;
import de.uniluebeck.tcs.jdrasil.sat.formulations.BaseEncoder;
import de.uniluebeck.tcs.jdrasil.sat.formulations.ImprovedEncoder;
import de.uniluebeck.tcs.jdrasil.sat.formulations.LadderEncoder;
import de.uniluebeck.tcs.jdrasil.sat.solver.SATSolver;

/**
 * This class computes a lower bound on the tree-width of G by solving sat-instances with increasing cardinality constraints until
 * the first satisfiable formula is found (then the tree-width is known) or until a timeout is reached.
 * 
 * @param <T>
 * @author Max Bannach
 */
public class SATLowerbound<T extends Comparable<T>> implements Lowerbound<T>, Serializable {

	private static final long serialVersionUID = 6896809338813200373L;

	/**
	 * Different encodings that are supported to compute lower bounds.
	 */
	public enum Encoding {
		BASE,
		IMPROVED,
		LADDER,
	}
	
	/** The graph for which we wish to compute a lower bound. */
	private Graph<T> graph;
	
	/** The currently known lower bound. */
	private int lb;
	
	/** This value is true if the lower bound is actually the optimal solution. */
	private boolean optimal;
	
	/** The SAT-solver used to compute the lower bound. */
	private SATSolver solver;
	
	/** The SAT-encoding used to compute the lower bound. */
	private final Encoding encoding;
	
	/**
	 * Default constructor that initialize data structures.
	 * @param graph
	 */
	public SATLowerbound(Graph<T> graph, SATSolver solver, Encoding encoding) {
		this.graph = graph;
		this.lb = 1;
		this.optimal = false;
		this.solver = solver;
		this.encoding = encoding;
	}
	
	/**
	 * Constructor that initialize data structures such that the algorithm starts
	 * at the given lower bound, i.e., if the class is used to improve a known lower bound.
	 * @param graph
	 */
	public SATLowerbound(Graph<T> graph, SATSolver solver, Encoding encoding, int lb) {
		this.graph = graph;
		this.lb = lb;
		this.optimal = false;
		this.solver = solver;
		this.encoding = encoding;
	}
	
	/**
	 * Test if the graph has tree-width k, otherwise k is a lower bound.
	 * @param k
	 * @return
	 */
	private boolean checkLb(int k) {
		
		// load the selected encoding
		BaseEncoder<T> encoder = null;
		switch (encoding) {
			case BASE:
				encoder = new BaseEncoder<>(graph);
				break;
			case IMPROVED:
				encoder = new ImprovedEncoder<>(graph);
				break;
			case LADDER:
				encoder = new LadderEncoder<>(graph);
				break;
			default:
				return false;
		}	
		
		// we need a new formula, so restart the solver
		solver = solver.newInstance();
// Constitute the formula
		encoder.initCardinality(k,k);
		Formula phi = encoder.getFormula();			
		solver.addFormula(phi);
		solver.addFormula(encoder.addAtMost(k));
		// if the formula is not satisfiable, then k is a lower bound 
		boolean b = solver.solve();
		return !b;
	}
	
	@Override
	public Integer call() throws Exception {
		solver.initSolver();
		int k = lb;
		while (checkLb(k)) {
			k = k + 1;
			lb = k;
			if (Thread.currentThread().isInterrupted()) return k; // timeout
		}
		optimal = true;
		
		return k;
	}

	/** Checks if the computed lower bound is the optimum. */
	public boolean foundOptimum() {
		return optimal;
	}
	
	@Override
	public Integer getCurrentSolution() {
		return lb;
	}
	
}
