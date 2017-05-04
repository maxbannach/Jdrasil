package jdrasil.algorithms.upperbounds;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import javax.management.RuntimeErrorException;

import jdrasil.Datastructures.IntVector;
import jdrasil.Datastructures.UpdatablePriorityQueue;
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
	
	public TreeDecomposition<Integer> computeTreeDecomposition(){
		long tStart = System.currentTimeMillis();
		IntVector permutation = new IntVector();
		adjacencyLists = new IntVector[graph.getNumVertices()+1];
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
		int iterations = 0;
		Set<Integer> eliminated = new HashSet<>();
		while(q.size() > 0){
			iterations++;
			if((iterations % 1000) == 0){
				LOG.info("Iteration " + iterations + " , largestBag=" + largestBag + " permutation.size()=" + permutation.size());
			}
			int next = q.removeMin();
			if(eliminated.contains(next))
				throw new RuntimeException();
			eliminated.add(next);
			IntVector bag = new IntVector();
			IntVector alsoDelete = new IntVector();
			makeClique(next, bag, alsoDelete);
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
//					q.checkIsHeap();
					
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
//					q.checkIsHeap();
				}
				else
					q.updateValue(n, adjacencyLists[n].size());
			}
			if(moreElims > 0){
				for(int i = 0 ; i < adjacencyLists[next].size() ; i++){
					int n = adjacencyLists[next].get(i);
					if(helper[n] == FLAG){
						// Removed already, don't care
					}
					else{
						int j = 0;
						while(j < adjacencyLists[n].size())
							if(helper[adjacencyLists[n].get(j)] == FLAG)
								adjacencyLists[n].popReplace(j);
							else
								j++;
					}
					
				}
			}
		}
		LOG.info("Time was " + (System.currentTimeMillis() - tStart) + " , largest bag: " + largestBag);
		return null;
	}
	
	
	private void makeClique(int next, IntVector bag, IntVector alsoDelete){
		for(int i = 0 ; i < adjacencyLists[next].size() ; i++){
			FLAG++;
			int neighbourIndex = adjacencyLists[next].get(i);
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
