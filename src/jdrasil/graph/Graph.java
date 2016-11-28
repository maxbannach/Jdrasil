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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import jdrasil.utilities.PartitionRefinement;

/**
 * This class represents a simple undirected graph and provides basic methods to modify it.
 * The graph is stored as adjacency list, making edge iterations effective but adjacency tests expensive.
 *
 * This class should be constructed with the graph factory.
 * @see GraphFactory 
 *
 * @param <T> The type of vertices is generic.
 * @author Max Bannach
 * @author Sebastian Berndt
 * @author Thorsten Ehlers
 */
public class Graph<T extends Comparable<T>> implements Iterable<T>, Serializable {
	
	private static final long serialVersionUID = -6506030235954373541L;
	
	/**
	 * The graph is stored as adjacency list, i.e., for every vertex of the graph
	 * there is one entry in the map that points to a list of neighbors.
	 */
	private Map<T, List<T>> adjacencyList;
	
	/**
	 * We also store edges in HashSets to get O(1) adjacency tests.
	 */
	private Map<T, Set<T>> adjacencies;
	
	/**
	 * Store the number of edges in the neighborhood of the vertices,
	 * this value can be used to determine if a vertex is simplicial in O(1).
	 */
	private Map<T, Integer> edgesInNeighborhood;
	
	/** The number of edges in the graph .*/
	private int m;
	
	/**
	 * Package private constructor, only initialize data structures.
	 */
	Graph() {
		adjacencyList = new HashMap<>();	
		adjacencies = new HashMap<>();
		edgesInNeighborhood = new HashMap<>();
	}
	
	
	/**
	 * Returns the vertices set of this graph. This is actually a Java set of
	 * the generic vertex type.
	 * @return Set<T> the vertices of the represented graph
	 */
	public Set<T> getVertices() {
		return new HashSet<T>(adjacencyList.keySet());
	}
	
	public boolean containsNode(T v){
		return adjacencyList.containsKey(v);
	}
	
	/**
	 * Compute the maximum degree of this graph.
	 * @return Delta(G)
	 */
	public int getMaxDegree() {
		int max = Integer.MIN_VALUE;
		for (T v : this) {
			if (getNeighborhood(v).size() > max) max = getNeighborhood(v).size();
		}
		return max;
	}
	
	/**
	 * Returns the neighborhood of the requested vertex.
	 * This will return an empty list if the vertex is isolated and null if the vertex is not part
	 * of the represented graph.
	 * @param v the requested vertex  
	 * @return List<T> the neighborhood of v
	 */
	public List<T> getNeighborhood(T v) {
		return adjacencyList.get(v);
	}
	
	/**
	 * Check if two vertices are adjacent
	 * @param u
	 * @param v
	 * @return
	 */
	public boolean isAdjacent(T u, T v) {
		if (adjacencies.get(u) == null) return false;
		return adjacencies.get(u).contains(v);
	}
	
	/**
	 * Adds a vertex to the graph with an initialized empty neighborhood.
	 * This method will do nothing if the given vertex is already part of the graph.
	 * @param v - the vertex to bed added
	 */
	public void addVertex(T v) {
		if (adjacencyList.get(v) == null) {
			adjacencyList.put(v, new ArrayList<T>());
			adjacencies.put(v, new HashSet<>());
			edgesInNeighborhood.put(v, 0);
		}
	}
	
	/**
	 * Adds an undirected edge {u,v} to the graph by adding the vertices to the neighborhood of the corresponding other vertex.
	 * If u or v is not in the graph they will automatically be added.
	 * 
	 * This method costs O(delta(u)), as it updates the simplicial properties
	 * 
	 * @param u - an endpoint of the edge
	 * @param v - an endpoint of the edge
	 */
	public void addEdge(T u, T v) {
		addVertex(u);
		addVertex(v);
		if (adjacencyList.get(u).contains(v)) return;
		if (adjacencyList.get(v).contains(u)) return;
		m++;
		adjacencyList.get(u).add(v);
		adjacencyList.get(v).add(u);
		adjacencies.get(u).add(v);
		adjacencies.get(v).add(u);
		
		// update number of neighbor edges value
		for (T x : getNeighborhood(u)) {
			if (isAdjacent(x, v)) {
				edgesInNeighborhood.put(x, edgesInNeighborhood.get(x)+1);
				edgesInNeighborhood.put(u, edgesInNeighborhood.get(u)+1);
				edgesInNeighborhood.put(v, edgesInNeighborhood.get(v)+1);
			}
		}
	}
	
