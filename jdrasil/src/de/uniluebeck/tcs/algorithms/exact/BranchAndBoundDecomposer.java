package de.uniluebeck.tcs.algorithms.exact;

/**
 * BranchAndBoundDecomposer.java
 * @author bannach
 */

import java.io.Serializable;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import de.uniluebeck.tcs.algorithms.EliminationOrderDecomposer;
import de.uniluebeck.tcs.algorithms.lowerbounds.MinorMinWidthLowerbound;
import de.uniluebeck.tcs.algorithms.upperbounds.MinFillInDecomposer;
import de.uniluebeck.tcs.graph.Graph;
import de.uniluebeck.tcs.graph.TreeDecomposer;
import de.uniluebeck.tcs.graph.TreeDecomposition;
import de.uniluebeck.tcs.graph.TreeDecomposition.TreeDecompositionQuality;

/**
 * A classical branch and bound algorithm based on QuickBB and its successors.
 * The algorithm searches through the space of elimination orders an utilizes dynamic programming,
 * reducing the search space to O(2^n).
 * 
 * @param <T>
 */
public class BranchAndBoundDecomposer<T extends Comparable<T>> implements TreeDecomposer<T>, Serializable {

	private static final long serialVersionUID = -6506020235954373541L;

	/** The graph we wish to decompose. (changes during computation) */
	private final Graph<T> graph, original;
	
	/** The size of the original graph. */
	private int n;
	
	/** An upper bound on the tree-width of the graph. */
	private int ub;

	/** Map the vertices of the graph to IDs */
	private final Map<T, Integer> vertexToID;
	
	/** Store the nodes that where already explored. */
	private final Map<Node, Integer> memorization;
	
	/** Store the vertex for every subgraph that has to be eliminated. */
	private final Map<BitSet, T> vertexToEliminate;
	
	/** A clique of the graph, we can eliminate this at last. */
	private Set<T> clique;
	
	/** The elimination order we try to compute. */
	private List<T> permutation;

	/** Source of randomness. */
	private final Random dice;
		
	/**
	 * The default constructor that initializes all the variables and data structures.
	 * @param graph
	 */
	public BranchAndBoundDecomposer(Graph<T> graph, Random dice) {
		this.graph = graph.copy();
		this.original = graph.copy();
		this.n = graph.getVertices().size();
		this.vertexToID = new HashMap<>();
		this.vertexToEliminate = new HashMap<>();
		int id = 0;
		for (T v : graph) {
			this.vertexToID.put(v, id);
			id++;
		}
		this.memorization = new HashMap<>();
		this.ub = n;
		this.dice = dice;
	}
	
	/**
	 * This class represents a node of the search tree.
	 */
	private class Node {
		// all vertices eliminated so far
		BitSet eliminatedVertices;
		
		// vertex currently eliminated
		T currentVertex;
		
		// width of the (partial) permutation represented by this node.
		int width;
		
		public Node() {
			this.width = 0;
			this.eliminatedVertices = new BitSet(); 
		}
		
		public Node(Node node, T v) {
			this.eliminatedVertices = (BitSet) node.eliminatedVertices.clone();
			this.eliminatedVertices.set(vertexToID.get(v));
			this.currentVertex = v;
			this.width = Math.max(node.width, graph.getNeighborhood(v).size());
		}
				
		@Override
		public int hashCode() {
			return eliminatedVertices.hashCode();
		}
		
		@Override
		public boolean equals(Object obj) {
			return this.hashCode() == obj.hashCode();
		}
		
	}
	
	/**
	 * Checks whether or not a node of the search tree was already visited.
	 * @param node
	 * @return
	 */
	private Integer memorized(Node node) {
		return memorization.get(node);
	}
	
	/**
	 * Remembers that the given node of the search tree was already visited and save the result.
	 * This will let memorized() prune this node in the future.
	 * @param node
	 */
	private void remember(Node node, Integer result) {
		memorization.put(node, result);
	}
	
