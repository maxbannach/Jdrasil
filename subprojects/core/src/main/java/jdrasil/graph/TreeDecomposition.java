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
	 * Compute the connected components of the tree-decomposition of bags that contain the vertex v.
	 * For each connected component there will be one arbitrary vertex on the returned stack, i.e.,
	 * in a valid decomposition the size of the stack is 1.
	 * @param x
	 * @return
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
	 * Computes the unique path between s and t in the tree-decomposition.
	 * @param s
	 * @param t
	 * @return
	 */
	private List<Bag<T>> getPath(Bag<T> s, Bag<T> t) {
		
		// DFS data structure
		Map<Bag<T>, Bag<T>> predecessor = new HashMap<>();
		Stack<Bag<T>> S = new Stack<>();		
		predecessor.put(s, s);
		S.push(s);
		
		// perform DFS starting on s
		while (!S.isEmpty()) {
			Bag<T> u = S.pop();
			for (Bag<T> v : tree.getNeighborhood(u)) {
				if (!predecessor.containsKey(v)) {
					predecessor.put(v, u);
					S.push(v);
				}
			}
		}
		
		// reconstruct the path
		List<Bag<T>> path = new ArrayList<>(tree.getCopyOfVertices().size());		
		Bag<T> current = t;
		while (!predecessor.get(current).equals(current)) {
			path.add(current);
			current = predecessor.get(current);
		}
		path.add(current);
		
		// done
		return path;		
	}
	
	/**
	 * This method returns an invalid vertex, or null if there is no invalid vertex.
	 * A vertex v is invalid, if there is a path \(B_1,B_2,...,B_k\) with \(v \in B_1\), \(v \in B_2\), but
	 * \(v \not\in B_i\) for \(1 \lt i \lt k\).
	 * The method computes such a path, and it can be stored in a given List object. 
	 * @return
	 */
	public T getInvalidVertex(List<Bag<T>> path) {
		if (path != null) path.clear();
		for (T v : graph) {
			Stack<Bag<T>> S = connectedComponents(v);
			if (S.size() <= 1) continue; // this vertex is valid
			Bag<T> s = S.pop();
			Bag<T> t = S.pop();					
			path.addAll(getPath(s, t));	
			
			return v;
		}
		
		// no path found -> decomposition is valid
		return null;
	}
	
	/**
	 * Improve the current decomposition by trying to find a suitable, improvable bag. 
	 */
	public void improveDecomposition(){
		boolean stop = false;
		do{
			stop = true;
			for(Bag<T> b: tree.getCopyOfVertices()){
				Graph<T> g = toGraph(b);
				
				// if the graph is not a clique, improve it
				if(!g.isClique()){
					improveBag(b);
					stop = false;
					break;
				}
			}
			
		}
		while(!stop);
		
	}
	
