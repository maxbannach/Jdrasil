package de.uniluebeck.tcs.jdrasil.sat.formulations;

/**
 * EmbeddingEncoding.java
 * @author bannach
 */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.uniluebeck.tcs.jdrasil.graph.Bag;
import de.uniluebeck.tcs.jdrasil.graph.Graph;
import de.uniluebeck.tcs.jdrasil.graph.TreeDecomposition;
import de.uniluebeck.tcs.jdrasil.sat.Formula;
import de.uniluebeck.tcs.jdrasil.sat.encodings.GenericCardinalityEncoder;

/**
 * The embedding-encoding computes a tree-decomposition of a given graph by embedding the vertices
 * of the graph into bags of a complete binary tree. The base encoding just requires that all vertices
 * and edges appear in at least on of the bags and that the bags are small. Incrementally lazy constraint will
 * ensure that bags containing the same vertex are connected.
 * 
 * This approach does only work for graphs that have an optimal balanced tree decomposition.
 */
public class EmbeddingEncoding<T extends Comparable<T>> {

	/** The graph we wish to decompose. */
	private final Graph<T> graph;
		
	/** Number of vertices in the graph. */
	private final int n;

	/** A bijection between the vertices and {1,...,n}. */
	protected final Map<T, Integer> vertexToInt = new HashMap<>();
	protected final Map<Integer, T> intToVertex = new HashMap<>();
	
	/** The variables used by this encoding. */
	private final int[][] bags;
	private final int[][][] edges;
	
	/** The formula created by this class .*/
	private final Formula phi;
	
	/** The constraints that ensure that the tree-width is at most k. */
	private Map<Integer, GenericCardinalityEncoder> amk;
	
	public EmbeddingEncoding(Graph<T> graph) {
		this.graph = graph;
		this.n = graph.getVertices().size();
		this.phi = new Formula();
		this.amk = new HashMap<>();
		
		// compute the bijection
		int varCount = 0;
		for (T v : graph) {
			varCount++;
			vertexToInt.put(v, varCount);
			intToVertex.put(varCount, v);
		}
		varCount = 1;
		
		// bags[i][j] is true, if vertex v_i is in bag b_j
		bags = new int[n+1][n+1];
		for (int i = 1; i <= n; i++) {
			for (int j = 1; j <= n; j++) {
				bags[i][j] = varCount;
				varCount = varCount + 1;
			}
		}
		
		// edges[i][j][k] is true, if edge {v_i,v_j} is in bag b_k
		edges = new int[n+1][n+1][n+1];
		for (int i = 1; i <= n; i++) {
			for (int j = 1; j <= n; j++) {
				for (int k = 1; k <= n; k++) {
					edges[i][j][k] = varCount;
					varCount = varCount + 1;
				}
			}
		}
		
		// init the formula
		baseEncoding();
	}
	
	/**
	 * Initialize the base encoding, this will ensure that the mapping maps every vertex to
	 * at least one bag, and that each edge is in at least on bag.
	 */
	private void baseEncoding() {
		
		// phi_1: ensures that each vertex is in at least one bag
		for (int i = 1; i <= n; i++) {
			List<Integer> C = new ArrayList<>(n);
			for (int k = 1; k <= n; k++) {
				C.add(bags[i][k]);
			}
			phi.addClause(C);
		}
		
		// phi_2: ensures every edge is in at least one bag
		for (T v : graph) {
			for (T w : graph.getNeighborhood(v)) {
				if (v.compareTo(w) < 0) {
					int i = vertexToInt.get(v);
					int j = vertexToInt.get(w);
					
					// ensure that edge is in one bag
					List<Integer> C = new ArrayList<>(n);
					for (int k = 1; k <= n; k++) {
						C.add(edges[i][j][k]);
					}
					phi.addClause(C);
					
					// if edge[i][j][k] is set <-> the edge is actually in the bag
					for (int k = 1; k <= n; k++) {
						phi.addClause(-1*bags[i][k], -1*bags[j][k], edges[i][j][k]);
						phi.addClause(-1*edges[i][j][k], bags[i][k]);
						phi.addClause(-1*edges[i][j][k], bags[j][k]);
					}
				}
			}
		}
		
	}
	
