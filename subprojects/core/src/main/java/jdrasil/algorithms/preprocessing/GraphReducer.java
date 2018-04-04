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
package jdrasil.algorithms.preprocessing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;
import java.util.logging.Logger;


import jdrasil.graph.Bag;
import jdrasil.graph.Graph;
import jdrasil.graph.GraphFactory;
import jdrasil.graph.TreeDecomposition;
import jdrasil.utilities.logging.JdrasilLogger;

/**
 * There are many reduction rules for tree width known in the
 * literature. A reduction rule thereby is a function that removes (in
 * polynomial time) a vertex from the graph and creates a bag of the
 * tree decomposition such that this bag glued to an optimal
 * tree decomposition of the remaining graph yields to an optimal
 * tree decomposition. For graphs of tree with at most 3, these rules
 * produce an optimal decomposition in polynomial time.  For graphs with
 * higher tree width, the rules can only be applied up to a certain
 * point. From this point on, an other algorithm has to be used.

 * This class provides methods to reduce a graph, and to obtain a optimal
 * tree-decomposition of the full graph, if it is provides with an
 * optimal decomposition of the reduced graph.
 *
 * @author Max Bannach
 * @author Thorsten Ehlers
 */
public class GraphReducer<T extends Comparable<T>> extends Preprocessor<T> {

	/** Jdrasils Logger */
	private final static Logger LOG = Logger.getLogger(JdrasilLogger.getName());

	/** The exhaustive application of reduction rules may be to expensive, in this case we use only a subset of them. */
	private final int EXHAUSTIVE_THRESHOLD = 2000;

	/** Bags that are created during the reduction (and which have to be glued to a later decomposition).*/
	private Stack<Set<T>> bags;
	
	/** A lower bound on the tree-width. */
	private int low;
	
	/**
	 * @param graph
	 */
	public GraphReducer(Graph<T> graph) {
		super(graph);
	}

	//MARK: Preprocessor interface
	
	/* (non-Javadoc)
	 * @see jdrasil.algorithms.preprocessing.Preprocessor#computeGraphs()
	 */
	@Override
	protected Graph<T> preprocessGraph() {

		// init data structures
		this.bags = new Stack<>();
		this.low = 0;
		this.treeDecomposition = new TreeDecomposition<>(graph);

		// the reduced graph we will produce
		Graph<T> reduced = GraphFactory.copy(graph);

		// input was empty graph – nothing to do
		if (reduced.getCopyOfVertices().size() == 0) { glueBags(); return GraphFactory.emptyGraph(); };
		
		// handle easy cases: vertices of degree 0 and 1
		LOG.info("Removing unconnected nodes");
		eliminateLowDegreeNodes(reduced, 0);
		LOG.info("Removing leafs");
		eliminateLowDegreeNodes(reduced, 1);
		
		// input was a tree
		if (reduced.getCopyOfVertices().size() == 0) { glueBags(); return GraphFactory.emptyGraph(); };
		
		// no tree, eliminate degree 2 vertices
		LOG.info("Removing nodes with degree <= 2");
		eliminateLowDegreeNodes(reduced, 2);
		if (reduced.getCopyOfVertices().size() == 0) { glueBags(); return GraphFactory.emptyGraph(); };
		
		LOG.info("Running single pass trick...");
		for(int i = 0 ; i < 20 && singlePass(reduced) ; i++) {}

		// apply classic reduction rules until exhaustion
		LOG.info("Applying other rules...");
		boolean fixPointReached = reduced.getNumVertices() > EXHAUSTIVE_THRESHOLD;
		while(!fixPointReached){
			Set<T> bag = null;
			bag = isolatedVertexRule(reduced);
			if (bag == null) bag = leafVertexRule(reduced);
			if (bag == null) bag = seriesRule(reduced);
			if (bag == null) bag = triangleRule(reduced);
			if (bag == null) bag = buddyRule(reduced);
			if (bag == null) bag = cubeRule(reduced);
			if (bag == null) bag = simplicialRule(reduced);
			if (bag == null) bag = almostSimplicialRule(reduced);
			if (bag == null)
				fixPointReached = true;
			else{
				bags.push(bag);
			}
		}
		if (reduced.getCopyOfVertices().size() == 0) { glueBags(); return GraphFactory.emptyGraph(); }
		
		// done
		return reduced;
	}

