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
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;

import jdrasil.algorithms.EliminationOrderDecomposer;
import jdrasil.graph.Graph;
import jdrasil.graph.TreeDecomposer;
import jdrasil.graph.TreeDecomposition;
import jdrasil.graph.TreeDecomposition.TreeDecompositionQuality;

/**
 * This class implements exact exponential time (and exponential space) algorithms to compute a tree-decomposition via dynamic programming.
 * The algorithms are based on "On Exact Algorithms for Treewidth" from Bodlaender, Fomin, Koster, Kratsch, and Thilikos (ESA 2006)
 * 
 * @param <T>
 * @author Max Bannach
 */
public class DynamicProgrammingDecomposer<T extends Comparable<T>> implements TreeDecomposer<T>, Serializable {

	private static final long serialVersionUID = -2397209961833807519L;

	/** The graph that we wish to decompose. */
	private final Graph<T> graph;
	
	/** The size of the graph that we decompose */
	private final int n;
	
	/** Bijection from V to {0,...,n-1} */
	private final Map<T, Integer> vertexToInt;
	private final Map<Integer, T> intToVertex;
		
	/** memorize the Q function */
	private final Map<BitSet, BitSet> memoryQ;
	
	/** memorization for the simple dynamic program */
	private final Map<BitSet, Integer> TW;
	
	/** store for a subgraph of G, which vertex should be eliminated next */
	private final Map<BitSet, Integer> vertexToEliminate;
	
	/** memorization for TWDP */
	private final Map<Integer, Map<BitSet, Integer>> TWi;
	
	/** an upper bound on the tree-width */
	private int ub;
	
	/** A clique in the graph */
	private BitSet clique;
	
	/** Three implementation modes of the cited paper. The optimized (and default) version is TWDP. */
	public enum Mode {
		simpleDP,
		recursiveDP,
		TWDP
	}
	
	/** The used mode. */
	private final Mode mode;
	
	
	/**
	 * The default constructor that initializes variables and data structures.
	 * This method will also compute a bijection from the vertices of the given graph 
	 * to {0,..,n-1}.
	 * 
	 * Beside the graph, an upper bound on the tree-width and a (not necessarily maximal) clique can be provided to speed-up
	 * the computation.
	 * 
	 * @param graph
	 * @param ub
	 * @param clique
	 */
	public DynamicProgrammingDecomposer(Graph<T> graph, int ub, Set<T> clique, Mode mode) {
		this.graph = graph;
		this.n = graph.getCopyOfVertices().size();
		this.vertexToInt = new HashMap<>();
		this.intToVertex = new HashMap<>();
		int i = 0;
		for (T v : graph) {
			vertexToInt.put(v, i);
			intToVertex.put(i,v);
			i = i+1;			
		}
		
		this.memoryQ = new HashMap<>();
		this.TW = new HashMap<>();
		this.TWi = new HashMap<>();
		this.vertexToEliminate = new HashMap<>();
		this.ub = ub;
		this.clique = new BitSet();
		for (T v : clique) this.clique.set(this.vertexToInt.get(v));
		this.mode = mode;
	}
	
	/**
	 * Initialize with just the graph, the upper bound will be set to n-1 and there will be no clique.
	 * @see DynamicProgrammingDecomposer#DynamicProgrammingDecomposer(jdrasil.graph.Graph, int, java.util.Set, jdrasil.algorithms.exact.DynamicProgrammingDecomposer.Mode)
	 * @param graph
	 */
	public DynamicProgrammingDecomposer(Graph<T> graph) {
		this(graph, graph.getCopyOfVertices().size()-1, new HashSet<T>(), Mode.TWDP);
	}
	
	/**
	 * Initialize with the graph and an upper bound, there will be no clique.
	 * @see DynamicProgrammingDecomposer#DynamicProgrammingDecomposer(jdrasil.graph.Graph, int, java.util.Set, jdrasil.algorithms.exact.DynamicProgrammingDecomposer.Mode)
	 * @param graph
	 */
	public DynamicProgrammingDecomposer(Graph<T> graph, int ub) {
		this(graph, ub, new HashSet<T>(), Mode.TWDP);
	}
	
	/**
	 * Initialize with the graph and a clique, the upper bound will be set to n-1.
	 * @see DynamicProgrammingDecomposer#DynamicProgrammingDecomposer(jdrasil.graph.Graph, int, java.util.Set, jdrasil.algorithms.exact.DynamicProgrammingDecomposer.Mode)
	 * @param graph
	 */
	public DynamicProgrammingDecomposer(Graph<T> graph, Set<T> clique) {
		this(graph, graph.getCopyOfVertices().size()-1, clique, Mode.TWDP);
	}
	