	/**
	 * Initialize cardinality constraint such that the given tree-decomposition has tree-width at most ub.
	 * By some cardinality constraints, the lb can be used to reduce search space.
	 */
	public void initCardinality(int lb, int ub) {
		for (int k = 1; k <= n; k++) {
			Set<Integer> C = new HashSet<>();
			for (int i = 1; i <= n; i++) {
				C.add(bags[i][k]);
			}
			GenericCardinalityEncoder encoder = new GenericCardinalityEncoder(phi, C, lb, ub);
			amk.put(k, encoder);
		}
	}
	
	/**
	 * Get the currently stores formula.
	 * @return
	 */
	public Formula getFormula() {
		return phi;
	}
	
	/**
	 * Lower the upper-bound on the tree-width.
	 * @param ub
	 */
	public Formula lowerUB(int ub) {
		Formula psi = new Formula();
		psi.setHighestVariable(phi.getHighestVariable());
		
		// just update constraint using existent encoder
		for (int i = 1; i <= n; i++) {
			amk.get(i).addAMK(psi, ub);
		}
		
		phi.setHighestVariable(psi.getHighestVariable());
		return psi;
	}
	
	/**
	 * Given a model for the currently stored formula, this method analyzes the induced tree-decomposition.
	 * If it is invalid, i.e., the subgraph a given vertex is not connected, a lazy constraint to fix the solution
	 * is returned and added to the locally stored formula.
	 * @return
	 */
	public Formula getLazyConstraint(Map<Integer, Boolean> model) {
		
		// compute a tree-decomposition
		TreeDecomposition<T> tree = getInducedTreeDecomposition(model);
		
		// compute an invalid path
		List<Bag<T>> path = new ArrayList<>(n);
		T v = tree.getInvalidVertex(path);
		
		// stop if decomposition is valid
		if (v == null) return null;
		
		// construct constraint as new formula
		Formula psi = new Formula();
		int i = vertexToInt.get(v);
		int s = path.get(0).id;
		int t = path.get(path.size()-1).id;
		
		// go along the path and ensure that v is in each bag
		for (int j = 1; j < path.size()-1; j++) {
			psi.addClause(-1*bags[i][s], -1*bags[i][t], bags[i][path.get(j).id]);
		}
		
		// done
		phi.and(psi);
		return psi;
	}
	
	/**
	 * Computes the tree-decomposition induced by the given formula for the locally stored formula.
	 * @param model
	 * @return
	 */
	public TreeDecomposition<T> getInducedTreeDecomposition(Map<Integer, Boolean> model) {	
		
		// the tree-decomposition we are creating
		TreeDecomposition<T> tree = new TreeDecomposition<>(graph);
		
		// we store the nodes of the decomposition as array and create the tree in a heap fashion
		ArrayList<Bag<T>> nodes = new ArrayList<Bag<T>>(n);
		
		// first create the bags
    	for (int k = 1; k <= n; k++) {
    		Set<T> set = new HashSet<>();
    		for (int i = 1; i <= n; i++) {
    			if (model.get(bags[i][k])) {
    				set.add(intToVertex.get(i));
    			}
    		}
    		nodes.add(tree.createBag(set));
    	}
		
    	// no connect them as binary tree (heap like)
    	for (int k = 0; k < n; k++){
    		if (2*k+1 < n){
    			tree.addTreeEdge(nodes.get(k), nodes.get(2*k+1));
    		}
    		if (2*k+2 < n){
    			tree.addTreeEdge(nodes.get(k), nodes.get(2*k+2));
    		}
    	}
    	
    	// done
		return tree;
	}
}