	/* (non-Javadoc)
	 * @see jdrasil.algorithms.preprocessing.Preprocessor#glueDecompositions()
	 */
	@Override
	protected TreeDecomposition<T> computeTreeDecomposition() {

		// we have only one decomposition, use it
		this.treeDecomposition = processedTreeDecomposition;
		this.treeDecomposition.setGraph(graph);

		// add the reduced bags
		glueBags();
		
		// done
		return this.treeDecomposition;
	}

	//MARK: reduction methods
	
	/**
	 * Eliminate nodes with low degree. 
	 * Here, called with maxDegree <= 2 - so this implements the rules "Twig rule", "Series rule" and easy cases of the "Simplicial" rule
	 */
	private void eliminateLowDegreeNodes(Graph<T> work, int maxDegree) {
		Set<T> seen = new HashSet<>();
		Queue<T> q = new LinkedList<>();
		for(T v : work){
			if(work.getNeighborhood(v).size() <= maxDegree){
				q.add(v);
				seen.add(v);
			}
		}
		while(!q.isEmpty()){
			T next = q.poll();
			List<T> N = new ArrayList<T>();
			/* Just to make sure I don't get into trouble with some side effects*/
			N.addAll(work.getNeighborhood(next));
			N.remove(next); // in case of self-loops
			if(N.size() <= maxDegree){
				Set<T> newBag = new HashSet<>();
				newBag.add(next);
				newBag.addAll(N);
				bags.push(newBag);
				work.eliminateVertex(next);
				for(T v : N){
					if(work.containsNode(v) && work.getNeighborhood(v).size() <= maxDegree && !seen.contains(v)){
						q.add(v);
						seen.add(v);
					}
				}
			}
		}
	}

	/**
	 * Eliminate several nodes in one run.
	 * This combines partial applications of several rules:
	 * 	- we eliminate all nodes with fill value of at most 1 - these are either simplicial, or almost simplicial
	 * 	- we eliminate nodes with degree <=3, and fill-in value of <= 2 - this is the triangle rule.
	 * Before this, make sure that no nodes with degree <= 2 have not been eliminated yet.
	 * @param work
	 * @return Whether at least one node has been eliminated or not.
	 */
	private boolean singlePass(Graph<T> work){
		int numApplications = 0;
		boolean ret = false;
		Queue<T> q = new LinkedList<>();
		Set<T> onQueue = new HashSet<>();
		for (T v : work) {
			if (work.getNeighborhood(v).size() <= 3 || work.getFillInValue(v) <= 1) {
				q.add(v);
				onQueue.add(v);
			}
		}
		while(!q.isEmpty()){
			T v = q.poll();
			if (!work.containsNode(v)) continue;
			int fillValue = work.getFillInValue(v);
			onQueue.remove(v);
			boolean eliminate = fillValue < 1 || (fillValue == 1 && work.getNeighborhood(v).size() < low);
			if (!eliminate) {
				// Check if triangle rule is applicable: In this case, the node has a degree of 3, and fillIn-value at most 2, as one of the edges between its neighbours exists
				eliminate = work.getNeighborhood(v).size() == 3 && fillValue <= 2 && low >= 3;
			}
			if (eliminate) {
				numApplications++;
				ret = true;
				for(T n : work.getNeighborhood(v)){
					if(!onQueue.contains(n)){
						onQueue.add(n);
						q.add(n);
					}
				}

				Set<T> newBag = new HashSet<>();
				newBag.add(v);
				newBag.addAll(work.getNeighborhood(v));
				bags.push(newBag);
				if (fillValue < 1) low = Math.max(low, newBag.size()-1);
				work.eliminateVertex(v);
			}
		}
		LOG.info("Ran single-pass, eliminated " + numApplications + " nodes! ");
		return ret;
	}

