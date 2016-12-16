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
package jdrasil.algorithms;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;

import jdrasil.App;
import jdrasil.graph.Bag;
import jdrasil.graph.Graph;
import jdrasil.graph.TreeDecomposition;

/**
 * There are many reduction rules for tree-width known in the literature. A reduction rule thereby is a function that removes 
 * (in polynomial time) a vertex from the graph and creates a bag of the tree-decomposition such that this bag glued to an optimal 
 * tree-decomposition of the remaining graph yields to an optimal tree-decomposition. With graphs of tree-with at most 3, these rules
 * produce an optimal decomposition in polynomial time.
 * For graphs with bigger tree-width, the rules can only be applied to certain point. From this point on, a other optimal algorithm has to be used.
 *
 * This class provides methods to reduce a graph, and to obtain a optimal tree-decomposition of the full graph, if it is provides with an optimal decomposition of
 * the reduced graph.
 *
 * @author Max Bannach
 * @author Thorsten Ehlers
 */
public class ReductionRuleDecomposer<T extends Comparable<T>> {

	/** The graph that is decomposed. */
	private final Graph<T> original;
	private final Graph<T> graph;
	
	/** A lower bound on the tree-width. */
	private int low;
	
	/** The tree-decomposition of @see graph that we compute */
	private TreeDecomposition<T> td;
	
	/** Bags that are created during the reduction (and which have to be glued to a later decomposition).*/
	private final Stack<Set<T>> bags;
	
	/**
	 * Default constructor to initialize data structures.
	 * @param graph
	 */
	public ReductionRuleDecomposer(Graph<T> graph) {
		this.original = graph.copy();
		this.graph = graph.copy();
		this.td = new TreeDecomposition<T>(graph);
		this.bags = new Stack<>();
		this.low = 0;
	}
		
