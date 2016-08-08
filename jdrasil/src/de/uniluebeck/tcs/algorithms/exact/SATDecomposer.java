package de.uniluebeck.tcs.algorithms.exact;

/**
 * SATDecomposer.java
 * @author bannach
 */

import java.io.Serializable;
import java.util.List;

import de.uniluebeck.tcs.App;
import de.uniluebeck.tcs.algorithms.EliminationOrderDecomposer;
import de.uniluebeck.tcs.graph.Graph;
import de.uniluebeck.tcs.graph.TreeDecomposer;
import de.uniluebeck.tcs.graph.TreeDecomposition;
import de.uniluebeck.tcs.graph.TreeDecomposition.TreeDecompositionQuality;
import de.uniluebeck.tcs.sat.Formula;
import de.uniluebeck.tcs.sat.formulations.BaseEncoder;
import de.uniluebeck.tcs.sat.formulations.EmbeddingEncoding;
import de.uniluebeck.tcs.sat.formulations.ImprovedEncoder;
import de.uniluebeck.tcs.sat.formulations.LadderEncoder;
import de.uniluebeck.tcs.sat.solver.SATSolver;

/**
 * This TreeDecomposer computes a tree-decomposition using the base encoding @see BaseEncoder.java.
 * The class is generic in the sence, that as initial parameter a SATSolver object is expected.
 * A call of @see call() will then envoce JNI and run the provides SATSolver with a constructed formula.
 * This may lead to multiple calls.
 */
public class SATDecomposer<T extends Comparable<T>> implements TreeDecomposer<T>, Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * Different encodings that are supported by this decomposer.
	 */
	public enum Encoding {
		BASE,
		IMPROVED,
		LADDER,
		EMBEDDING;
	}
	
	/** The graph that we want to decompose. */
	private final Graph<T> graph;
	
	/** A lower bound for the tree-width of the graph. */
	private int lb;
	
	/** A upper bound for the tree-width of the graph. */
	private int ub;
		
	/** An interface to an SAT solver. */
	private final SATSolver solver;
	
	/** The specific encoding this solver uses. */
	private Encoding encoding;
	
	/** The elimination order computed by some of the encodings. */
	private List<T> permutation;
	
	/** The tree-decomposition computed by the embedding encoding */
	private TreeDecomposition<T> tree;
	
	/**
	 * Initialize the algorithm. The problem will be solved by sending multiple formulas
	 * of the base encoding the given SATSolver. The optimal tree-width is searched in the interval [0,n].
	 * @param graph
	 * @param solver
	 */
	public SATDecomposer(Graph<T> graph, SATSolver solver, Encoding encoding) {
		this.graph = graph;
		this.lb = 0;
		this.ub = graph.getVertices().size();
		this.solver = solver;
		this.encoding = encoding;
	}
	
	/**
	 * Initialize the algorithm. The problem will be solved by sending multiple formulas
	 * of the base encoding the given SATSolver. The optimal tree-width is searched in the interval [lb,ub].
	 * @param graph
	 * @param solver
	 */
	public SATDecomposer(Graph<T> graph, SATSolver solver, Encoding encoding, int lb, int ub) {
		this.graph = graph;
		this.lb = lb;
		this.ub = ub;
		this.solver = solver;
		this.encoding = encoding;
	}

	/**
	 * Compute a optimal elimination order for the graph based on SAT-techniques.
	 * @see BaseEncoder.java
	 * @see ImprovedEncoder.java
	 * @return
	 */
	protected List<T> computePermutation() {
		
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
				return null;
		}
		
		encoder.initCardinality(lb, ub);
		Formula phi = encoder.getFormula();				
		solver.addFormula(phi);	

		// starting at the ub, this will for sure give us one solution
		int k = ub;
		solver.addFormula(encoder.addAtMost(k));
		
		// as long as we can improve, improve
		while (solver.solve() && k >= lb) {
			if (k < ub) App.reportNewSolution(k);
			permutation = encoder.getPermutation(solver.getModel());
			k = k - 1;
			Formula psi = encoder.addAtMost(k);
			solver.addFormula(psi);
		}
				
		// done
		return permutation;
	}
	
	/**
	 * The embedding-encoding tries to find an optimal tree-decomposition by embedding
	 * the vertices into the bags of a complete binary tree.
	 * @return
	 */
	protected TreeDecomposition<T> embeddedTreeDecomposition() {
		
		// create the base encoding
		EmbeddingEncoding<T> encoder = new EmbeddingEncoding<>(graph);
		encoder.initCardinality(lb, ub);
		solver.addFormula(encoder.getFormula());
		
		// start at the upper bound, so we will find a decomposition for sure
		int k = ub+1;
		solver.addFormula(encoder.lowerUB(k));
		
		// as long as we can improve -> improve
		search: while (solver.solve() && k >= lb) {
			
			// find a model that actually induces a tree-decomposition
			Formula lazy = encoder.getLazyConstraint(solver.getModel());
			while (lazy != null) {
				solver.addFormula(lazy);
				if (!solver.solve()) break search;
				lazy = encoder.getLazyConstraint(solver.getModel());
			}
			
			// at this point a model induces a valid tree-decomposition
			tree = encoder.getInducedTreeDecomposition(solver.getModel());
			
			// lets try to improve it
			k = k - 1;
			solver.addFormula(encoder.lowerUB(k));
		}
		
		// done
		return tree;
	}
	
	@Override
	public TreeDecomposition<T> call() throws Exception {
		solver.initSolver();
		
		// catch the empty graph
		if (graph.getVertices().size() == 0) return new TreeDecomposition<T>(graph);
						
		// this encoding directly produces a decomposition
		if (encoding == Encoding.EMBEDDING) {
			return embeddedTreeDecomposition();
		}
		
		// compute the permutation with respect to the choose encoding
		List<T> permutation = computePermutation();

		// if something went wrong, return a trivial tree-decomposition
		if (permutation == null) {		
			TreeDecomposition<T> T = new TreeDecomposition<>(graph);
			T.createBag(graph.getVertices());
			return T;
		}
		
		// done – return a corresponding decomposition
		return new EliminationOrderDecomposer<T>(graph, permutation, TreeDecompositionQuality.Exact).call();
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
				if (permutation != null) return new EliminationOrderDecomposer<T>(graph, permutation, TreeDecompositionQuality.Heuristic).call();
				break;
			case IMPROVED:
				if (permutation != null) return new EliminationOrderDecomposer<T>(graph, permutation, TreeDecompositionQuality.Heuristic).call();
				break;
			case LADDER:
				if (permutation != null) return new EliminationOrderDecomposer<T>(graph, permutation, TreeDecompositionQuality.Heuristic).call();
				break;
			case EMBEDDING:
				return tree;
			}
		} catch (Exception e) {
			return null;
		}
		return null;
	}
	
}