	/**
	 * Remove a given edge from the graph.
	 * @param u
	 * @param v
	 */
	public void removeEdge(T u, T v) {
		m--;
		adjacencyList.get(u).remove(v);
		adjacencyList.get(v).remove(u);
		adjacencies.get(u).remove(v);
		adjacencies.get(v).remove(u);
		
		// update number of neighbor edges value
		for (T x : getNeighborhood(u)) {
			if (isAdjacent(x, v)) {
				edgesInNeighborhood.put(x, edgesInNeighborhood.get(x)-1);
				edgesInNeighborhood.put(u, edgesInNeighborhood.get(u)-1);
				edgesInNeighborhood.put(v, edgesInNeighborhood.get(v)-1);
			}
		}
	}
	
	/**
	 * Returns the number of edges present in the graph.
	 * @return
	 */
	public int getNumberOfEdges() {
		return m;
	}
	
	/**
	 * Delete a given vertex from the graph.
	 * @param v
	 */
	public void deleteVertex(T v) {
		
		// remove the edges from other vertices
		for (T u : new ArrayList<>(getNeighborhood(v))) {
			removeEdge(u, v);
		}
				
		// actually remove the vertex
		adjacencyList.remove(v);
		adjacencies.remove(v);
		edgesInNeighborhood.remove(v);
	}
	
	/**
	 * This method performs an edge contraction on the graph contracting the edge {v, w}.
	 * This will identify w as v and map all edges of w to v, afterwards w is removed.
	 * If v and w have a common neighbor, resulting multi edges are removed, i.e., the result
	 * is a simple graph again.
	 * @param v
	 * @param w
	 */
	public void contract(T v, T w) {
		
		// add the neighbors of w to N(v)
		for (T u : getNeighborhood(w)) {
			if (!u.equals(v) && !isAdjacent(u, v)) {
				addEdge(u, v);
			}
		}
		
		// delete w
		deleteVertex(w);
	}
	
	/**
	 * This methods performs the vertex elimination operation on a given vertex v.
	 * Eliminating a vertex means: (a) making its neighborhood a clique and (b) deleting the vertex from the graph.
	 * @param v
	 */
	public EliminationInformation eliminateVertex(T v) {

		EliminationInformation info = new EliminationInformation(v);
		
		// make the neighborhood of v a clique
		for (T u : getNeighborhood(v)) {
			info.addNeighbors(u);
			for (T w : getNeighborhood(v)) {
				if (u.compareTo(w) < 0 && !isAdjacent(u, w)) {
					addEdge(u, w);
					info.addEdge(u, w);
				}
			}
		}

		// delete the vertex
		deleteVertex(v);
		
		return info;
	}
	
	/**
	 * Undo the elimination of a vertex.
	 * @param info
	 */
	public void deEliminateVertex(EliminationInformation info) {
		
		// remove inserted edge
		for (int i = 0; i < info.addedEdges.size()-1; i+=2) {	
			T u = info.addedEdges.get(i);
			T v = info.addedEdges.get(i+1);	
			removeEdge(u, v);
		}
		
		// add deleted vertex
		addVertex(info.vertex);
		
		// add deleted edges
		for (T u : info.neighborhood) {
			addEdge(info.vertex, u);
		}
		
	}
	
	/**
	 * This class stores information generated by the elimination of a vertex.
	 * These informations are needed to de-eliminate the vertex, i.e., to undo the elimination.
	 */
	public class EliminationInformation {
		List<T> addedEdges;
		Set<T> neighborhood;
		T vertex;
		
		public EliminationInformation(T vertex) {
			this.vertex = vertex;
			this.neighborhood = new HashSet<>();
			this.addedEdges = new ArrayList<>();
		}
		
		public void addEdge(T u, T v) {
			addedEdges.add(u);
			addedEdges.add(v);
		}
		
		public void addNeighbors(T v) {
			neighborhood.add(v);
		}
		
	}
	
	/**
	 * Construct a copy of this graph.
	 * @return
	 */
	public Graph<T> copy() {
		Graph<T> copy = GraphFactory.emptyGraph();
		
		// copy vertices
		for (T v : this) copy.addVertex(v);
		
		// copy edges
		for (T v : this) {
			for (T w : getNeighborhood(v)) {
				if (v.compareTo(w) < 0) {
					copy.addEdge(v, w);
				}
			}
		} 
		
		return copy;
	}
	