	/**
	 * Checks whether or not a node represents a feasible solution.
	 * If so, the method checks if the solution is better then the current optimum and, if so, reports it.
	 * @param node
	 * @return
	 */
	private boolean solution(Node node) {		
		if (graph.getVertices().size() == 0) {
			if (node.width < ub) { // new optimum
				List<T> tmp = getPermutation();
				if (tmp != null) permutation = tmp;
				ub = node.width;
			}
			return true;
		}
		return false;
	}
	
	/**
	 * Compute the bounds for a node of the search tree and returns true,
	 * if this node can safely be pruned.
	 * @param node
	 * @return
	 */
	private boolean bound(Node node) {
		// simple prune with upper bound
		if (node.width >= ub) {
			return true;
		}

		// prune with lower bound
		int lb = 0;
		try {
			lb = new MinorMinWidthLowerbound<T>(graph, dice).call();			
		} catch (Exception e) {}

		if (lb >= ub) {
			return true;
		}
		
		// all prune rules failed
		return false;
	}
	
	/**
	 * Compute a list of successor nodes of the given node,
	 * i.e., the nodes we have to branch to.
	 */
	private List<Node> branch(Node node) {
		
		// the list of children we have to branch to
		List<Node> children = new LinkedList<>();
				
		// if there is a simplicial vertex, that is not in the clique, we can simply use that
		T simple = graph.getSimplicialVertex(clique);
		if (simple != null) {
			children.add(new Node(node, simple));
			return children;
		}
		
		// if there is an almost simplicial vertex, that is not in the clique, we can simply use that
		T almostSimple = graph.getAlmostSimplicialVertex(clique);
		if (almostSimple != null) {
			children.add(new Node(node, almostSimple));
			return children;
		}
		
		// compute a twin decomposition of the graph, since we have to branch to only one vertex of each twin group
		Map<T, Set<T>> twins = graph.getTwinDecomposition();
		for (Set<T> S : twins.values()) {
			
			// we only have to consider one vertex in each set
			T v = S.iterator().next(); 
			
			// we can skip the clique until the graph is almost empty
			if (clique.contains(v)) continue;
						
			// we can ignore neighbors, this costs O(1)
			if (graph.isAdjacent(v, node.currentVertex)) continue;
			
			// if we reach this point, we have to branch to v
			children.add(new Node(node, v));
		}
		
		// sort vertices by fillIn value
		children.sort((u,v) -> {
			int fillIn_u = graph.getFillInValue(u.currentVertex);
			int fillIn_v = graph.getFillInValue(v.currentVertex);
			if (fillIn_u < fillIn_v) return 1;
			if (fillIn_u > fillIn_v) return -1;
			return u.currentVertex.compareTo(v.currentVertex); // natural ordering
		});
		
		// at the end, we simply eliminate the clique
		if (children.size() == 0) {
			for (T v : clique) {
				if (graph.getVertices().contains(v)) {
					children.add(new Node(node, v));
					break;
				}
			}
		}
		
		// done
		return children;
	}
	
	/**
	 * Compute edges for the edge addition rule and add them to the graph.
	 * @return
	 */
	private List<T> edgeAdditionRule() {
		List<T> edgesToAdd = new ArrayList<>();
		for (T v : graph) {
			for (T w : graph) {
				if (v.compareTo(w) < 0 && !graph.isAdjacent(v, w)) {
					if (Math.min(graph.getNeighborhood(v).size(), graph.getNeighborhood(w).size()) <= ub) continue;
					int commonNeighbors = 0;
					for (T x : graph.getNeighborhood(w)) {
						if (graph.isAdjacent(v, x)) commonNeighbors++;
					}
					if (commonNeighbors > ub + 1) {
						edgesToAdd.add(v);
						edgesToAdd.add(w);
					}
				}
			}
		}
		for (int i = 0; i < edgesToAdd.size()-1; i += 2) {
			graph.addEdge(edgesToAdd.get(i), edgesToAdd.get(i+1));
		}
		return edgesToAdd;
	}
	