	/**
	 * If the graph contains an isolated vertex v, create a bag {v} and remove the vertex.
	 * This method returns the created Bag, or null if the rule can not be applied.
	 * @return
	 */
	private Set<T> isolatedVertexRule(Graph<T> work) {
		for (T v : work) {
			if (work.getNeighborhood(v).size() == 0) {
				Set<T> set = new HashSet<>();
				set.add(v);
				work.removeVertex(v);
				low = Math.max(low, 1);
				return set;
			}
		}
		return null;
	}
	
	/**
	 * If the graph contains an leaf vertex v, create a bag {v,w} where w is the neighbor of v, and remove the vertex v.
	 * This method returns the created Bag, or null if the rule can not be applied.
	 * @return
	 */
	private Set<T> leafVertexRule(Graph<T> work) {
		for (T v : work) {
			if (work.getNeighborhood(v).size() == 1) {
				Set<T> set = new HashSet<>();
				set.add(v);
				set.addAll(work.getNeighborhood(v));
				work.removeVertex(v);
				low = Math.max(low, 2);
				return set;
			}
		}
		return null;
	}
	
	/**
	 * If the graph contains a vertex v with deg(v) = 2 and neighbors u, w.
	 * Remove v, add edge {u,w}, and create bag {v,w,u}.
	 * 
	 * This is a special case of the almost simplical rule, but can be computed quicker and guarantees that
	 * we can use the buddy rule (has graphs with \(tw \lt 3\) will be eliminated by the rule).
	 * 
	 * Returns the bag {v,w,u} if the rule applies, otherwise it returns null.
	 * 
	 * @return
	 */
	public Set<T> seriesRule(Graph<T> work) {
		for (T v : work) {
			if (work.getNeighborhood(v).size() == 2) {
				Set<T> set = new HashSet<>();
				set.add(v);
				set.addAll(work.getNeighborhood(v));
				work.eliminateVertex(v);
				low = Math.max(low, 3);
				return set;
			}
		}
		return null;
	}
	
	/**
	 * If the graph contains a vertex v with deg(v) = 3 and N(b)={x,y,z} and there is at least on
	 * of the edges {x,y}, {x,z}, {y,z}, then remove v, create the bag {v,x,y,z}, and make x,y,z a clique.
	 * @return
	 */
	public Set<T> triangleRule(Graph<T> work) {
		low = Math.max(low, 4);
		for (T v : work) {
			if (work.getNeighborhood(v).size() != 3) continue;
			ArrayList<T> neighbours = new ArrayList<>();
			neighbours.addAll(work.getNeighborhood(v));
			T x = neighbours.get(0);
			T y = neighbours.get(1);
			T z = neighbours.get(2);
			if (work.isAdjacent(x, y) || work.isAdjacent(x, z) || work.isAdjacent(y, z)) {
				Set<T> set = new HashSet<>();
				set.add(v);
				set.addAll(work.getNeighborhood(v));
				work.eliminateVertex(v);
				return set;
			}
		}
		return null;
	}
	
	/**
	 * If the graph contains two vertices v and w such that deg(v)=deg(w)=3 and they both have the same neighbors, 
	 * say x,y,z, then create bag {v,x,y,z}, remove v, and make {x,y,z} a clique.
	 * 
	 * Returns the bag if the rule can be applied, otherwise it returns null.
	 * @return
	 */
	private Set<T> buddyRule(Graph<T> work) {
		for (T v : work) {
			if (work.getNeighborhood(v).size() != 3) continue;
			search: for (T w : work) {
				if (v.compareTo(w) >= 0) continue;
				if (work.getNeighborhood(v).contains(w)) continue;
				if (work.getNeighborhood(w).size() != 3) continue;
				for (T x : work.getNeighborhood(v))  {
					if (!work.getNeighborhood(w).contains(x)) continue search;
				}
					
				// v and w are buddies;
				Set<T> set = new HashSet<>();
				set.add(v);
				set.addAll(work.getNeighborhood(v));
				work.eliminateVertex(v);
				return set;
			}
		}
		return null;
	}
	
