package jdrasil.algorithms.upperbounds;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import jdrasil.datastructures.IntVector;
import jdrasil.datastructures.UpdatablePriorityQueue;
import jdrasil.Heuristic;
import jdrasil.graph.Bag;
import jdrasil.graph.Graph;
import jdrasil.graph.GraphFactory;
import jdrasil.graph.TreeDecomposition;
import jdrasil.utilities.logging.JdrasilLogger;

public class PaceGreedyDegreeDecomposer {

	/** Jdrasils Logger */
	private final static Logger LOG = Logger.getLogger(JdrasilLogger.getName());
	
	
	private Graph<Integer> graph;
	
	private IntVector[] adjacencyLists;
	
	private int  FLAG;
	
	private int[] helper;
	
	public PaceGreedyDegreeDecomposer(Graph<Integer> input){
		graph = GraphFactory.copy(input);
		FLAG = 0;
	}
	
	public TreeDecomposition<Integer> computeTreeDecomposition(int upperBound){
		long tStart = System.currentTimeMillis();
		IntVector permutation = new IntVector();
		adjacencyLists = new IntVector[graph.getNumVertices()+1];
		IntVector[] bags = new IntVector[graph.getNumVertices()+1];
		helper = new int[graph.getNumVertices()+1];
		UpdatablePriorityQueue<Integer, Integer> q = new UpdatablePriorityQueue<>();
		for(int i = 0 ; i <= graph.getNumVertices() ; i++){
			if(graph.getNeighborhood(i) != null){
				q.insert(i, graph.getNeighborhood(i).size());
				adjacencyLists[i] = new IntVector();
				for(Integer e : graph.getNeighborhood(i))
					adjacencyLists[i].push(e);
			}
		}
		LOG.info("Created adjacency lists and queue, time was " + (System.currentTimeMillis()-tStart));
		
		int largestBag = 0;
		boolean [] hasOwnBag = new boolean[graph.getNumVertices()+1];
		Set<Integer> eliminated = new HashSet<>();
		int[] eliminatedAt = new int[graph.getNumVertices()+1];
		
		for(int iteration = 0 ; (q.size() > 0) ; iteration++){
			int minPrio = q.getMinPrio();
			int next = q.removeMinRandom();
			if(adjacencyLists[next].size() != minPrio){
				throw new RuntimeException("Stored wrong size of neighbourhood! ");
			}
			if(adjacencyLists[next].size() > upperBound){
				return null;
			}
			if(Heuristic.shutdownFlag){
				return null; 
			}
			if(eliminated.contains(next))
				throw new RuntimeException();
			
			eliminatedAt[next]=iteration;
			eliminated.add(next);
			IntVector bag = new IntVector();
			IntVector alsoDelete = new IntVector();
			makeClique(next, bag, alsoDelete);
			bags[iteration] = bag;
			hasOwnBag[next] = true;
			if(adjacencyLists[next].size() >= largestBag)
				largestBag = adjacencyLists[next].size()+1;
			permutation.push(next);
			// Eliminate subsumed nodes
			// Update priorities
			int moreElims = 0;
			for(int i = 0 ; i < adjacencyLists[next].size() ; i++){
				int n = adjacencyLists[next].get(i);
				if(adjacencyLists[n].size() < adjacencyLists[next].size()){ // Must be "<" as the node to be eliminated has already been removed from the adjacency list!
					if(eliminated.contains(n)){
						throw new RuntimeException();
					}
					///////////////////////////////////////////////////
					// DEBUG
					///////////////////////////////////////////////////
					eliminated.add(n);
					eliminatedAt[n] = iteration;
//					q.checkIsHeap();
					
					// Mark additionally added nodes, so we can remove them from adjacency lists later on!
					if(moreElims == 0)
						FLAG++;
					moreElims++;
					helper[n] = FLAG;
					// Okay, eliminate it immediately! 
					permutation.push(n);
					int minValue = q.getMinPrio();
					if(minValue < 0)
						throw new RuntimeException();
					q.updateValue(n, -1000);
//					q.checkIsHeap();
					minValue = q.getMinPrio();
					if(minValue >= 0)
						throw new RuntimeException("Crap! q.size()=" + q.size() + ", minValue=" + minValue);
					int removed = q.removeMin();
					if(removed != n)
						throw new RuntimeException();
				}
				else
					q.updateValue(n, adjacencyLists[n].size());
			}
			if(moreElims > 0){
				for(int i = 0 ; i < adjacencyLists[next].size() ; i++){
					int n = adjacencyLists[next].get(i);
					boolean changed = false;
					if(helper[n] == FLAG){
						// Removed already, don't care
					}
					else{
						int j = 0;
						while(j < adjacencyLists[n].size())
							if(helper[adjacencyLists[n].get(j)] == FLAG){
								adjacencyLists[n].popReplace(j);
								changed = true;
							}
							else
								j++;
					}
					if(changed){
						q.updateValue(n, adjacencyLists[n].size());
					}
				}
			}
		}
		LOG.info("Time for creating the permutation: " + (System.currentTimeMillis()-tStart));
		TreeDecomposition<Integer> result = new TreeDecomposition<>(graph);
		// Add bags:
		
		Map<Integer, Bag<Integer> > actualBags = new HashMap<>();
		for(int i = 0 ; i < bags.length ; i++){
			if(bags[i] != null){
				Set<Integer> tmp = new HashSet<>();
				IntVector intBag = bags[i];
				for(int j = 0 ; j < intBag.size() ; j++)
					tmp.add(intBag.get(j));
				actualBags.put(i, result.createBag(tmp));
			}
		}
		boolean foundNull = false;
		int bagsSeen = 0;
		int edgesAdded = 0;
		for(int i = 0 ; i < bags.length ; i++){
			if(foundNull && bags[i] != null)
				throw new RuntimeException();
			if(bags[i] == null)
				foundNull = true;
			else{
				bagsSeen++;
				IntVector thisBag = bags[i];
				int nextIndex = graph.getNumVertices()+2;
				for(int j = 0 ; j < thisBag.size() ; j++){
					int iterationWhereThisWasEliminated = eliminatedAt[thisBag.get(j)];
					if(iterationWhereThisWasEliminated < i)
						throw new RuntimeException("Node should have been eliminated earlier???");
					if(iterationWhereThisWasEliminated != i && iterationWhereThisWasEliminated < nextIndex)
						nextIndex = iterationWhereThisWasEliminated;
				}
				if(nextIndex < graph.getNumVertices()+2){
					edgesAdded++;
					result.addTreeEdge(actualBags.get(i), actualBags.get(nextIndex));
				}
			}
		}
		
//		for(int i = 0 ; i < permutation.size() ; i++){
//			int elimHere = permutation.get(i);
//			if(bags[elimHere] != null){
//				int nearestNeighbour = -1;
//				IntVector v = bags[elimHere];
//				int lowestIndex = graph.getNumVertices()+2;
//				for(int j = 0 ; j < v.size() ; j++){
//					if(v.get(j) != elimHere){
//						if(eliminatedAt[v.get(j)] < lowestIndex){
//							lowestIndex = v.get(j);
//							nearestNeighbour = eliminatedAt[v.get(j)];
//						}
//					}
//				}
//				if(lowestIndex < graph.getNumVertices()+2){
//					result.addTreeEdge(actualBags.get(elimHere), actualBags.get(nearestNeighbour));
//				}
//				
//			}
//		}
		
		LOG.info("Time was " + (System.currentTimeMillis() - tStart) + " , largest bag: " + largestBag);
		return result;
	}
	
	
	private void makeClique(int next, IntVector bag, IntVector alsoDelete){
		bag.push(next);
		for(int i = 0 ; i < adjacencyLists[next].size() ; i++){
			FLAG++;
			int neighbourIndex = adjacencyLists[next].get(i);
			bag.push(neighbourIndex);
			IntVector thisNeighbours = adjacencyLists[neighbourIndex];
			int j = 0;
			// Remove "next" from adjacency list, and mark which nodes have already been seen: 
			while(j < thisNeighbours.size()){
				if(thisNeighbours.get(j) == next){
					thisNeighbours.popReplace(j);
				}
				else
					helper[thisNeighbours.get(j++)] = FLAG;
			}
			for(j = 0 ; j < adjacencyLists[next].size() ; j++){
				int node = adjacencyLists[next].get(j);
				if(node != neighbourIndex && helper[node] != FLAG)
					thisNeighbours.push(node);
			}
		}
	}
}
