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
package jdrasil.algorithms.upperbounds;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.logging.Logger;

import jdrasil.Heuristic;
import jdrasil.datastructures.UpdatablePriorityQueue;
import jdrasil.graph.Bag;
import jdrasil.graph.Graph;
import jdrasil.graph.GraphFactory;
import jdrasil.graph.TreeDecomposer;
import jdrasil.graph.TreeDecomposition;
import jdrasil.graph.TreeDecomposition.TreeDecompositionQuality;
import jdrasil.utilities.JdrasilProperties;
import jdrasil.utilities.RandomNumberGenerator;
import jdrasil.utilities.logging.JdrasilLogger;

/**
 * This class implements greedy permutation heuristics to compute a tree-decomposition. The heuristic eliminates
 * the vertex v that minimises some function f(v), ties are broken randomly.
 * See "Bodlaender and Koster: Treewidth computations I. Upper bounds" for an overview of possible functions f.
 * 
 * @param <T>
 * @author Max Bannach
 * @author Thorsten Ehlers
 */
public class GreedyPermutationDecomposer<T extends Comparable<T>> implements TreeDecomposer<T>, Serializable {

	

	/** Jdrasils Logger */
	private final static Logger LOG = Logger.getLogger(JdrasilLogger.getName());
	
	
	private static final long serialVersionUID = 1L;

	/** The graph to be decomposed. */
	private final Graph<T> graph;

	/** The permutation that is computed. */
	private List<T> permutation;

	/** The value k used by the heuristic. */
	private int k;
	
	/** Which algorithm to use? Choices: GreedyFillIn and SparsestSubgraph (c.f. Bodlaender, Upper Bounds) */
	public enum Algorithm {
		Degree,
		FillIn,
		DegreePlusFillIn,
		SparsestSubgraph,
		FillInDegree,
		DegreeFillIn
	}

	/** Algorithm that is used. */
	private Algorithm toRun;

	/**
	 * The algorithm is initialized with a graph that should be decomposed.
	 * The default algorithm to run is FillIn.
	 * @param graph
	 */
	public GreedyPermutationDecomposer(Graph<T> graph) {
		this.graph = graph;
		setToRun(Algorithm.FillIn); // default is FillIn, this heuristic shows solid results on most tests
		setLookAhead(1); // default mode is no look-ahead, this is the fastest version
	}

	/** A Tuple storing a vertex and its value (with respect to the heuristic of the current algorithm). */
	private class VertexValue {
		T vertex;
		int value;
	}

	/**
	 * Given a graph and a vertex of it, this method computes the value (with respect to the choosen heuristic)
	 * of the given vertex in this graph.
	 * @param G the graph which contains the vertex
	 * @param v the vertex for which the value should be computed
	 * @return the heuristic value of the given vertex as VertexTuple
	 */
	private VertexValue getValue(Graph<T> G, T v) {
		VertexValue tuple = new VertexValue();
		tuple.vertex = v;

		// fill-in value and degree of the vertex
		int phi = G.getFillInValue(v);
		int delta = G.getNeighborhood(v).size();
		int n = G.getNumVertices();

		// compute the value of the vertex with respect to the current algorithm
		int value = 0;
		switch (toRun) {
			case Degree:
				value = delta;
				break;
			case FillIn:
				value = phi;
				break;
			case DegreePlusFillIn:
				value = delta + phi;
				break;
			case SparsestSubgraph:
				value = phi - delta;
				break;
			case FillInDegree:
				int m = n*n;
				if(m > 0)
					value = delta + (1/m) * phi;
				else
					value = delta;
				break;
			case DegreeFillIn:
				value = phi + (1/n)*delta;
				break;
		}
		tuple.value = value;

		// done
		return tuple;
	}

	/**
	 * Computes the next vertex to be eliminated with respect to the selected heuristic.
	 * The vertex is returned as tuple, containing the vertex and its value
	 *
	 * The parameter \(k\) defines the look-ahead of the method, i.e., if k = 1 the vertex with the smallest value
	 * is returned; if k = 2 the vertex that minimises the sum of its value and the next best vertex (in the then contracted
	 * graph) is returned. Each layer of \(k\) improves the heuristic, but increases the running time by \(O(n)\).
	 *
	 * @param G
	 * @param k the number of vertices that should be considered in the future
	 * @return A VertexValue tuple storing the best vertex and its value
	 */
	private VertexValue nextVertex(Graph<T> G, int k) {
		// min value that any vertex has, and a list of vertices with this value
		int min = Integer.MAX_VALUE;
		List<VertexValue> best = new LinkedList<>();

		// search for the best vertex
		for (T v : G.getCopyOfVertices()) {

			// the vertex-value-tuple of the current vertex
			VertexValue tuple = getValue(G, v);

			// eliminate vertex and look for further value
			if (k > 1 && G.getCopyOfVertices().size() > 1) {
//				System.out.println("test");
				Graph.EliminationInformation info = G.eliminateVertex(v);
				VertexValue next = nextVertex(G, k - 1);
				G.deEliminateVertex(info);
				tuple.value += next.value;
			}

			// update data
			if (tuple.value < min) {
				min = tuple.value;
				best.clear();
				best.add(tuple);
			} else if (tuple.value == min) {
				best.add(tuple);
			}

		}

		// done
		return best.get(RandomNumberGenerator.nextInt(best.size()));
	}

