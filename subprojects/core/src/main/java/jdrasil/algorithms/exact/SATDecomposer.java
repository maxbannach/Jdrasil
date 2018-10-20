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
package jdrasil.algorithms.exact;

import java.io.Serializable;
import java.util.List;
import java.util.logging.Logger;


import jdrasil.algorithms.EliminationOrderDecomposer;
import jdrasil.graph.Graph;
import jdrasil.graph.TreeDecomposer;
import jdrasil.graph.TreeDecomposition;
import jdrasil.graph.TreeDecomposition.TreeDecompositionQuality;
import jdrasil.sat.Formula;
import jdrasil.sat.formulations.BaseEncoder;
import jdrasil.sat.formulations.ImprovedEncoder;
import jdrasil.utilities.logging.JdrasilLogger;

/**
 * This TreeDecomposer computes a tree-decomposition using the base encoding @see BaseEncoder.java.
 * The class is generic in the sence, that as initial parameter a SATSolver object is expected.
 * A call of @see call() will then envoce JNI and run the provides SATSolver with a constructed formula.
 * This may lead to multiple calls.
 * 
 * @author Max Bannach
 */
public class SATDecomposer<T extends Comparable<T>> implements TreeDecomposer<T>, Serializable {

	private static final long serialVersionUID = 1L;

	/** Jdrasils Logger */
	private final static Logger LOG = Logger.getLogger(JdrasilLogger.getName());

	/**
	 * Different encodings that are supported by this decomposer.
	 */
	public enum Encoding {
		BASE,
		IMPROVED
	}
	
	/** The graph that we want to decompose. */
	private final Graph<T> graph;
	
	/** A lower bound for the tree-width of the graph. */
	private int lb;
	
	/** A upper bound for the tree-width of the graph. */
	private int ub;
			
	/** The specific encoding this solver uses. */
	private Encoding encoding;
	
	/** The elimination order computed by some of the encodings. */
	private List<T> permutation;
	
	/**
	 * Initialize the algorithm. The problem will be solved by sending multiple formulas
	 * of the base encoding the given SATSolver. The optimal tree-width is searched in the interval [0,n].
	 * @param graph to be decomposed
	 * @param encoding that should be used
	 */
	public SATDecomposer(Graph<T> graph, Encoding encoding) {
		this.graph = graph;
		this.lb = 0;
		this.ub = graph.getCopyOfVertices().size();
		this.encoding = encoding;
	}
	
	/**
	 * Initialize the algorithm. The problem will be solved by sending multiple formulas
	 * of the base encoding the given SATSolver. The optimal tree-width is searched in the interval [lb,ub].
	 * @param graph The graph to be decomposed.
	 */
	public SATDecomposer(Graph<T> graph, Encoding encoding, int lb, int ub) {
		this.graph = graph;
		this.lb = lb;
		this.ub = ub;
		this.encoding = encoding;
	}

	/**
	 * Compute a optimal elimination order for the graph based on SAT-techniques.
	 * @see BaseEncoder
	 * @see ImprovedEncoder
	 * @return The computed elimination order.
	 */
	protected List<T> computePermutation() {
		// load the selected encoding
		BaseEncoder<T> encoder;
		switch (encoding) {
			case BASE:
				encoder = new BaseEncoder<>(graph);
				break;
			case IMPROVED:
				encoder = new ImprovedEncoder<>(graph);
				break;
			default:
				return null;
		}

		encoder.initCardinality(ub);
		Formula phi = encoder.getFormula();
		try {
			phi.registerSATSolver();
		} catch(Exception e) {
			LOG.warning("Failed to register the SAT solver");
		}
		
		// starting at the ub
		int k = ub;

		// as long as we can improve, improve
		try {
			while (phi.isSatisfiable() && k >= lb) {
				LOG.info("new upperbound: " + k);
				permutation = encoder.getPermutation(phi.getModel());
				k = k - 1;							
				encoder.improveCardinality(k);
			}
		} catch (Exception e) {}
		phi.unregisterSATSolver(); // clean up

		// done
		return permutation;
	}
	
	
	@Override
	public TreeDecomposition<T> call() throws Exception {
		// catch the empty graph
		if (graph.getCopyOfVertices().size() == 0) return new TreeDecomposition<>(graph);

		// compute the permutation with respect to the choose encoding
		List<T> permutation = computePermutation();

		// if something went wrong, return a trivial tree-decomposition
		if (permutation == null) {		
			TreeDecomposition<T> T = new TreeDecomposition<>(graph);
			T.createBag(graph.getCopyOfVertices());
			return T;
		}
		
		// done – return a corresponding decomposition
		return new EliminationOrderDecomposer<>(graph, permutation, TreeDecompositionQuality.Exact).call();
	}

	@Override
	public TreeDecompositionQuality decompositionQuality() {
		return TreeDecompositionQuality.Exact;
	}

	@Override
	public TreeDecomposition<T> getCurrentSolution() {
		try {
			switch (encoding) {
			case BASE:
				if (permutation != null) return new EliminationOrderDecomposer<>(graph, permutation, TreeDecompositionQuality.Heuristic).call();
				break;
			case IMPROVED:
				if (permutation != null) return new EliminationOrderDecomposer<>(graph, permutation, TreeDecompositionQuality.Heuristic).call();
				break;
			}
		} catch (Exception e) {
			return null;
		}
		return null;
	}
	
}