/**
 * Improve a bag by computing a minimum separator for it and splitting it according to the separator. 
 * See "Treewidth computations I. Upper bounds" by Bodlaender and Koster.
 * @param b the bag to improve
 */
	public void improveBag(Bag<T> b){
		Set<Bag<T>> neighbours = tree.getNeighborhood(b);
		Graph<T> g = toGraph(b);
		
		// compute a minimal separator
		Set<T> sep = new MinimalSeparator<T>(g).getSeperator();

		// remove the separator from the graph
		for(T v: sep){
			g.removeVertex(v);
		}
		
		// compute the remaining connected components
		List<Set<T>> cs = g.getConnectedComponents();
		
		// remove the bag b from the decomposition
		tree.removeVertex(b);
		
		// replace b by a new bag containing the separator
		Bag<T> bsep = createBag(sep);
		tree.addVertex(bsep);
		
		// add more bags containing the separator and the connected components
		for(Set<T> set: cs){
			Set<T> tset = new HashSet<>();
			tset.addAll(set);
			
			set.addAll(sep);
			Bag<T> bset =  createBag(set);
			tree.addVertex(bset);
			tree.addEdge(bsep, bset);

			// connected the components to the outer bags
			for(Bag<T> bx: neighbours){
				Set<T> intersection = new HashSet<T>(sep); // use the copy constructor
				intersection.retainAll(bx.vertices);
				Set<T> union = new HashSet<T>(sep); // use the copy constructor
				union.addAll(tset);
				if(union.containsAll(intersection)){
					tree.addEdge(bset, bx);
				}
			}
		}
	}
	
	/**
	 * Construct a graph from a bag. 
	 * It consists of all vertices from the bag and all graph edges. 
	 * In addition, it also contains {u,v}, if there is another bag that contains u and v.
	 * @param b the bag
	 * @return the graph constructed from the bag
	 */
	public Graph<T> toGraph(Bag<T> b){
			Graph<T> g = new Graph<>();
		for(T v: b.vertices){
			g.addVertex(v);
		}
		for(T v: b.vertices){
			for(T u: b.vertices){
				if(u != v){
					if(graph.isAdjacent(u, v) || inAnotherBag(u,v,b)){
						g.addEdge(u, v);
					}
				}
			}
		}
		return g;
	}
	
	/**
	 * Computes whether another bag exists that contains u and v.
	 * @param u the first node u
	 * @param v the second node v
	 * @param b the original bag that contains u and v
	 * @return whether a bag b' != b exists that also contains u and v
	 */
	private boolean inAnotherBag(T u, T v, Bag<T> b){
		boolean res = false;
		for(Bag<T> d : tree.getCopyOfVertices()){
			if(d != b && d.contains(u) && d.contains(v)){
				res = true;
			}
		}
		return res;
	}

	public TreeDecomposition<T> copy() {
		TreeDecomposition<T> res = new TreeDecomposition<T>(GraphFactory.copy(graph));
		res.tree = GraphFactory.copy(tree);
		res.n = n;
		res.numberOfBags = numberOfBags;
		res.width = width;
		res.createdFromPermutation = createdFromPermutation;
		return res;
	}
	
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

	// Contract adjacent bags with same content.
	public void contractDuplicateBags() {
		if (numberOfBags==0) {
			return;
		}
		Set<Bag<T>> visited=new HashSet<>();
		Stack<Bag<T>> S = new Stack<>();

		Bag<T> root=tree.iterator().next();
		S.push(root);
		visited.add(root);
		while (!S.isEmpty()) {
			Bag<T> parent = S.pop();
			boolean modified=true;
			while (modified) {
				modified=false;
				for (Bag<T> child : tree.getNeighborhood(parent)) {
					if (!visited.contains(child)) {
						// TODO contraction is inconsequent!
						if (child.vertices.equals(parent.vertices)
								|| (parent.vertices.containsAll(child.vertices) && (tree.getNeighborhood(child).size()<=2)) ) {
							tree.contract(parent, child);
							modified=true;
							break;
						} else {
							S.push(child);
							visited.add(child);
						}
					}
				}
			}
		}
		
		numberOfBags=tree.getCopyOfVertices().size();
		int id=1;
		for (Bag<T> v : tree) {
			v.id=id++;
		}
	}
	
	// Make the tree decomposition nice. The root and each leaf is an empty bag.
	// Each inner bag has either two copies of itself as child or a single child,
	// which differs in exactly one vertex.
	// Path decompositions remain paths.
	public void makeNice() {
		if (numberOfBags==0) {
			return;
		}
		contractDuplicateBags();

		Set<Bag<T>> visited=new HashSet<>();
		Stack<Bag<T>> S = new Stack<>();

		// find a leaf as root, so that path decompositions remain paths
		Bag<T> root=null;
		for (Bag<T> v : tree) {
			if (tree.getNeighborhood(v).size()<=1) {
				root=v;
				break;
			}
		}
		S.push(root);
		visited.add(root);
		
		// Add bags: from root bag to empty bag one by one
		if (root.vertices.size()>0) {
			Set<T> currentRootSet=new HashSet<>(root.vertices);
			Bag<T> prevRoot=root;
			for (T v : root.vertices) {
				currentRootSet.remove(v);
				Bag<T> b=createBag(new HashSet<T>(currentRootSet));
				addTreeEdge(b, prevRoot);
				prevRoot=b;
				visited.add(b); // these nodes are already nice
			}
		}
		
		while (!S.isEmpty()) {
			Bag<T> parent = S.pop();
			Set<Bag<T>> childs=new HashSet<>();
			for (Bag<T> v : tree.getNeighborhood(parent)) {
				if (!visited.contains(v)) {
					childs.add(v);
				}
			}
			int childsToCome=childs.size();
			// if leaf, then add bags: remove vertices one by one
			if (childsToCome==0) {
				Set<T> currentSet=new HashSet<>(parent.vertices);
				Bag<T> prevParent=parent;
				for (T v : parent.vertices) {
					currentSet.remove(v);
					Bag<T> b=createBag(new HashSet<T>(currentSet));
					addTreeEdge(prevParent, b);
					prevParent=b;
					//visited.add(b); not needed
				}
			}
			// else iterate over childs
			for (Bag<T> child : childs) {
				// remove edges; we probably will create new nodes in between.
				tree.removeEdge(parent, child);
				//if (child.vertices.equals(parent.vertices)) {
				//	System.out.println("error on " + parent);
				//}
			}
			for (Bag<T> child : childs) {				
				Bag<T> leftParent=parent;
				Bag<T> rightParent=null;
				// if there is more than one child left, make the node a join node
				if (childsToCome>1) {
					leftParent=createBag(new HashSet<T>(parent.vertices));
					addTreeEdge(parent, leftParent);
					visited.add(leftParent);
					rightParent=createBag(new HashSet<T>(parent.vertices));
					addTreeEdge(parent, rightParent);
					visited.add(rightParent);
				}

				// Append the child (to the left parent, if join node; otherwise, 
				// leftParent is the right parent from the iteration before).
				// Do this nicely, so remove or add one vertex at a time.
				
				Set<T> removedVertices=new HashSet<>(parent.vertices);
				removedVertices.removeAll(child.vertices);
				Set<T> addedVertices=new HashSet<>(child.vertices);
				addedVertices.removeAll(parent.vertices);
				int diffSize=removedVertices.size()+addedVertices.size();
				
				Set<T> currentSet=new HashSet<>(parent.vertices);
				// in the following, at least one for loop will be executed once.
				Bag<T> newChild=null;
				for (T v : removedVertices) {
					currentSet.remove(v);
					diffSize--;
					if (diffSize==0) {
						newChild=child;
					} else {
						newChild=createBag(new HashSet<T>(currentSet));
						visited.add(newChild);
					}
					addTreeEdge(leftParent, newChild);
					leftParent=newChild;
				}
				for (T v : addedVertices) {
					currentSet.add(v);
					diffSize--;
					if (diffSize==0) {
						newChild=child;
					} else {
						newChild=createBag(new HashSet<T>(currentSet));
						visited.add(newChild);
					}
					addTreeEdge(leftParent, newChild);
					leftParent=newChild;
				}

				// In the following, newChild is the same as child.
				S.push(newChild);
				visited.add(newChild);
				
				parent=rightParent;
				childsToCome--;
			}
		}
	}
}