	/**
	 * Simple way to print the graph as string.
	 * @return
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("p tw " + this.getVertices().size() + " " + this.getNumberOfEdges() + "\n");
		for (T v : this.getVertices()) {
			for (T w : this.getNeighborhood(v)) {
				if (v.compareTo(w) > 0) continue;
				sb.append(v + " " + w + "\n");				
			}
		}
		return sb.toString();
	}

	/**
	 * Represents the graph as string the the .gr file format of PACE.
	 * In order to do so, a bijection from V to {1,...,|V|} will be computed and vertices
	 * will be represented as integers in the output.
	 * @return
	 */
	public String toValidGraphString() {
		// compute the bijection
		int index = 1;
		Map<T, Integer> phi = new HashMap<T, Integer>();
		for (T v : this.getVertices()) {
			phi.put(v, index);
			index = index + 1;
		}
		
		// compute the string using a string builder
		StringBuilder sb = new StringBuilder();
		sb.append("p tw " + this.getVertices().size() + " " + this.getNumberOfEdges() + "\n");
		for (T v : this.getVertices()) {
			for (T w : this.getNeighborhood(v)) {
				if (v.compareTo(w) > 0) continue;
				sb.append(phi.get(v) + " " + phi.get(w) + "\n");				
			}
		}
		
		// done
		return sb.toString();
	}
	
	/**
	 * Output the graph in TikZ syntax to embed it into LaTeX documents.
	 * Compiling this code needs an actual TikZ version, LuaLaTeX, and the TikZ graphdrawing force library.
	 * @return
	 */
	public String toTikZ() {
		StringBuilder sb = new StringBuilder();
		sb.append("\\tikz\\graph[spring electrical layout] {\n");
		for (T v : this.getVertices()) sb.append(v+";");
		sb.append("\n");
		for (T v : this.getVertices()) {
			for (T w : this.getNeighborhood(v)) {
				if (v.compareTo(w) < 0) {
					sb.append(v + " -- " + w + ";\n");
				}
			}
		}
		sb.append("};\n");
		return sb.toString();
	}
	
	/**
	 * Returns an iterator over the vertices of this graph.
	 * @return vertex Iterator<T>
	 */
	@Override
	public Iterator<T> iterator() {
		return new GraphVertexIterator<T>(this);
	}
	
	/**
	 * An iterator to iterate over the vertices of a graph object.
	 * @param <Z>
	 */
	class GraphVertexIterator<Z extends Comparable<Z>> implements Iterator<Z> {

		private final Iterator<Z> itr;
		
		GraphVertexIterator(Graph<Z> G) {
			itr = G.getVertices().iterator();
		}
		
		@Override
		public boolean hasNext() {
			return itr.hasNext();
		}

		@Override
		public Z next() {
			return itr.next();
		}		
	}

	/**
	 * Compute a maximum clique of the graph using a SAT-oracle.
	 * If the sat-solver fails, this method returns null.
	 * @return
	 */
	public Set<T> getMaximumClique() {
		//TODO: implement
		return null;
	}
	
	/**
	 * Returns the number of edges the elimination operation on v will introduce to the graph.
	 * This method works in O(1) as the data is computed during graph modification.
	 * @param v
	 * @return
	 */
	public int getFillInValue(T v) {
		int delta = getNeighborhood(v).size();
		return (delta*delta-delta)/2 - edgesInNeighborhood.get(v);
	}
	
	/**
	 * Returns an arbitrary simplicial vertex of the graph.
	 * @return
	 */
	public T getSimplicialVertex(Set<T> forbidden) {
		for (T v : this) {
			if (forbidden.contains(v)) continue;
			int delta = getNeighborhood(v).size();
			if (edgesInNeighborhood.get(v) == (delta*delta-delta)/2) {
				return v;
			}
			
		}
		return null;
	}

	/**
	 * Returns an arbitrary almost simplicial vertex of the graph.
	 * @return
	 */
	public T getAlmostSimplicialVertex(Set<T> forbidden) {
		
		search: for (T v : this) {
			if (forbidden.contains(v)) continue;
			Set<T> incidentVertices = null;
			for (T x : getNeighborhood(v)) {
				for (T y : getNeighborhood(v)) {
					if (x.compareTo(y) >= 0) continue;
					if (isAdjacent(x, y)) continue;
					if (incidentVertices == null) {
						incidentVertices = new HashSet<>();
						incidentVertices.add(x);
						incidentVertices.add(y);
					} else {
						Set<T> E = new HashSet<>();
						E.add(x);
						E.add(y);
						incidentVertices.retainAll(E);
						if (incidentVertices.size() == 0) continue search;
					}
				}	
			}
			
			if (incidentVertices != null && incidentVertices.size() == 1) return v;
		}
		
		return null;
	}
	
	/**
	 * Compute all twins of the graph in time O(n+m) using the partition refinement paradigm.
	 * Two vertices v, w are true twins if N[v]=N[w], and they are false twins if N(v)=N(w).
	 * 
	 * This method can be used to compute both kinds of twins, regulated with the given boolean parameter.
	 * 
	 */
	public Map<T, Set<T>> getTwinDecomposition(boolean trueTwins) {
		PartitionRefinement<T> P = new PartitionRefinement<>(getVertices());
		for (T v : this) {
			Set<T> Nv = new HashSet<T>(getNeighborhood(v)); 
			if (trueTwins) Nv.add(v);
			P.refine(Nv);
		}
		return P.getPartition();
	}
	