	/**
	 * If the graph contains a vertex v with N(v) = {x, y, z} such that deg(x)=deg(y)=deg(z)=3 and such
	 * that x,y,z have pairwise one neighbor in common (a,b,c), then create the bag {z, b, c, v} and remove z.
	 * 
	 * Returns the bag if the rule can be applied, otherwise it returns null.
	 * @return
	 */
	private Set<T> cubeRule(Graph<T> work) {
			for (T v : work) {
				if (work.getNeighborhood(v).size() != 3) continue;
				ArrayList<T> neighboursOfV = new ArrayList<>();
				neighboursOfV.addAll(work.getNeighborhood(v));
				for(T x : neighboursOfV)
					if(work.getNeighborhood(x).size() != 3)
						continue;

				List<T> N = new ArrayList<>(work.getNeighborhood(v));
				T x = N.get(0);
				if (work.getNeighborhood(x).size() != 3) continue;
				T y = N.get(1);
				if (work.getNeighborhood(y).size() != 3) continue;
				T z = N.get(2);
				if (work.getNeighborhood(z).size() != 3) continue;

				// v is center of cube with neighbors x,y,z, compute other corners a,b,c
				N = new ArrayList<>(work.getNeighborhood(x));
				T a = N.get(0);
				if (a.compareTo(v) == 0) a = N.get(2);
				T b = N.get(1);
				if (b.compareTo(v) == 0) b = N.get(2);

				if ( !(work.isAdjacent(y, a) && work.isAdjacent(z, b)) ) {
					T tmp = a;
					a = b;
					b = tmp;
				}
				if ( !(work.isAdjacent(y, a) && work.isAdjacent(z, b)) ) continue;

				T c = null;
				for (T tmp : work.getNeighborhood(y)) {
					if (tmp.compareTo(v) != 0 && work.isAdjacent(z, tmp)) c = tmp;
				}
				if (c == null) continue;

				Set<T> set = new HashSet<>();
				set.add(z);
				set.add(b);
				set.add(c);
				set.add(v);
				work.removeVertex(z);
				if (!work.isAdjacent(a, b)) work.addEdge(a, b);
				if (!work.isAdjacent(a, c)) work.addEdge(a, c);
				if (!work.isAdjacent(a, v)) work.addEdge(a, v);
				if (!work.isAdjacent(b, c)) work.addEdge(b, c);
				if (!work.isAdjacent(b, v)) work.addEdge(b, v);
				if (!work.isAdjacent(c, v)) work.addEdge(c, v);
				return set;
			}

		// done
		return null;
	}
	
	/**
	 * A simplicial vertex v is a vertex, such that N[v] is a clique.
	 * We can remove v and create a bag containing N[v].
	 * 
	 * Returns the bag if the rule can be applied, otherwise it returns null.
	 * @return
	 */
	public Set<T> simplicialRule(Graph<T> work) {
		T v = work.getSimplicialVertex(new HashSet<>());
		if (v == null) return null;
		Set<T> set = new HashSet<>();
		set.add(v);
		set.addAll(work.getNeighborhood(v));
		low = Math.max(low, set.size()-1);
		work.removeVertex(v);
		return set;
	}

	/**
	 * An almost simplicial vertex v is a vertex, such that N[v] is a clique.
	 * We can remove v and create a bag containing N[v].
	 * 
	 * Returns the bag if the rule can be applied, otherwise it returns null.
	 * @return
	 */
	public Set<T> almostSimplicialRule(Graph<T> work) {
		T v = work.getAlmostSimplicialVertex(new HashSet<>());
		if (v == null) return null;
		Set<T> set = new HashSet<>();
		set.add(v);
		set.addAll(work.getNeighborhood(v));
		if (set.size() > low) return null;
		work.eliminateVertex(v);
		return set;
	}
	