	/**
	 * Initialize with the graph and a mode. Notice that the default mode is TWDP, so this constructor does only make sense if you
	 * use simpleDP or recursiveDP, hence, an upper bound or clique has not to be set, as it is not used.
	 * @see DynamicProgrammingDecomposer#DynamicProgrammingDecomposer(jdrasil.graph.Graph, int, java.util.Set, jdrasil.algorithms.exact.DynamicProgrammingDecomposer.Mode)
	 * @param graph
	 */
	public DynamicProgrammingDecomposer(Graph<T> graph, Mode mode) {
		this(graph, graph.getCopyOfVertices().size()-1, new HashSet<T>(), mode);
	}
	
	/**
	 * For S subseteq V und v in V\S, Q(S,v) is the set of vertices w in V\S\{v} such that there is a path
	 * from v to w in G[S union {v,w}].
	 * 
	 * This method computes the set in time O(n+m) by performing a DFS starting at v in G[S union {v}] to obtain the connected component of v in this graph.
	 * All vertices that have a neighbor in this component constitute the set Q.
	 * 
	 * @param S
	 * @param v
	 * @return
	 */
	private BitSet Q(BitSet S, int v) {
		
		// check if the value was already computed
		S.set(n+v);
		if (memoryQ.containsKey(S)) {
			return memoryQ.get(S);
		} else {
			S.set(n+v, false);
		}		
		BitSet result = new BitSet();
		
		// perform DFS starting at s to compute the connected component of v in G[S union {v}]
		Stack<Integer> stack = new Stack<>();
		BitSet component = new BitSet();
		stack.push(v);
		component.set(v);
		while (!stack.isEmpty()) {
			int s = stack.pop();
			for (T neighbor : graph.getNeighborhood(intToVertex.get(s))) {
				int t = vertexToInt.get(neighbor);
				if (!component.get(t) && S.get(t)) {
					component.set(t);
					stack.push(t);
				}
			}			
		}
				
		// test all w in V\S\{v}
		for (T vertex : graph.getCopyOfVertices()) {
			int w = vertexToInt.get(vertex);
			if (w != v && !S.get(w)) { // not v and not in S 
				for (T neighbor : graph.getNeighborhood(vertex)) { // check the neighbors -> is one in the component in S?
					if (component.get(vertexToInt.get(neighbor))) { // if so, add w to Q
						result.set(w);
						break;
					}
				}
			}
		}
		
		// store the result for the future
		S.set(n+v);
		memoryQ.put(S, result);
		S.set(n+v, false);
		
		// done
		return result;
	}
	
	/**
	 * A simple recursive implementation of the dynamic program. (Theorem 3.3 of the cited paper).
	 * This method computes the tree-width of the graph if the vertices of S are already eliminated by recursively compute the tree-width of subsets of S.
	 * @return
	 */
	private int simpleDPrec(BitSet S) {
		
		// end of the recursion
		if (S.cardinality() == 0) return -1;
		
		// memorization
		if (TW.containsKey(S)) return TW.get(S);
		
		// the tw we try to minimize
		int result = Integer.MAX_VALUE;
		int nextV = -1;
		
		// iterate over every subset of S that is one vertex smaller		
		for (int v = S.nextSetBit(0); v >= 0; v = S.nextSetBit(v+1)) {
			
			BitSet Sprime = (BitSet) S.clone();
			Sprime.set(v, false); // remove v
			int value = Math.max(simpleDPrec(Sprime), Q(Sprime, v).cardinality()); // recursion
			if (value < result) { // take the best v
				result = value; 
				nextV = v;
			}
				 
			if (v == Integer.MAX_VALUE) {
				break; // or (i+1) would overflow
		    }
			
		}
		
		// store the result
		TW.put(S, result);
		vertexToEliminate.put(S, nextV);
		
		// done
		return result;
	}
	
	/**
	 * A simple iterative implementation of the dynamic program. (Theorem 3.3 of the cited paper).
	 * @return
	 */
	private int simpleDPitr() {
		
		// empty set is set to "-infinity" .... -1 is good enough, though
		TW.put(new BitSet(), -1);		
		Queue<BitSet> Q = new LinkedList<BitSet>();
		
		// initialize queue with |S|=1
		for (T vertex : graph.getCopyOfVertices()) {
			BitSet S = new BitSet();
			S.set(vertexToInt.get(vertex));
			Q.offer(S);
		}
		
		// with the queue we simulate the iteration over increasing subsets
		while (!Q.isEmpty()) {
			
			// current set
			BitSet S = Q.poll();
						
			// compute TW(S)
			int result = Integer.MAX_VALUE;
			int nextV = -1;
			for (int v = S.nextSetBit(0); v >= 0; v = S.nextSetBit(v+1)) {				
				BitSet Sprime = (BitSet) S.clone();
				Sprime.set(v, false); // remove v
				int value = Math.max(TW.get(Sprime), Q(Sprime, v).cardinality()); // recursion
				if (value < result) { // take the best v
					result = value; 
					nextV = v;
				}					 
				if (v == Integer.MAX_VALUE) {
					break; // or (i+1) would overflow
			    }
			}
			
			// store result
			TW.put(S, result);
			vertexToEliminate.put(S, nextV);
			
			// end of computation
			if (S.cardinality() == n) return result;
			
			// add next layer to Queue (i.e. S' with |S'|=|S|+1)
			for (T vertex : graph.getCopyOfVertices()) {
				int v = vertexToInt.get(vertex);
				if (!S.get(v)) {
					BitSet next = (BitSet) S.clone();
					next.set(v);
					if (!TW.containsKey(next)) {
						Q.offer(next);
						TW.put(next, Integer.MAX_VALUE);
					}					
				}				
			}
		}
		
		// error
		return -1;
	}
	
