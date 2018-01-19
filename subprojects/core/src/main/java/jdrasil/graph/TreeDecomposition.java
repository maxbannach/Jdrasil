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
package jdrasil.graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.logging.Logger;

import jdrasil.graph.invariants.MinimalSeparator;
import jdrasil.utilities.logging.JdrasilLogger;

/**
 * This class models a tree-decomposition of an undirected graph G.
 * A tree-decomposition is a tuple (T,iota) of a tree T and mapping iota that assigns a bag B_i to each
 * node n_i of T. A bag B_i is a subset of the vertices of G. A tree-decomposition has to satisfy the following properties:
 *   - all vertices in G are in some bag
 *   - each edge of G is in some bag
 *   - for each vertex v of G, the subgraph of T containing only bags that contain v is connected
 *
 * This class models the tree-decomposition by storing a tree (as graph of vertex type Bag) and a graph (as graph of vertex type T).
 * Bag is a vertex that stores a set of vertices of type T from G.
 *
 * A tree-decomposition should not be created manually but by a class implementing the tree-decomposer protocol.
 * @see TreeDecomposer
 *
 * @param <T> vertex type of the graph
 * @author Max Bannach
 */
public class TreeDecomposition<T extends Comparable<T>> implements java.io.Serializable {

	/** Jdrasils Logger */
	private final static Logger LOG = Logger.getLogger(JdrasilLogger.getName());

	private static final long serialVersionUID = -3485395969061663790L;

	/**
	 * This enum defines the quality of a tree-decomposition.
	 * An algorithm can produce a heuristic, approximation, or exact tree-decomposition.
	 * A heuristic does not has to produce a solution of width related to the actual tree-width of the graph. 
	 * An approximation has to provide some function that bounds the width of the solution with respect to the actual
	 * tree-width of the graph. Finally, an exact solution has to produce a tree-decomposition of width that equals 
	 * the tree-width of the graph.
	 */
	public enum TreeDecompositionQuality {
		Heuristic,
		Approximation,
		Exact
	}
	
	/** The number of bags of the tree-decomposition. */
	protected int numberOfBags;
	
	/** The width of the decomposition, i.e., the size of the largest bag minus 1. */
	protected int width;
	
	/** The size of the original graph. */
	protected int n;
	
	/** The tree of the tree-decomposition. */
	protected Graph<Bag<T>> tree;
	
	/** The original graph that is decomposed within this tree-decomposition. */
	private Graph<T> graph;
	
	private boolean createdFromPermutation;
	/**
	 * Default constructor initialize the tree-decomposition for a given graph.
	 * (This does not compute a tree-decomposition, the tree will be empty)
	 * 
	 * @param graph
	 */
	public TreeDecomposition(Graph<T> graph) {
		this.graph = graph;
		this.tree = GraphFactory.emptyGraph();
		this.numberOfBags = 0;
		this.width = -1;
		this.n = graph.getNumVertices();
		createdFromPermutation = false;
	}
	
	/**
	 * Getter for width of the tree decomposition.
	 * @return
	 */
	public int getWidth() {
		return width;
	}
	
	/**
	 * Setter for width of the tree decomposition.
	 * @param width to be set to
	 */
	public void setWidth(int width) {
		this.width = width;
	}
	
	/**
	 * Sets the size of the original graph.
	 * @param n
	 */
	public void setN(int n) {
		this.n = n;
	}
	
	/**
	 * Setter for the graph that is represented by this decomposition.
	 * @param graph
	 */
	public void setGraph(Graph<T> graph) {
		this.graph = graph;
		this.n = graph.getCopyOfVertices().size();
	}
	
	/**
	 * Add an edge to the tree. This will _not_ add the two bags, if the corresponding nodes
	 * are not already part of the decomposition. In this, the edge is not added.
	 * @param bi
	 * @param bj
	 */
	public void addTreeEdge(Bag<T> bi, Bag<T> bj) {
		if (bi == null || bj == null) return;
		if(!tree.containsNode(bi)) return;
		if(!tree.containsNode(bj)) return;
//		if (!tree.getVertices().contains(bi)) return;
//		if (!tree.getVertices().contains(bj)) return;
		
		if (tree.getNeighborhood(bi).contains(bj)) return;
//		this.tree.setDisableEdgeCounting(true);
		this.tree.addEdge(bi, bj);
	}
	
	/**
	 * Get the neighborhood of an bag.
	 * @param s
	 * @return
	 */
	public Set<Bag<T>> getNeighborhood(Bag<T> s) {
		return tree.getNeighborhood(s);
	}
	