	//MARK: glue methods
	
	/**
	 * Glues the bag to the tree-decomposition in construction.
	 * @param bag
	 */
	private void glue(Set<T> bag) {
		for (Bag<T> x : treeDecomposition.getBags()) {
			int count = 0;
			for (T v : bag) {
				if (!x.contains(v)) count++;
			}
			if (count > 1) continue;
			Bag<T> y = treeDecomposition.createBag(bag);
			treeDecomposition.addTreeEdge(y, x);
			return;
		}
		
		// if there is no bag, just insert new bag
		treeDecomposition.createBag(bag);
	}
	
	/**
	 * Glues all bags that where generated during the reduction.
	 */
	private synchronized void glueBags() {
		// Don't call the alternative version - does not work if td was produced by the dynamic program? 
		if(treeDecomposition.isCreatedFromPermutation())
			glueBags_test();
		// 
		LOG.info("Calling old glueBags, bags to glue: " + bags.size());
		while (!bags.isEmpty()) {
			glue(bags.pop());
		}
	}
	
	private void glueBags_test(){
		// Get elimination order of given TD
		long tStart = System.currentTimeMillis();
		int components_created = 0;
		Stack<Set<T>> myStack = bags; //(Stack<Set<T>>) bags.clone();

		int edgesAdded = 0;
		// Okay, just undo pre-solving and glue the bags created by the preprocessor to this treedecomposition! 
		if(treeDecomposition.getNumberOfBags() == treeDecomposition.getTree().getNumberOfEdges()+1){
			if(myStack.isEmpty())
				return;
			// Okay, I will create new bags here. Adjust the indices! 
			for(Bag<T> b : treeDecomposition.getBags()){
				b.id += myStack.size();
			}
			Map<T, Bag<T>> eliminatedAt = new HashMap<>();
			int lowestId = Integer.MAX_VALUE;
			for(Bag<T> b : treeDecomposition.getBags()){
				if(b.id < lowestId)
					lowestId = b.id;
				for(T v : b.vertices){
					if(!eliminatedAt.containsKey(v) || eliminatedAt.get(v).id < b.id){
						eliminatedAt.put(v, b);
					}
				}
			}
			while(!myStack.isEmpty()){
				Set<T> s = myStack.pop();
				Bag<T> newBag = treeDecomposition.createBag(s);
				// Now this bag has somewhat been created BEFORE the permutation was computed - adjust its id accordingly! 
				newBag.id = --lowestId;
				// Get an edge from this bag to one that existed before
				Bag<T> next = null;
				int numElimHere = 0;
				for(T v : newBag.vertices){
					if(eliminatedAt.containsKey(v)){
						if(next == null || next.id > eliminatedAt.get(v).id){
							next = eliminatedAt.get(v);
						}
					}
					else{
						numElimHere++;
						eliminatedAt.put(v, newBag);
					}
				}
				if(next != null){
					treeDecomposition.addTreeEdge(newBag, next);
				}
				else{
					LOG.info("No outgoing edge here! ");
				}
				if(numElimHere > 1){
					LOG.info("Eliminated two nodes at once? ");
				}
			}
		}
		else{
			// For each node in the tree decomposition, check if it's been eliminated already
			LOG.info("This was created by a  permutation, but the tree decomposition is not actually a tree?");
			
			for(Bag<T> b : treeDecomposition.getBags())
				b.id += myStack.size();
			
			Map<T, Bag<T>> eliminatedAt = new HashMap<>();
			// Go throw the bags. For each node, look at the last bag where it appears --- this is the one at which it was eliminated! 
			for(Bag<T> b : treeDecomposition.getBags()){
				for(T v : b.vertices){
					if(!eliminatedAt.containsKey(v) || eliminatedAt.get(v).id < b.id){
						eliminatedAt.put(v, b);
					}
				}
			}
			// Make sure that the current tree decomposition is valid! 
			int edgesAddedToTreeDecomp = 0;
			for(Bag<T> b : treeDecomposition.getBags()){
				// Look for an outgoing edge
				Bag<T> nextBag = null;
				for(T v : b.vertices){
					Bag<T> bagWhereThisWasEliminated = eliminatedAt.get(v);
					if(bagWhereThisWasEliminated.id != b.id){
						if(nextBag == null || nextBag.id > bagWhereThisWasEliminated.id)
							nextBag = bagWhereThisWasEliminated;
					}
				}
				if(nextBag != null)
					treeDecomposition.addTreeEdge(b, nextBag);
			}
			// Now glue in the new bags: 
			while(!myStack.isEmpty()){
				Set<T> s = myStack.pop();
				Bag<T> added = treeDecomposition.createBag(s);
				added.id = myStack.size()+1;
				Bag<T> nextBag = null;
				for(T v : added.vertices){
					if(!eliminatedAt.containsKey(v)){
						eliminatedAt.put(v, added);
					}
					Bag<T> bagWhereThisWasEliminated = eliminatedAt.get(v);
					if(bagWhereThisWasEliminated.id != added.id){
						if(nextBag == null || nextBag.id > bagWhereThisWasEliminated.id)
							nextBag = bagWhereThisWasEliminated;
					}
				}
				if(nextBag != null)
					treeDecomposition.addTreeEdge(added, nextBag);
			}
			
//			while(!myStack.isEmpty()){
//				Set<T> s = myStack.pop();
//				treeDecomposition.createBag(s);
//			}
//			///////////////////////////////////////////////////////////////////////
//			// Get a list of all bags, and sort it in reverse elimination order
//			ArrayList<Bag<T>> allBags = new ArrayList<>(treeDecomposition.getBags());
//			allBags.sort((Bag<T> b1, Bag<T> b2) -> b1.id - b2.id);
//			
//			
//			Set<T> seenAlready = new HashSet<>();
//			
//			///////////////////////////////////////////////////////////////////////
//			// Remember of each node when it was eliminated
//			HashMap<T, Integer> posInElimOrder = new HashMap<>();
//			LOG.info("running test on arraylist of size " + allBags.size());
//			
//			for(int i = 0 ; i < allBags.size();i++){
//				Bag<T> nextBag = allBags.get(i);
//				if(nextBag.vertices.size() > 0){
//					T eliminated = null;
//					int count = 0;
//					int lowestNextIndex = -1;
//					for(T v : nextBag.vertices){
//						if(!seenAlready.contains(v)){
//							eliminated = v;
//						}
//						else{
//							count++;
//							if(posInElimOrder.containsKey(v) == false){
//								throw new RuntimeException();
//							}
//							if(posInElimOrder.get(v) > lowestNextIndex)
//								lowestNextIndex = posInElimOrder.get(v);
//						}
//					}
//					if(eliminated == null){
//						LOG.info("eliminated = null?");
//						System.exit(-666);
//					}
//					if(count < nextBag.vertices.size() - 1){
//						LOG.info("count is quatsch?");
//						System.exit(-666);
//					}
//					posInElimOrder.put(eliminated, i);
//					seenAlready.add(eliminated);
//					
//					if(lowestNextIndex >= 0){
//						treeDecomposition.addTreeEdge(nextBag, allBags.get(lowestNextIndex));
//						edgesAdded++;
//					}
//					else{
//						//System.out.println("Starting new connected component! ");
//						components_created++;
//					}
//				}
//			}
		}
		LOG.info("restored elimination order in time " + (System.currentTimeMillis() - tStart) + " , added edges: " + edgesAdded);
		LOG.info("Now the tree has " + treeDecomposition.getNumberOfBags() + " bags, and " + treeDecomposition.getTree().getNumberOfEdges() + " edges!");
		LOG.info("Created " + components_created + " components! ");
		treeDecomposition.connectComponents();
	}


}
