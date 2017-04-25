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
import java.util.Set;
import java.util.Stack;

import jdrasil.algorithms.lowerbounds.MinorMinWidthLowerbound;
import jdrasil.graph.Bag;
import jdrasil.graph.Graph;
import jdrasil.graph.TreeDecomposer;
import jdrasil.graph.TreeDecomposition;
import jdrasil.graph.TreeDecomposition.TreeDecompositionQuality;

/**
 * An alternative definition of tree-width is about the cops and robbers game.
 * This class checks if the cops in this game have a winning strategy with classic dynamic programming.
 * 
 * @param <T>
 * @author Max Bannach
 */
public class CopsAndRobber<T extends Comparable<T>> implements TreeDecomposer<T>, Serializable {

	private static final long serialVersionUID = -8191701142481993553L;

	/** The graph that we wish to decompose. */
	private final Graph<T> graph;
	
	/** The size of the graph that we decompose */
	private final int n;
	
	/** Bijection from V to {0,...,n-1} */
	private final Map<T, Integer> vertexToInt;
	private final Map<Integer, T> intToVertex;
	
	/** The dynamic programming table */
	private final Map<Node, Boolean> memorization;
	
	/** 
	 * The winning strategy computed for the cops. 
	 * With this the actual tree-decomposition can be restored. 
	 * The winning strategy is represented as a directed tree, which actually contains
	 * the tree-decomposition as subgraph. 
	 **/
	private final Map<BitSet, List<BitSet>> winningStrategy;
		
	/**
	 * This class represents a configuration of the cops and robber games, i.e.,
	 * a tuple (X,y) where X is the set of cop positions and y is the position of the robber.
	 */
	class Node {		
		BitSet cops; int robber;	
		public Node(BitSet cops, int robber) {
			this.cops = cops;
			this.robber = robber;
		}
		
		@Override
		public int hashCode() {
			BitSet tmp = (BitSet) cops.clone();
			tmp.set(n + robber, true);
			return tmp.hashCode();
		}
		
		@Override
		public boolean equals(Object obj) {
			if(obj instanceof CopsAndRobber.Node){
				Node n = (CopsAndRobber.Node) obj;
				return n.robber == this.robber && n.cops.equals(this.cops);
			}
			return false;
//			return this.hashCode() == obj.hashCode();
		}
	}
	
	/**
	 * The default constructor that initializes variables and data structures.
	 * This method will also compute a bijection from the vertices of the given graph 
	 * to {0,..,n-1}.
	 * @param graph
	 */
	public CopsAndRobber(Graph<T> graph) {
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
		this.memorization = new HashMap<>();
		this.winningStrategy = new HashMap<>();
	}
	
	/**
	 * Computes the reachable connected component of the robber in the configuration x, i.e.,
	 * without crossing the cops.
	 * @param x
	 * @return
	 */
	private BitSet reach(Node x) {
		BitSet area = new BitSet(n);
		
		// perform DFS starting at the robbers position
		Stack<T> S = new Stack<>();
		area.set(x.robber);
		S.push(intToVertex.get(x.robber));
		while (!S.isEmpty()) {
			T v = S.pop();
			for (T w : graph.getNeighborhood(v)) {
				int wi = vertexToInt.get(w);
				if (!x.cops.get(wi) && !area.get(wi)) { // go to non-blocked & non-visited vertices					
					area.set(wi);
					S.push(w);
				}
			}
		}
		
		// done
		return area;
	}
	
	/**
	 * Given a set cops that separate the area from the rest of the graph, this method removes all cops
	 * that are not necessary for this task,
	 * @param cops
	 * @param area
	 * @return
	 */
	private BitSet reduceCops(BitSet cops, BitSet area) {
		BitSet newCops = (BitSet) cops.clone();
		
		// iterate over the cops
		for (int i = cops.nextSetBit(0); i >= 0; i = cops.nextSetBit(i+1)) {
			T v = intToVertex.get(i);
			boolean reduce = true;
			for (T w : graph.getNeighborhood(v)) {
				if ( area.get(vertexToInt.get(w)) ) {
					reduce = false;
					break;
				}
			}
			if (reduce) newCops.clear(i);
		    if (i == Integer.MAX_VALUE) break;
		 }
		
		// done
		return newCops;
	}
	