	/**
	 * If the graph contains an isolated vertex v, create a bag {v} and remove the vertex.
	 * This method returns the created Bag, or null if the rule can not be applied.
	 * @return
	 */
	private Set<T> isolatedVertexRule() {
		for (T v : graph) {
			if (graph.getNeighborhood(v).size() == 0) {
				Set<T> set = new HashSet<>();
				set.add(v);
				graph.deleteVertex(v);
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
	private Set<T> leafVertexRule() {
		for (T v : graph) {
			if (graph.getNeighborhood(v).size() == 1) {
				Set<T> set = new HashSet<>();
				set.add(v);
				set.add(graph.getNeighborhood(v).get(0));
				graph.deleteVertex(v);
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
	public Set<T> seriesRule() {
		for (T v : graph) {
			if (graph.getNeighborhood(v).size() == 2) {
				Set<T> set = new HashSet<>();
				set.add(v);
				set.addAll(graph.getNeighborhood(v));
				graph.eliminateVertex(v);
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
	public Set<T> triangleRule() {
		low = Math.max(low, 4);
		for (T v : graph) {
			if (graph.getNeighborhood(v).size() != 3) continue;
			T x = graph.getNeighborhood(v).get(0);
			T y = graph.getNeighborhood(v).get(1);
			T z = graph.getNeighborhood(v).get(2);
			if (graph.isAdjacent(x, y) || graph.isAdjacent(x, z) || graph.isAdjacent(y, z)) {
				Set<T> set = new HashSet<>();
				set.add(v);
				set.addAll(graph.getNeighborhood(v));
				graph.eliminateVertex(v);
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
	private Set<T> buddyRule() {
		for (T v : graph) {
			if (graph.getNeighborhood(v).size() != 3) continue;
			search: for (T w : graph) {
				if (v.compareTo(w) >= 0) continue;
				if (graph.getNeighborhood(v).contains(w)) continue;
				if (graph.getNeighborhood(w).size() != 3) continue;
				for (T x : graph.getNeighborhood(v))  {
					if (!graph.getNeighborhood(w).contains(x)) continue search;
				}
					
				// v and w are buddies;
				Set<T> set = new HashSet<>();
				set.add(v);
				set.addAll(graph.getNeighborhood(v));
				graph.eliminateVertex(v);
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
	private Set<T> cubeRule() {
		for (T v : graph) {
			if (graph.getNeighborhood(v).size() != 3) continue;
			T x = graph.getNeighborhood(v).get(0);
			if (graph.getNeighborhood(x).size() != 3) continue;
			T y = graph.getNeighborhood(v).get(1);
			if (graph.getNeighborhood(y).size() != 3) continue;
			T z = graph.getNeighborhood(v).get(2);
			if (graph.getNeighborhood(z).size() != 3) continue;
			
			// v is center of cube with neighbors x,y,z, compute other corners a,b,c
			T a = graph.getNeighborhood(x).get(0);
			if (a.compareTo(v) == 0) a = graph.getNeighborhood(x).get(2); 
			T b = graph.getNeighborhood(x).get(1);
			if (b.compareTo(v) == 0) b = graph.getNeighborhood(x).get(2);
			
			if ( !(graph.isAdjacent(y, a) && graph.isAdjacent(z, b)) ) {
				T tmp = a;
				a = b;
				b = tmp;
			}
			if ( !(graph.isAdjacent(y, a) && graph.isAdjacent(z, b)) ) continue;
			
			T c = null;
			for (T tmp : graph.getNeighborhood(y)) {
				if (tmp.compareTo(v) != 0 && graph.isAdjacent(z, tmp)) c = tmp;
			}			
			if (c == null) continue;
			
			Set<T> set = new HashSet<>();
			set.add(z);
			set.add(b);
			set.add(c);
			set.add(v);
			graph.deleteVertex(z);
			if (!graph.isAdjacent(a, b)) graph.addEdge(a, b);
			if (!graph.isAdjacent(a, c)) graph.addEdge(a, c);
			if (!graph.isAdjacent(a, v)) graph.addEdge(a, v);
			if (!graph.isAdjacent(b, c)) graph.addEdge(b, c);
			if (!graph.isAdjacent(b, v)) graph.addEdge(b, v);
			if (!graph.isAdjacent(c, v)) graph.addEdge(c, v);
			
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
	public Set<T> simplicialRule() {
		T v = graph.getSimplicialVertex(new HashSet<>());
		if (v == null) return null;
		Set<T> set = new HashSet<>();
		set.add(v);
		set.addAll(graph.getNeighborhood(v));
		low = Math.max(low, set.size());
		graph.deleteVertex(v);
		return set;
	}

	/**
	 * An almost simplicial vertex v is a vertex, such that N[v] is a clique.
	 * We can remove v and create a bag containing N[v].
	 * 
	 * Returns the bag if the rule can be applied, otherwise it returns null.
	 * @return
	 */
	public Set<T> almostSimplicialRule() {
		T v = graph.getAlmostSimplicialVertex(new HashSet<>());
		if (v == null) return null;
		Set<T> set = new HashSet<>();
		set.add(v);
		set.addAll(graph.getNeighborhood(v));
		if (set.size() > low) return null;
		graph.eliminateVertex(v);
		return set;
	}
	
	/**
	 * Glues the bag to the tree-decomposition in construction.
	 * @param bag
	 */
	private void glue(Set<T> bag) {
		for (Bag<T> x : td.getBags()) {
			int count = 0;
			for (T v : bag) {
				if (!x.contains(v)) count++;
			}
			if (count > 1) continue;
			Bag<T> y = td.createBag(bag);
			td.addTreeEdge(y, x);
			return;
		}
		
		// if there is no bag, just insert new bag
		td.createBag(bag);
	}
	
	/**
	 * Glues all bags that where generated during the reduction.
	 */
	private synchronized void glueBags() {
		// Don't call the alternative version - does not work if td was produced by the dynamic program? 
		if(td.isCreatedFromPermutation())
			glueBags_test();
		// 
		App.log("Calling old glueBags, bags to glue: " + bags.size());
		while (!bags.isEmpty()) {
			glue(bags.pop());
		}
	}
	
	private void glueBags_test(){
		// Get elimination order of given TD
		long tStart = System.currentTimeMillis();
		
		Stack<Set<T>> myStack = bags; //(Stack<Set<T>>) bags.clone();
		while(!myStack.isEmpty()){
			Set<T> s = myStack.pop();
			td.createBag(s);
		}
		///////////////////////////////////////////////////////////////////////
		// Get a list of all bags, and sort it in reverse elimination order
		ArrayList<Bag<T>> allBags = new ArrayList<>(td.getBags());
		allBags.sort((Bag<T> b1, Bag<T> b2) -> b1.id - b2.id);
		
		
		Set<T> seenAlready = new HashSet<>();
		
		///////////////////////////////////////////////////////////////////////
		// Remember of each node when it was eliminated
		HashMap<T, Integer> posInElimOrder = new HashMap<>();
		App.log("running test on arraylist of size " + allBags.size());
		int edgesAdded = 0;
		int components_created = 0;
		for(int i = 0 ; i < allBags.size();i++){
			Bag<T> nextBag = allBags.get(i);
			if(nextBag.vertices.size() > 0){
				T eliminated = null;
				int count = 0;
				int lowestNextIndex = -1;
				for(T v : nextBag.vertices){
					if(!seenAlready.contains(v)){
						eliminated = v;
					}
					else{
						count++;
						if(posInElimOrder.containsKey(v) == false){
							throw new RuntimeException();
						}
						if(posInElimOrder.get(v) > lowestNextIndex)
							lowestNextIndex = posInElimOrder.get(v);
					}
				}
				if(eliminated == null){
					App.log("eliminated = null?");
					System.exit(-666);
				}
				if(count < nextBag.vertices.size() - 1){
					App.log("count is quatsch?");
					System.exit(-666);
				}
				posInElimOrder.put(eliminated, i);
				seenAlready.add(eliminated);
				
				if(lowestNextIndex >= 0){
					td.addTreeEdge(nextBag, allBags.get(lowestNextIndex));
					edgesAdded++;
				}
				else{
					//System.out.println("Starting new connected component! ");
					components_created++;
				}
			}
		}
		App.log("restored elimination order in time " + (System.currentTimeMillis() - tStart) + " , added edges: " + edgesAdded);
		App.log("Created " + components_created + " components! ");
		td.connectComponents();
	}
	
	/**
	 * Reduce the stored graph. If the graph can reduced to the empty graph, this methods returns true.
	 * If this is the case, @see getTreeDecomposition can be called.
	 * @return
	 */
	public boolean reduce() {
				
		// graph fully reduced
		if (graph.getVertices().size() == 0) {
			glueBags();
			return true; 
		}
		
		
		// Handle the easy cases: Nodes with degree 0/1
		eliminateLowDegreeNodes(0);
		eliminateLowDegreeNodes(1);
		// If the graph is a tree, we should be done by now. 
		if(graph.getVertices().size() == 0){
			App.log("Done, input was a tree! ");
			glueBags();
			return true;
		}
		// No tree, so we can run the simple elimination on nodes with degree 2: 
		eliminateLowDegreeNodes(2);
		
		if(graph.getVertices().size() == 0){
			glueBags();
			return true;
		}
		
		boolean fixPointReached = false;
		
		while(!fixPointReached){
			Set<T> bag = null;
			bag = isolatedVertexRule();
			if (bag == null) bag = leafVertexRule();
			if (bag == null) bag = seriesRule();
			if (bag == null) bag = triangleRule();
			if (bag == null) bag = buddyRule();
			if (bag == null) bag = cubeRule();
			if (bag == null) bag = simplicialRule();
			if (bag == null) bag = almostSimplicialRule();
			if (bag == null)
				fixPointReached = true;
			else{
				bags.push(bag);
			}
		}
		if(graph.getVertices().size() == 0){
			glueBags();
			return true;
		}
		return false;	
//		// try to apply rule
//		Set<T> bag = null;
//		bag = isolatedVertexRule();
//		if (bag == null) bag = leafVertexRule();
//		if (bag == null) bag = seriesRule();
//		if (bag == null) bag = triangleRule();
//		if (bag == null) bag = buddyRule();
//		if (bag == null) bag = cubeRule();
//		if (bag == null) bag = simplicialRule();
//		if (bag == null) bag = almostSimplicialRule();
//		
//		// no rule could be applied
//		if (bag == null) return false;
//		
//		// recursion
//		bags.push(bag);
//		boolean result = reduce();	
//		return result;
	}
	/*
	 * Eliminate nodes with low degree. 
	 * Here, called with maxDegree <= 2 - so this implements the rules "Twig rule", "Series rule" and easy cases of the "Simplicial" rule
	 */
	private void eliminateLowDegreeNodes(int maxDegree) {
		Set<T> seen = new HashSet<>();
		Queue<T> q = new LinkedList<>();
		for(T v : graph){
			if(graph.getNeighborhood(v).size() <= maxDegree){
				q.add(v);
				seen.add(v);
			}
		}
		while(!q.isEmpty()){
			T next = q.poll();
			List<T> N = new ArrayList<T>();
			/* Just to make sure I don't get into trouble with some side effects*/
			N.addAll(graph.getNeighborhood(next));
			if(N.size() <= maxDegree){
				Set<T> newBag = new HashSet<>();
				newBag.add(next);
				newBag.addAll(N);
				bags.push(newBag);
				graph.eliminateVertex(next);
				for(T v : N){
					if(graph.getNeighborhood(v).size() <= maxDegree && !seen.contains(v)){
						q.add(v);
						seen.add(v);
					}
				}
			}
		}
	}
	/**
	 * Glues a tree-decomposition of the reduced graph to the currently stored decomposition.
	 * This method should be called after @see reduce()
	 */
	public void glueTreeDecomposition(TreeDecomposition<T> decomposition) {
		if (decomposition.getWidth() == 0) return;
		this.td = decomposition;
		this.td.setGraph(original);
		glueBags();
	}
	
	/**
	 * This method returns the reduced graph generated by this class.
	 * The method should be called after @see reduce()
	 * @return
	 */
	public Graph<T> getReducedGraph() {
		return graph;
	}
	
	/**
	 * This method returns a tree-decomposition of the stored graph.
	 * If it is called after @see reduce() it may contain a uncomplete decomposition.
	 * If it is called after @see glueTreeDecomposition() it holds a complete decomposition with quality
	 * depending on the provided decomposition.
	 * Otherwise this method returns an empty tree-decomposition.
	 * @return
	 */
	public TreeDecomposition<T> getTreeDecomposition() {
		return td;
	}
}