	/**
	 * Compute a twin decomposition of the graph, that is it assigns each vertex to a group
	 * of vertices with the same opened or closed neighborhood (what ever is bigger).
	 * 
	 * This method invokes two times @see getTwinDecomposition(boolean trueTwins) and uses for
	 * each vertex the bigger set.
	 * 
	 * @return
	 */
	public Map<T, Set<T>> getTwinDecomposition() {
		Map<T, Set<T>> trueTwins = getTwinDecomposition(true);
		Map<T, Set<T>> falseTwins = getTwinDecomposition(false);
		Map<T, Set<T>> twins = new HashMap<T, Set<T>>();
		for (T v : this) {
			if (trueTwins.get(v).size() > falseTwins.get(v).size()) {
				twins.put(v, trueTwins.get(v));
			} else {
				twins.put(v, falseTwins.get(v));
			}
		}
		return twins;
	}
	
	/**
	 * Computes a minimum separating vertex set, i.e. a vertex set of minimal size such 
	 * that its removal splits the graph into at least two connected components.
	 * 
	 * This method uses the @see Dinic algorithm and should only be called upon small graphs, 
	 * as its running time is roughly n^3 * m.
	 * 
	 * 
	 * @return a minimal separating vertex set
	 */
	public Set<T> getMinimalSeparator(){
		int n = getVertices().size();
		// we need a mapping between the nodes of the graph and their indices
		HashMap<T, Integer> mapTI = new HashMap<>();
		HashMap<Integer,T> mapIT = new HashMap<>();
		
		int c = 0;
		for(T v: getVertices()){
			mapTI.put(v, c);
			mapIT.put(c, v);
			c++;
		}
		
		// create the temporary graph. For every original node i, 
		// it contains and input node i and an output node n+i
		int[][] g = new int[2*n][2*n];
		for(T u: getVertices()){
			int mu = mapTI.get(u);
			// the capacities between and input node and its corresponding output node is 1
			g[mu][mu+n] = 1;
			for(T v: getVertices()){
				if(isAdjacent(u,v)){
					//two adjacent vertices have unbounded edge capacity
					int mv = mapTI.get(v);
					g[mu+n][mv] = Integer.MAX_VALUE;
					g[mv+n][mu] = Integer.MAX_VALUE;
				}
			}
		}
		
		// the first separator consists of all nodes
		HashSet<T> sep = new HashSet<>();
		for(T u: getVertices()){
			sep.add(u);
		}
		// for every node i, compute a maximum flow that separates i from any other node j
		int i = 0;
			for(int j = i+1; j < n; j++){
				if(!isAdjacent(mapIT.get(i),mapIT.get(j))){
				boolean[][] res = new Dinic(g, i, j+n).start();
				
				// construct the minimimal i-j-separator
				HashSet<T> cand = new HashSet<>();
				for(int l = 0; l < n; l++){
					if(l != i && l != j){
					if(res[l][l+n] ){
						cand.add(mapIT.get(l));
					}
					}
				}
				if(cand.size() < sep.size()){
					sep = cand;
				}	
				}
			}
		return sep;	
	}

	/**
	 * Compute the connected components of the graph using a DFS.
	 * The components are represented as (Hash)-Sets of vertices, which are stored in a list.
	 * @return
	 */
	public List<Set<T>> getConnectedComponents() {
		List<Set<T>> connectedComponents = new LinkedList<>();
		
		Set<T> marked = new HashSet<>();
		for (T v : this) {
			if (marked.contains(v)) continue;
			
			/* found new connected component */
			Set<T> component = new HashSet<>();
			component.add(v);
			marked.add(v);
			
			/* Explore the component using DFS */
			Stack<T> S = new Stack<>();
			S.push(v);
			while (!S.isEmpty()) {
				T s = S.pop();
				for (T t : getNeighborhood(s)) {
					if (!marked.contains(t)) {
						marked.add(t);
						component.add(t);
						S.push(t);
					}
				}
			}
			
			/* Store the component */
			connectedComponents.add(component);
		}
		
		// done
		return connectedComponents;
	}
	
	/**
	 * Tests whether the graph is a clique
	 * @return whether the graph is a clique
	 */
	public boolean isClique(){
		boolean res = true;
		for(T v: getVertices()){
			for(T u: getVertices()){
				if(u != v && !isAdjacent(u,v)){
					res = false;
				}
			}
		}
		return res;
		
	}
	
	
}