	/**
	 * The actual dynamic program that checks if the cops have a winning strategy in configuration x.
	 * @param x
	 * @param k
	 * @return
	 */
	private boolean computeWinningStrategy(Node x, int k) {
		
		// use memorization
		if (memorization.containsKey(x)) return memorization.get(x);
	
		boolean result = false;
		BitSet cops = x.cops;
		BitSet area = reach(x); // compute robbers area
		BitSet nextBag = new BitSet(n); // the next bag, computed during this method
		
		if (cops.cardinality() + area.cardinality() <= k) {
			// end of recursion - cops win
			result = true;
			nextBag = (BitSet)cops.clone();
			nextBag.or(area);
		} else if (cops.cardinality() == k) {
			// game goes on, but we can not introduce new cops
			nextBag = reduceCops(cops, area);			
			if (nextBag.cardinality() == cops.cardinality()) {
				// cops can make no monotone move -> robber wins
				result = false;
			} else {
				Node y = new Node(nextBag, x.robber);
				result = computeWinningStrategy(y, k);			
			}
		} else { // we can add new cops in this case
			
			// iterate over all possible new cops
			for (int i = area.nextSetBit(0); i >= 0; i = area.nextSetBit(i+1)) {	
				nextBag = (BitSet) cops.clone();
				nextBag.set(i);				
				boolean valid = true; // check if it was a good move

				// now we have to test all possible robber positions
				for (int j = area.nextSetBit(0); j >= 0; j = area.nextSetBit(j+1)) {
					if (i == j) continue; // robber does not move to the cop
					Node y = new Node(nextBag, j);
								
					if (!computeWinningStrategy(y, k)) {
						// robber can win in this configuration, we can stop
						valid = false;
						break;
					}
					
					if (j == Integer.MAX_VALUE) break;
				}
				
				if (valid) {
					// we can stop if we found any valid cop move
					result = true;									
					break;
				}
			 }
		}
				
		// report an edge of the tree-decomposition
		if (result) {
			reportTreeEdge(cops, nextBag);
		}
		
		// done - store and return
		memorization.put(x, result);
		return result;		
	}
	
	/**
	 * Reports a edge of the tree-decomposition as part of the winning strategy of the cops.
	 * This edge may be a false alarm and has to be deleted if it is not in the connected component of
	 * the empty bag.
	 * @param cops
	 * @param nextBag
	 */
	private void reportTreeEdge(BitSet cops, BitSet nextBag) {
		if (!winningStrategy.containsKey(cops)) winningStrategy.put(cops, new LinkedList<>());
		if (!winningStrategy.containsKey(nextBag)) winningStrategy.put(nextBag, new LinkedList<>());
		if (!winningStrategy.get(cops).contains(nextBag)) winningStrategy.get(cops).add(nextBag);
	}
	
	/**
	 * Create a tree-decomposition bag corresponding to the given BitSet in the given tree.
	 * @param bs
	 * @param tree
	 * @return
	 */
	private Bag<T> bitsetToBag(BitSet bs, TreeDecomposition<T> tree) {
		Set<T> vertices = new HashSet<>();
		for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i+1)) {
			vertices.add(intToVertex.get(i));
		    if (i == Integer.MAX_VALUE) break;
		}
		return tree.createBag(vertices);
	}
	
	/**
	 * Compute the actual tree-decomposition from a winning strategy of the cops.
	 * This method should be called after @see computeWinningStrategy was envoced on a empty start configuration.
	 * @return
	 */
	private TreeDecomposition<T> computeTreeDecomposition() {
		TreeDecomposition<T> tree = new TreeDecomposition<>(graph);		
		Map<BitSet, Bag<T>> setToBag = new HashMap<>();
		
		// catch the empty graph
		if (graph.getCopyOfVertices().size() == 0) return tree;
		
		// perform DFS starting on root node
		Stack<BitSet> S = new Stack<>();
		BitSet empty = new BitSet(n);
		S.push(empty);
		setToBag.put(empty, bitsetToBag(empty, tree));
		while (!S.isEmpty()) {
			BitSet u = S.pop();
			for (BitSet v : winningStrategy.get(u)) {
				if (!setToBag.containsKey(v)) setToBag.put(v, bitsetToBag(v, tree));
				tree.addTreeEdge(setToBag.get(u), setToBag.get(v));
				S.push(v);
			}			
		}
				
		// done
		return tree;
	}
	
	@Override
	public TreeDecomposition<T> call() throws Exception {

		// compute a lowerbound
		MinorMinWidthLowerbound<T> mmw = new MinorMinWidthLowerbound<T>(graph);
		int lb = mmw.call();

		// search a k for which the cops have a winning strategy
		int k = lb;
		BitSet root = new BitSet(n);
		while (!computeWinningStrategy(new Node(root, 0), k)) {			
			memorization.clear();
			winningStrategy.clear();
			k = k + 1;
		}
		
		// done, compute the corresponding decomposition
		return computeTreeDecomposition();
		
	}

	@Override
	public TreeDecompositionQuality decompositionQuality() {
		return TreeDecompositionQuality.Exact;
	}

	@Override
	public TreeDecomposition<T> getCurrentSolution() {
		return null;
	}
	
}