	/**
	 * Create a new bag, adds it to the tree, and returns the corresponding object.
	 * @param bagVertices
	 * @return
	 */
	public Bag<T> createBag(Set<T> bagVertices) {
		Bag<T> bag = new Bag<>(bagVertices, 1 + numberOfBags++);
		this.tree.addVertex(bag);	
		int size = bagVertices.size() - 1;
		if (size > this.width) this.width = size;
		return bag;
	}
	
	/**
	 * Get the bags of the tree-decomposition.
	 * @return
	 */
	public Set<Bag<T>> getBags() {
		return tree.getCopyOfVertices();
	}
	
	/**
	 * Returns get number of bags stored in the tree-decomposition.
	 * @return
	 */
	public int getNumberOfBags() {
		return numberOfBags;
	}

	/**
	 * Sets the number of bags of the tree-decomposition.
	 * @param n
	 */
	public void setNumberOfBags(int n) { this.numberOfBags = n; }
	
	/**
	 * Returns the original graph that is decomposed.
	 * @return
	 */
	public Graph<T> getGraph() {
		return graph;
	}

	/**
	 * Getter for the underlying tree of the decomposition.
	 * @return
	 */
	public Graph<Bag<T>> getTree() { return tree; };
	
	/**
	 * Print the tree-decomposition.
	 * The String is already in the .td format specified by pace.
	 */
	public String toString() {
		return GraphWriter.treedecompositionToString(this);
	}
	
	/**
	 * Compute the connected components of the tree-decomposition of bags that contain the vertex $x$.
	 * For each connected component there will be one arbitrary vertex on the returned stack, i.e.,
	 * in a valid decomposition the size of the stack is 1.
	 * @param x The vertex for which we extract the connected components.
	 * @return A Stack containing one bag for each connected component that contains bags with $x$.
	 */
	private Stack<Bag<T>> connectedComponents(T x) {
		Stack<Bag<T>> startVertices = new Stack<>();
		Set<Bag<T>> visited = new HashSet<>();
		
		// one each node, there could start a connected component
		for (Bag<T> b : tree) {
			if (visited.contains(b) || !b.contains(x)) continue;
			startVertices.push(b);
			visited.add(b);
			
			// perform DFS on current vertex
			Stack<Bag<T>> S = new Stack<>();
			S.push(b);
			while (!S.isEmpty()) {
				Bag<T> v = S.pop();				
				for (Bag<T> w : tree.getNeighborhood(v)) {					
					if (!visited.contains(w) && w.contains(x))  {
						visited.add(w);
						S.push(w);
					}
				}
			}			
		}
		
		// done
		return startVertices;
	}

	/**
	 * This method checks whether nor not the tree decomposition is valid.
	 * If it is invalid, the reason will be logged.
	 * @return true if the decomposition is valid
	 */
	public boolean isValid() {
		boolean valid = true;

		// Property 1: every vertex is in a bag
		for (T v : graph) {
			boolean contained = false;
			for (Bag<T> b : tree) {
				if (b.contains(v)) contained = true;
			}
			if (!contained) {
				valid = false;
				LOG.warning("Vertex " + v + " not contained in any bag!");
			}
		}

		// Property 2: every edge is in a bag
		for (T v : graph) {
			for (T w : graph.getNeighborhood(v)) {
				if (v.compareTo(w) >= 0) continue;
				boolean contained = false;
				for (Bag<T> b : tree) {
					if (b.contains(v) && b.contains(w)) contained = true;
				}
				if (!contained) {
					valid = false;
					LOG.warning("Edge {" + v + ", " + w + "} not contained in any bag!");
				}
			}
		}

		// Property 3: subtrees connected
		for (T v : graph) {
			Stack<Bag<T>> S = connectedComponents(v);
			if (S.size() != 1) {
				valid = false;
				LOG.warning("Tree containing vertex " + v + " is not connected!");
			}
		}

		// done
		return valid;
	}

	/**
	 * If the tree-decomposition is not connected (i.\,e., the underlying graph is a forest), this method will connect
	 * it.
	 */
	public void connectComponents(){
		List<Set<Bag<T>>> comps = tree.getConnectedComponents();
		ArrayList<Bag<T>> heads = new ArrayList<>();
		LOG.info("got " + comps.size() + " components...");
		for(Set<Bag<T>> s : comps){
			for(Bag<T> b : s){
				heads.add(b);
				break;
			}
		}
		for(int i = 0 ; i < heads.size()-1 ; i++){
			addTreeEdge(heads.get(i),  heads.get(i+1));
		}
	}

	public boolean isCreatedFromPermutation() {
		return createdFromPermutation;
	}

	public void setCreatedFromPermutation(boolean createdFromPermutation) {
		this.createdFromPermutation = createdFromPermutation;
	}

}