	/**
	 * Returns the elimination order computed by call().
	 * @return
	 */
	public List<T> getPermutation() {
		return permutation;
	}
	
	@Override
	public TreeDecomposition<T> call() throws Exception {
		return call(graph.getNumVertices()+1);
	}

	
	
	/**
	 * If the algorithm is used with a good upper bound, it can aboard whenever the width of the greedily constructed permutation
	 * becomes to big.
	 * @param upper_bound
	 * @return a tree decomposition or null, if the width of the constructed permutation exceeds the upper bound
	 * @throws Exception
	 */
	public TreeDecomposition<T> call(int upper_bound) throws Exception {
		
		// catch the empty graph
		if (graph.getCopyOfVertices().size() == 0) return new TreeDecomposition<T>(graph);
		long tStart = System.currentTimeMillis();
		// the permutation that we wish to compute and a copy of the graph, which will be modified
		List<T> permutation = new LinkedList<T>();
		Graph<T> workingCopy = GraphFactory.copy(graph);
		if(toRun == Algorithm.Degree){
			workingCopy.setLogEdgesInNeighbourhood(false);
		}
		UpdatablePriorityQueue<T, Integer> q = new UpdatablePriorityQueue<T, Integer>();
		ArrayList<T> helper = new ArrayList<>(graph.getCopyOfVertices());
		Collections.shuffle(helper, new Random(RandomNumberGenerator.nextLong()));
		for(T v : helper){
			VertexValue vv = getValue(graph, v);
			q.insert(vv.vertex, vv.value);
		}
		Map<T, Bag<T>> eliminatedAt = new HashMap<>();
		TreeDecomposition<T> td = new TreeDecomposition<>(graph);
			
		// compute the permutation
		for (int i = 0; i < graph.getNumVertices() && q.size() > 0; i++) {
			if(workingCopy.getNumVertices() != q.size())
				throw new RuntimeException("Queue is wrong???");
			/*-**********************************************************************************
			 * 				Check for termination. 
			 * - Check if it make sense to finalize this decomposition by putting all remaining 
			 * 		nodes into one single bag. If this yields a decomposition which improves 
			 * 		the upper bound, do so. Otherwise, return null
			 ***********************************************************************************/
			if((i % 10) == 0 && (JdrasilProperties.timeout() || Heuristic.shutdownFlag)){
				// Panic, we're running out of time! 
				if(q.size() <= upper_bound){
					Set<T> allRemainingVertices = workingCopy.getCopyOfVertices();
					Bag<T> finalBag = td.createBag(allRemainingVertices);
					for(T v : allRemainingVertices){
						permutation.add(v);
						eliminatedAt.put(v, finalBag);
					}
					td.setCreatedFromPermutation(true);
					LOG.info("PANIC! Returning a bag with " + allRemainingVertices.size() + " nodes, time since started: " + (System.currentTimeMillis() - tStart));
					LOG.info("This bag has id " + finalBag.id + ", and the TD has " + td.getNumberOfBags() + " bags...");
					break;
				}
				else{
					return null;
				}
			}
			// obtain next vertex with respect to the current algorithm and check if this is a reasonable choice
			int lowestPrio = q.getMinPrio();
			T vv = q.removeMinRandom();
			Set<T> tmp = new HashSet<>();
			T v = vv; // nextVertex(working, this.k);
			for(T v1 : workingCopy.getNeighborhood(v)){
				tmp.add(v1);
				for(T v2 : workingCopy.getNeighborhood(v1)){
					tmp.add(v2);
				}
			}
			int predictionNewNumberEdges = workingCopy.getNumberOfEdges() + workingCopy.getFillInValue(v) - workingCopy.getNeighborhood(v).size();
			
			if(workingCopy.getNeighborhood(v).size() >= upper_bound){
				// Okay, this creates a clique of size >= upper_bound + 1, I can abort!
				return null;
			}

			// add it to the permutation and eliminate it in the current subgraph
			permutation.add(v);
			Set<T> bagNodes = new HashSet<>();
			bagNodes.addAll(workingCopy.getNeighborhood(v));
			bagNodes.add(v);
			eliminatedAt.put(v, td.createBag(bagNodes));
			// Look into this bag: Is there a node such that its neighbourhood is a subset of this bag? 
			// If so, it can be removed here as well! 
			workingCopy.eliminateVertex(v, toRun != Algorithm.Degree);
			if(toRun != Algorithm.Degree &&  workingCopy.getNumberOfEdges() != predictionNewNumberEdges){
				throw new RuntimeException("Miss-predicted fill values!");
			}
			// Check if further nodes can be eliminated here! 
			List<T> deleteImmediately = new ArrayList<>();
			for(T u : bagNodes){
				if(u.compareTo(v) != 0){
					if(workingCopy.getNeighborhood(u).size() < bagNodes.size()-1){
						for(T w : workingCopy.getNeighborhood(u))
							if(!bagNodes.contains(w))
								throw new RuntimeException("Hmm. This node DOES have neighbours outside the clique I just created??? ");
						deleteImmediately.add(u);
					}
				}
			}
			if(deleteImmediately.size() == workingCopy.getNumVertices()){
				LOG.info("Creating final bag (" + workingCopy.getNumVertices() + "), time till here: " + (System.currentTimeMillis() -tStart));
			}
			// Delete nodes which have only neighbours in the clique of the node that was just eliminated. 
			// For compatibility reasons, add them to the permutation, and give them bags... 
			for(T u : deleteImmediately){
				permutation.add(u);
				eliminatedAt.put(u, eliminatedAt.get(v));
				workingCopy.eliminateSimplicialVertex(u, toRun != Algorithm.Degree);
				q.updateValue(u, q.getMinPrio()-1);
				if(q.removeMin().compareTo(u) != 0)
					throw new RuntimeException("Removing the node from the queue did not work???");
				tmp.remove(u);
			}
			for(T v_n : tmp){
				if(v_n != v && v.compareTo(v_n) != 0){
					//LOG.info("updating value of node " + v_n + ", v=" + v + ", compare yields" + v.compareTo(v_n));
					VertexValue v_update = getValue(workingCopy, v_n);
					if(v_update == null){
						LOG.info("v_update was null");
					}
					
					q.updateValue(v_n, getValue(workingCopy, v_n).value);
				}
			}
		}

		// done
		LOG.info("Adding bags, time till here: " + (System.currentTimeMillis() - tStart));
		this.permutation = permutation;
		td.setCreatedFromPermutation(true);
		// Finalise the tree decomposition by adding edges to it!
		for(T v : permutation){
			Bag<T> elimBag = eliminatedAt.get(v);
			if(elimBag.id >= td.getNumberOfBags()){
				LOG.info("Reached the final bag, cancelling! Time till here: " + (System.currentTimeMillis() - tStart));
				break;
			}
			Bag<T> connectTo = null;
			for(T u : elimBag.vertices){
				Bag<T> otherBag = eliminatedAt.get(u);
				if(otherBag.id != elimBag.id){
					if(connectTo == null || connectTo.id > otherBag.id)
						connectTo = otherBag;
				}
			}
			if(connectTo != null)
				td.addTreeEdge(elimBag, connectTo);
		}
		LOG.info("And returning, time till here: " + (System.currentTimeMillis() - tStart) + ". Got " + td.getNumberOfBags() + " bags for " + permutation.size() + " nodes!");
		return td; //new EliminationOrderDecomposer<T>(graph, permutation, TreeDecompositionQuality.Heuristic).call();
	}

	@Override
	public TreeDecompositionQuality decompositionQuality() {
		return TreeDecompositionQuality.Heuristic;
	}

	@Override
	public TreeDecomposition<T> getCurrentSolution() {
		return null;
	}

	/**
	 * Returns the algorithm that is currently used.
	 * @return
	 */
	public Algorithm getToRun() {
		return toRun;
	}

	/**
	 * Sets the algorithm that should be used.
	 * @param toRun
	 */
	public void setToRun(Algorithm toRun) {
		this.toRun = toRun;
	}

	/**
	 * Defines how far the heuristic looks into the future while selecting a good vertex.
	 * @param k the number of vertices considered while selected a good vertex
	 */
	public void setLookAhead (int k) { this.k = k; }

	/**
	 * Get the look-ahead value.
	 * @return the look-ahead value.
	 */
	public int getLookAhead() { return k; }

}
