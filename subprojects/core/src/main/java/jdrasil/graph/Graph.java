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
import java.util.logging.Logger;

import javax.management.RuntimeErrorException;

import jdrasil.utilities.logging.JdrasilLogger;

/**
 * This class represents a generic directed graph and provides basic methods to modify it.
 * As Jdrasil mainly deals with undirected graphs, this class provides method to perform all operations symmetrically,
 * and, thus, can be used as representation for undirected graphs.
 * 
 * The graph is stored as adjacency list, making edge iterations effective but adjacency tests expensive.
 * In addition, a hashmap provides fast access to adjacency tests.
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
	
	/** Jdrasils Logger */
	private final static Logger LOG = Logger.getLogger(JdrasilLogger.getName());
	
	private static final long serialVersionUID = -6506030235954373541L;
	
	/**
	 * The graph is stored as adjacency list, i.e., for every vertex of the graph
	 * there is one entry in the map that points to a list of neighbors.
	 */
//	private Map<T, List<T>> adjacencyList;
	
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
	
	/** Enable or disable logging of the number of edges in each vertices' neighbourhood */
	
	private boolean logEdgesInNeighbourhood;
	
	/**
	 * Package private constructor, only initialize data structures.
	 */
	Graph() {
//		adjacencyList = new HashMap<>();	
		adjacencies = new HashMap<>();
		edgesInNeighborhood = new HashMap<>();
		setLogEdgesInNeighbourhood(true);
	}
	
	public Graph(Graph<T> original){
//		adjacencyList = new HashMap<>();
		setLogEdgesInNeighbourhood(original.isLogEdgesInNeighbourhood());
//		for(T v : original.adjacencyList.keySet()){
//			adjacencyList.put(v,  new ArrayList<>());
//			adjacencyList.get(v).addAll(original.adjacencyList.get(v));
//		}
		
		adjacencies = new HashMap<>();
		for(T v : original.adjacencies.keySet()){
			adjacencies.put(v, new HashSet<>());
			adjacencies.get(v).addAll(original.adjacencies.get(v));
		}
		edgesInNeighborhood = new HashMap<>();
		for(T v : original.edgesInNeighborhood.keySet()){
			edgesInNeighborhood.put(v,  original.edgesInNeighborhood.get(v));
		}
		m = original.m;
	}
	
	/**
	 * Returns the vertices set of this graph. This is actually a Java set of
	 * the generic vertex type.
	 * @return Set the vertices of the represented graph
	 */
	public Set<T> getCopyOfVertices() {
		return new HashSet<T>(adjacencies.keySet());
	}
	
	public boolean containsNode(T v){
		return adjacencies.containsKey(v);
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
	 * @return List the neighborhood of v
	 */
	public Set<T> getNeighborhood(T v) {
		return adjacencies.get(v);
	}
	
	
	public List<T> getNeighbourhoodAsList(T v){
		return new ArrayList(adjacencies.get(v));
	}
	/**
	 * Check if two vertices are adjacent
	 * @param u first vertex
	 * @param v second vertex
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
		if (adjacencies.get(v) == null) {
//			adjacencyList.put(v, new ArrayList<T>());
			adjacencies.put(v, new HashSet<>());
			edgesInNeighborhood.put(v, 0);
		}
	}
	
	/**
	 * Adds an directed edge (u,v) to the graph by adding the vertices to the neighborhood of the corresponding other vertex.
	 * If u or v is not in the graph they will automatically be added.
	 * 
	 * @param u - the startpoint of the edge
	 * @param v - the endpoint of the edge
	 */
	public void addDirectedEdge(T u, T v) {
		addVertex(u);
		addVertex(v);
		if (adjacencies.get(u).contains(v)) return;
		m++;
//		adjacencyList.get(u).add(v);
		adjacencies.get(u).add(v);		
	}
	
	/**
	 * Adds an undirected edge {u,v} to the graph by adding the vertices to the neighborhood of the corresponding other vertex.
	 * If u or v is not in the graph they will automatically be added.
	 * 
	 * This method costs \(O(\delta(u))\), as it updates the simplicial properties
	 * 
	 * @param u - an endpoint of the edge
	 * @param v - an endpoint of the edge
	 */
	public void addEdge(T u, T v) {
		
		// add symetric edge
		addDirectedEdge(u, v);
		addDirectedEdge(v, u);
		
		// update number of neighbor edges value
		if(logEdgesInNeighbourhood){
			for (T x : getNeighborhood(u)) {
				if (isAdjacent(x, v)) {
					edgesInNeighborhood.put(x, edgesInNeighborhood.get(x)+1);
					edgesInNeighborhood.put(u, edgesInNeighborhood.get(u)+1);
					edgesInNeighborhood.put(v, edgesInNeighborhood.get(v)+1);
				}
			}
		}
	}
	
	/**
	 * Remove a given directed edge from the graph.
	 * This method costs O(1), as operations are performed with hashing.
	 * @param u
	 * @param v
	 */
	public void removeDirectedEdge(T u, T v) {
		m--;
//		adjacencyList.get(u).remove(v);
		adjacencies.get(u).remove(v);
	}
	
	/**
	 * Remove a given edge from the graph.
	 * @param u
	 * @param v
	 */
	public void removeEdge(T u, T v) {
		
		// remove symmetric edge
		removeDirectedEdge(u, v);
		removeDirectedEdge(v, u);
		
		// update number of neighbor edges value
		if(logEdgesInNeighbourhood){
			for (T x : getNeighborhood(u)) {
				if (isAdjacent(x, v)) {
					edgesInNeighborhood.put(x, edgesInNeighborhood.get(x)-1);
					edgesInNeighborhood.put(u, edgesInNeighborhood.get(u)-1);
					edgesInNeighborhood.put(v, edgesInNeighborhood.get(v)-1);
				}
			}
		}
	}
	
	/**
	 * Returns the number of directed edges present in the graph.
	 * @return
	 */
	public int getNumberOfDirectedEdges() {
		return m;
	}
	
	/**
	 * Returns the number of (undirected) edges present in the graph.
	 * Assuming the edge relation is symmetric.
	 * @return
	 */
	public int getNumberOfEdges() {
		return m/2;
	}
	
	/**
	 * Remove a given vertex from the graph.
	 * @param v
	 */
	public void removeVertex(T v) {
		
		// remove the edges from other vertices
		for (T u : new ArrayList<>(getNeighborhood(v))) {
			removeEdge(u, v);
		}
				
		// actually remove the vertex
//		adjacencyList.remove(v);
		adjacencies.remove(v);
		edgesInNeighborhood.remove(v);
	}
	
	/**
	 * This method performs an undirected edge contraction on the graph contracting the undirected edge {v, w}.
	 * This will identify w as v and map all edges of w to v, afterwards w is removed.
	 * If v and w have a common neighbor, resulting multi edges are removed, i.e., the result
	 * is a simple graph again.
	 * 
	 * This returns an information object that can be used to decontract the edge later.
	 * 
	 * @param v
	 * @param w
	 */
	public ContractionInformation contract(T v, T w) {
		
		ContractionInformation info = new ContractionInformation(v, w);
		
		// add the neighbors of w to N(v)
		for (T u : getNeighborhood(w)) {
			info.addEdges.add(u);
			if (!u.equals(v) && !isAdjacent(u, v)) {
				addEdge(u, v);
				info.removeEdges.add(u);
			}
		}
		
		// delete w
		removeVertex(w);
		
		// done
		return info;
	}
	
	/**
	 * Revert an edge contraction.
	 * @param info
	 */
	public void deContract(ContractionInformation info) {
		addVertex(info.w);
		for (T u : info.addEdges) addEdge(u, info.w);
		for (T u : info.removeEdges) removeEdge(u, info.v);
	}
	
	/**
	 * Container class to store informations about an edge contraction.
	 */
	public class ContractionInformation {
		
		public T v; // vertex to which we contracted
		public T w; // vertex removed during contraction
		Set<T> removeEdges; // edges that have to be removed from v
		Set<T> addEdges; // edges that have to be added to w
		
		public ContractionInformation(T v, T w) {
			this.setV(v);
			this.w = w;
			this.removeEdges = new HashSet<>();
			this.addEdges = new HashSet<>();
		}

		/**
		 * @param v the v to set
		 */
		public void setV(T v) {
			this.v = v;
		}
		
	} 
	
	/**
	 * This methods performs the vertex elimination operation on a given vertex v.
	 * Eliminating a vertex means: (a) making its neighborhood a clique and (b) deleting the vertex from the graph.
	 * @param v
	 */
	public EliminationInformation eliminateVertex(T v) {

		EliminationInformation info = new EliminationInformation(v);
		if(!logEdgesInNeighbourhood)
			throw new RuntimeException("bla");
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
		removeVertex(v);
		
		return info;
	}
	private int getSizeOfIntersection(Set<T> s1, Set<T> s2){
		if(s1.size() > s2.size())
			return getSizeOfIntersection(s2, s1);
		int ret = 0;
		for(T v : s1)
			if(s2.contains(v))
				ret++;
		return ret;
	}
	
	private int getSizeOfIntersectionIfLarger(Set<T> s1, Set<T> s2, T threshold){
		if(s1.size() > s2.size())
			return getSizeOfIntersectionIfLarger(s2, s1, threshold);
		int ret = 0;
		for(T v : s1){
			if(v.compareTo(threshold) > 0 && s2.contains(v))
				ret++;
		}
		return ret;
	}
	
	private Set<T> intersect(Set<T> s1, Set<T> s2){
		if(s1.size() > s2.size())
			return intersect(s2, s1);
		Set<T> ret = new HashSet<>();
		for(T v : s1)
			if(s2.contains(v))
				ret.add(v);
		return ret;
	}
	
	/**
	 * Get nodes which have distance 1 and 2, respectively, from node v
	 * TODO: Make this faster :) 
	 * @param v
	 * @param dist1
	 * @param dist2
	 */
	private void getNodes(T v, Set<T> dist1, Set<T> dist2){
		dist1.addAll(getNeighborhood(v));
		for(T u : getNeighborhood(v))
			for(T w : getNeighborhood(u))
				if(w.compareTo(v) != 0 && !dist1.contains(w))
					dist2.add(w);
		
	}
	
	
	private void checkFillValues(){
		for(T v : getCopyOfVertices()){
			int tmp = 0;
			for(T u1 : getNeighborhood(v))
				for(T u2 : getNeighborhood(v))
					if(u1.compareTo(u2) < 0 && isAdjacent(u1, u2))
						tmp++;
			if(edgesInNeighborhood.get(v) != tmp)
				throw new RuntimeException("Edges in neighbourhood were not counted correctly! ");
		}
	}
	public EliminationInformation eliminateVertex(T v, boolean updateFillValues){
		boolean runOldWay = true;
		if(runOldWay){
			setLogEdgesInNeighbourhood(true);
			return eliminateVertex(v);
		}
		
		
		setLogEdgesInNeighbourhood(true);
		checkFillValues();
		if(!updateFillValues)
			return eliminateVertex(v);
		Map<T, Integer> predicedValues = new HashMap<>();
		boolean debugThis = true;
		if(getFillInValue(v) == 0){
			setLogEdgesInNeighbourhood(false);
			Set<T> neighbourhood = getNeighborhood(v);
			for(T u : neighbourhood)
				edgesInNeighborhood.put(u, edgesInNeighborhood.get(u)-(neighbourhood.size()-1));	
			EliminationInformation ret = eliminateVertex(v);
			checkFillValues();
			return ret;
		}
		else{
			logEdgesInNeighbourhood = true;
			// Divide the set of neighbours of "v" and "u" in three disjoint sets: 
			// N_a contains nodes that are neighbours of v, bot not u (Additional neighbours: Will become neighbours when eliminating v)
			// N_e contains the neighbours of both u and v (Existing neighbours)
			// N_u contains the neighbours of u which are not neighbours of v (Unaffected)
			Set<T> dist1 = new HashSet<>();
			Set<T> dist2 = new HashSet<>();
			getNodes(v, dist1, dist2);
			Map<T, Set<T> > N_u = new HashMap<>();
			Map<T, Set<T> > N_e = new HashMap<>();
			Map<T, Set<T> > N_a = new HashMap<>();
			for(T u : getNeighborhood(v)){
				N_u.put(u, new HashSet<>());
				N_e.put(u, new HashSet<>());
				N_a.put(u, new HashSet<>());
			}
			for(T u : getNeighborhood(v)){
				for(T w : getNeighborhood(u)){
					
					if(getNeighborhood(v).contains(w)) // w is both a neighbour of v and u (Existing)
						N_e.get(u).add(w);
					else if(w.compareTo(v) != 0)
						N_u.get(u).add(w);				// W is a neighbour of u, but not w (Unaffected)
						
				}
				for(T u2 : getNeighborhood(v)){			// Other neighbours of v: If there's no edge to u, they are additional neighbours
					if(u2.compareTo(u) != 0 && !getNeighborhood(u).contains(u2))
						N_a.get(u).add(u2);
				}
			}
			/**********************
			 * Some debugging
			 */
			if(debugThis){
				// Check that everything is correct!!!
				for(T u : getNeighborhood(v)){
					// Nodes from N_u are not connected to v
					for(T w : N_u.get(u)){
						if(isAdjacent(v, w) || !isAdjacent(u, w))
							throw new RuntimeException("N_u looks weird! ");
					}
					if(N_u.get(u).contains(v) || N_a.get(u).contains(v) || N_e.get(u).contains(v))
						throw new RuntimeException("V occured in set! ");
					// The set of neighbours is {v} + N_u + N_e
					for(T w : getNeighborhood(u))
						if(w.compareTo(v) != 0){
							int s = 0;
							if(N_u.get(u).contains(w)) s++;
							if(N_a.get(u).contains(w)) s++;
							if(N_e.get(u).contains(w)) s++;
							if(s != 1)
								throw new RuntimeException();
						}
//					StringBuilder sb = new StringBuilder();
//					sb.append("Node " + u + ": ");
//					sb.append("N_u=" ); for(T w : N_u.get(u)) sb.append(w + " ");
//					sb.append("N_e: "); for(T w : N_e.get(u)) sb.append(w + " ");
//					sb.append("N_a: "); for(T w : N_a.get(u)) sb.append(w + " ");
//					LOG.info(sb.toString());
				}
				
				
			}
			/**
			 * Actually update the fill-values. Distinguish 3 cases: 
			 */
			for(T u : getNeighborhood(v)){
				if(N_u.get(u).size() > 0){
					if(N_a.get(u).size() > 0){
						/**
						 * N_u and N_a are non-empty. 
						 */
						// Count the number of edges between nodes in N_a and N_u. All of them will appear newly in the neighbourhood of u!
//						LOG.info("Node " + u + ": degree " + getNeighborhood(u).size() + "old fill: " + getFillInValue(u) + " old number n-edges: " + edgesInNeighborhood.get(u));
						int newNumberEdges = edgesInNeighborhood.get(u);
						newNumberEdges -= N_a.get(u).size(); 	// For each node in N_a, one edge will be deleted --- the one to v
						int newFill = getFillInValue(u) + (N_a.get(u).size()-1)*N_u.get(u).size();
//						LOG.info("Increasing by (" + N_a.get(u).size() + "-1)*" + N_u.get(u).size());
						for(T w : N_a.get(u)){
							newFill -= getSizeOfIntersection(N_u.get(u), N_u.get(w));
						}
//						LOG.info("After subtraction the intersections: " + newFill);
						// Are there edges between nodes in N_e? They have to be considered as well! 
						if(N_e.get(u).size() > 1){
							int missing = ((N_e.get(u).size()-1) * N_e.get(u).size())/2;
							ArrayList<T> nodeList = new ArrayList<>();
							nodeList.addAll(N_e.get(u));
							for(int i = 0 ; i < nodeList.size();i++){
								for(int j = i+1 ; j < nodeList.size();j++)
									if(isAdjacent(nodeList.get(i), nodeList.get(j)))
										missing--;
							}
//							LOG.info("Decreasing fill value by " + missing);
							newFill -= missing;
						}
						// Now store the new fill-value: 
						int n = N_a.get(u).size() + N_u.get(u).size() + N_e.get(u).size();
						n = (n * (n-1))/2;
//						LOG.info("Now have a new fill value of " + newFill + ", changing number edges from " + edgesInNeighborhood.get(u) + " to " + (n-newFill));
						predicedValues.put(u, n-newFill);
					}
					else{
						/**
						 * N_a is empty. 
						 * This is, u will not get any new neighbours. 
						 */
						// N_u non-empty, but N_a is. 
						int newFill = getFillInValue(u) - N_u.get(u).size(); // Fill-value is decreased by the number of non-missing edges between v and nodes from N_u
						for(T w : N_e.get(u)){
							newFill -= getSizeOfIntersectionIfLarger(N_e.get(u), N_a.get(w), w);
						}
						int n = N_a.get(u).size() + N_u.get(u).size() + N_e.get(u).size();
						n = (n * (n-1))/2;
						predicedValues.put(u, n-newFill);
					}
				}
				else{
					/**
					 * There are no unaffected neighbours. This is, after eliminating v, the neighbours of u will form a clique, thus, the new fill value will equal zero! 
					 */
					int n = N_e.get(u).size() + N_a.get(u).size();
					int newNumberOfEdges = (n * (n-1))/2;
					predicedValues.put(u, newNumberOfEdges);
				}
			}
			// Now nodes with distance 2: 
			for(T u : dist2){
				Set<T> commonNeighbours = intersect(getNeighborhood(v), getNeighborhood(u));
				// These neighbours will become part of a clique. Thus, every missing edge between them will added, and thus reduce the fill-value accordingly. 
				int fillDecrease = 0;
				for(T u1 : commonNeighbours)
					for(T u2 : commonNeighbours)
						if(u1.compareTo(u2) < 0)
							if(!isAdjacent(u1, u2))
								fillDecrease++;
				predicedValues.put(u, edgesInNeighborhood.get(u) + fillDecrease);
			}
			int numEdgesBefore = getNumberOfEdges();
			int predictedFill = getFillInValue(v);
			int deg = getNeighborhood(v).size();
			EliminationInformation ret = eliminateVertex(v);
			if(getNumberOfEdges() != numEdgesBefore + predictedFill - deg)
				throw new RuntimeException("Something was wrong. Eliminated the node, but the number of added edges was different from what it should be like! ");
			// Do some debug stuff
			
			if(debugThis){
				if(containsNode(v))
					throw new RuntimeException("v was not eliminated! ");
//				LOG.info("Checking " + dist2.size() + " nodes with distance 2: ");
				for(T u : dist2){
					int tmp = 0;
					
					for(T x1 : getNeighborhood(u)){
						for(T x2 : getNeighborhood(u)){
							if(x1.compareTo(x2) < 0 && isAdjacent(x1, x2))
								tmp++;
						}
					}
					if(tmp != edgesInNeighborhood.get(u) ){
						
						throw new RuntimeException("Miss-predicted fill value: Dist 2,  Predicted " + edgesInNeighborhood.get(u) + ", was " + tmp);
					}
				}
//				LOG.info("Checking " + dist1.size() + " nodes with distance 1: ");
				for(T u : dist1){
					// Count number of edges in neighbourhood: 
					int tmp = 0;
					for(T x1 : getNeighborhood(u)){
						for(T x2 : getNeighborhood(u)){
							if(x1.compareTo(x2) < 0 && isAdjacent(x1, x2))
								tmp++;
						}
					}
					if((tmp != edgesInNeighborhood.get(u).intValue()) || (predicedValues.get(u) != edgesInNeighborhood.get(u).intValue())){
						
						LOG.info("This is weird. ");
						LOG.info("I counted " + tmp + " edges in the neighbourhood, predicted value was " + predicedValues.get(u) + ", graph says " + edgesInNeighborhood.get(u));
						LOG.info("" + ((tmp != edgesInNeighborhood.get(u)) || (predicedValues.get(u) != edgesInNeighborhood.get(u))));
						LOG.info("" + ( (predicedValues.get(u) != edgesInNeighborhood.get(u))));
						LOG.info("" + ((tmp != edgesInNeighborhood.get(u)) ));
						LOG.info("Miss-predicted fill value for node " + u + ": Dist 1, N_u.size()=" + N_u.get(u).size()  + ", N_e.size()=" + N_e.get(u).size() + ", and N_a.size()=" + N_a.get(u).size());
						LOG.info("Predicted " + predicedValues.get(u) + ", now is " + getFillInValue(u));
						LOG.info("New degree is " + getNeighborhood(u).size());
						StringBuilder nbSb = new StringBuilder("Neighbours are");
						for(T w : getNeighborhood(u))
							nbSb.append(w + " ");
						LOG.info(nbSb.toString());
						checkFillValues();
						throw new RuntimeException("\n Predicted " + predicedValues.get(u) + ", was " + tmp + "\n. FillValue=" + getFillInValue(u));
					}
//					else
//						LOG.info("Everything was fine: N_u.size()=" + N_u.get(u).size()  + ", N_e.size()=" + N_e.get(u).size() + ", and N_a.size()=" + N_a.get(u).size() );
				}
			}
			return ret;
		}
		
//		return eliminateVertex(v);
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
	 * Simple way to print the graph as string.
	 * @return
	 */
	@Override
	public String toString() {
		return GraphWriter.graphToString(this);
	}
	
	/**
	 * Returns an iterator over the vertices of this graph.
	 * @return vertex Iterator
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
			itr = G.getCopyOfVertices().iterator();
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
		for(T v: getCopyOfVertices()){
			for(T u: getCopyOfVertices()){
				if(u != v && !isAdjacent(u,v)){
					res = false;
				}
			}
		}
		return res;
	}
	public int getNumVertices(){
		return adjacencies.keySet().size();
	}

	public boolean isLogEdgesInNeighbourhood() {
		return logEdgesInNeighbourhood;
	}

	public void setLogEdgesInNeighbourhood(boolean logEdgesInNeighbourhood) {
		this.logEdgesInNeighbourhood = logEdgesInNeighbourhood;
	}
	
}