	/**
	 * Revert edge addition rules
	 * @param edges
	 */
	private void removeAddedEdges(List<T> edges) {
		for (int i = 0; i < edges.size()-1; i += 2) {
			graph.removeEdge(edges.get(i), edges.get(i+1));
		}
	}
	
	/**
	 * Classical Branch and Bound algorithm based on QuickBB and its successors.
	 * @param node
	 * @return
	 * @throws Exception 
	 */
	private int BB(Node node) throws Exception {
		if (Thread.currentThread().isInterrupted()) throw new Exception(); // timeout
		if (solution(node)) return 0; // end of recursion
		if (bound(node)) return Integer.MAX_VALUE; // we can prune
		
		Integer tw = memorized(node);
		if (tw == null) {
			
			// the best vertex to branch to
			T branchVertex = null;
			
			// Edge Addition Rule
			List<T> edgesToRemove = edgeAdditionRule();
			
			// handle children
			tw = Integer.MAX_VALUE;
			for (Node child : branch(node)) {
				int delta = child.currentVertex == null ? 0 : graph.getNeighborhood(child.currentVertex).size();
				Graph<T>.EliminationInformation info = graph.eliminateVertex(child.currentVertex);
				
				// store current branch
				vertexToEliminate.put(node.eliminatedVertices, child.currentVertex);
				
				// verify or search for better branch
				Integer childTW = BB(child);
				if (Math.max(childTW, delta) < tw) {					
					tw = Math.max(childTW, delta);
					branchVertex = child.currentVertex;				
				}
				
				graph.deEliminateVertex(info);
			}
			
			// remove added edges
			removeAddedEdges(edgesToRemove);
			
			// node is completely handled, store the result
			vertexToEliminate.put(node.eliminatedVertices, branchVertex);
			remember(node, tw);	
		}
		
		return memorized(node);
	}
	
	/**
	 * Compute the permutation induced by the root node of the search tree.
	 * The root has to be processed by BB() before.
	 * @return
	 */
	private List<T> getPermutation() {
		List<T> permutation = new ArrayList<T>();

		// recompute the permutation
		BitSet subgraph = new BitSet();
		while (subgraph.cardinality() < original.getVertices().size()) {
			T v = vertexToEliminate.get(subgraph);
			if (v == null) return null; // this happens if BB() does not improve the ub -> fast prune
			permutation.add(v);
			subgraph.set(vertexToID.get(v));
		}	
		
		return permutation;
	}
	
	@Override
	public TreeDecomposition<T> call() throws Exception {		
		
		// catch the empty graph
		if (graph.getVertices().size() == 0) return new TreeDecomposition<T>(graph);
				
		// compute upper and lower bounds
		MinFillInDecomposer<T> MinFill = new MinFillInDecomposer<T>(graph, dice);
		ub = MinFill.call().getWidth();
		int lb = new MinorMinWidthLowerbound<T>(graph, dice).call();
		permutation = MinFill.getPermutation();
		
		// we can safely eliminate a clique at last
		this.clique = graph.getMaximumClique(5);
		
		// call the branch and bound algorithm to find an optimal solution, this is any time so the currently best
		//  solution is always available
		if (ub != lb) {
			Node root = new Node();
			BB(root);
		}
				
		// done
		return new EliminationOrderDecomposer<T>(original, permutation, decompositionQuality()).call();
	}

	@Override
	public TreeDecompositionQuality decompositionQuality() {
		return TreeDecompositionQuality.Exact;
	}

	@Override
	public TreeDecomposition<T> getCurrentSolution() {
		try {
			if (permutation != null) return new EliminationOrderDecomposer<T>(original, permutation, TreeDecompositionQuality.Heuristic).call();
		} catch (Exception e) {
			return null;
		}
		return null;
	}
	
}