	/**
	 * The improved algorithm of the cited paper. The number of of considered subsets is reduced, and a given clique is used
	 * to reduce the search-space further.
	 * @param C
	 */
	private int TWDP(int ub, BitSet C) {
		
		// TW_0 contains only the pair (empty set, -infinity)
		TWi.put(0, new HashMap<>());
		TWi.get(0).put(new BitSet(), -1);
		
		// iteratively compute pairs for bigger subsets
		for (int i = 1; i <= n-C.cardinality(); i++) {
			TWi.put(i, new HashMap<>()); // initialize TW_i to be an empty set
			
			// iterate over previously computed pairs (S, r)
			for (BitSet S : TWi.get(i-1).keySet()) {
				int r = TWi.get(i-1).get(S); 
				
				// iterate over vertices x in V \ S
				for (T vertex : graph.getCopyOfVertices()) {
					int x = vertexToInt.get(vertex);
					if (S.get(x)) continue;
					
					int q = Q(S, x).cardinality();
					int r2 = Math.max(q,r);
					
					if (r2 <= ub) {
						
						// update upper bound
						if (r2 < ub) ub = Math.min(ub, n - S.cardinality() - 1);
						BitSet Sx = (BitSet) S.clone();
						Sx.set(x);
						
						// if there is already a pair, update it
						if (TWi.get(i).containsKey(Sx)) {
							int t = TWi.get(i).get(Sx);
							TWi.get(i).put(Sx, Math.min(t, r2));
							if (r2 < t) vertexToEliminate.put(Sx, x);
						} else { // otherwise insert new pair
							TWi.get(i).put(Sx, r2);
							vertexToEliminate.put(Sx, x);
						}
					}
				}

			}
		}
		
		// compute Bitset V\C
		BitSet VC = new BitSet();
		for (T vertex : graph.getCopyOfVertices()) {
			int v = vertexToInt.get(vertex);
			if (!C.get(v)) VC.set(v);
		}
		
		// if there is a pair (V\C, r) then return r
		if (TWi.get(n-C.cardinality()).containsKey(VC)) {
			return Math.max(C.cardinality()-1, TWi.get(n-C.cardinality()).get(VC));			
		} else { // else return the ub
			return ub;
		}
	}
	
	/**
	 * Compute an optimal elimination order of the graph.
	 * This method has to be called _after_ one of the following methods:
	 * @see DynamicProgrammingDecomposer#simpleDPrec(java.util.BitSet)
	 * @see DynamicProgrammingDecomposer#simpleDPitr()
	 * @see DynamicProgrammingDecomposer#TWDP(int, java.util.BitSet)
	 * @return
	 */
	private List<T> computeEliminationOrder() {
		
		// a optimal elimination order
		List<T> permutation = new LinkedList<>();
		
		// restore from saved data
		BitSet S = new BitSet();
		for (T v : graph) S.set(vertexToInt.get(v));
		while (S.cardinality() > 0) {
			int v = -1;
			if (vertexToEliminate.containsKey(S)) {
				v = vertexToEliminate.get(S);
			} else { // otherwise eliminate a vertex of the clique
				for (int i = clique.nextSetBit(0); i >= 0; i = clique.nextSetBit(i+1)) {
				    if (S.get(i)) {
				    	v = i;
				    	break;
				    } 
				 };
			}

			S.set(v, false);
			permutation.add(0, intToVertex.get(v));
		}
		
		// done
		return permutation;
	}
	
	@Override
	public TreeDecomposition<T> call() throws Exception {

		// run a corresponding algorithm, this will enable @see computeEliminationOrder()
		switch (mode) {
		case simpleDP:
			simpleDPitr();
			break;
		case recursiveDP:
			BitSet S = new BitSet();
			for (T v : graph) S.set(vertexToInt.get(v));			
			simpleDPrec(S);
			break;
		case TWDP:
			TWDP(ub, clique);
			break;
		default:
			break;
		}

		// return a corresponding decomposition
		return new EliminationOrderDecomposer<T>(graph, computeEliminationOrder(), TreeDecompositionQuality.Exact).call();
	}

	@Override
	public TreeDecomposition<T> getCurrentSolution() {
		return null;
	}

	@Override
	public TreeDecompositionQuality decompositionQuality() {
		return TreeDecompositionQuality.Exact;
	}
	
}
