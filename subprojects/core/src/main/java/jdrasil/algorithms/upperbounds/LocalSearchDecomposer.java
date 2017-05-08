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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.logging.Logger;

import jdrasil.Heuristic;
import jdrasil.algorithms.EliminationOrderDecomposer;
import jdrasil.graph.Bag;
import jdrasil.graph.Graph;
import jdrasil.graph.GraphFactory;
import jdrasil.graph.TreeDecomposer;
import jdrasil.graph.TreeDecomposition;
import jdrasil.graph.TreeDecomposition.TreeDecompositionQuality;
import jdrasil.utilities.RandomNumberGenerator;
import jdrasil.utilities.logging.JdrasilLogger;
import jdrasil.utilities.JdrasilProperties;

/**
 * This class implements a tabu search on the space of elimination orders developed by Clautiax, Moukrim, Negre and Carlier
 * in their work "Heuristic and Metaheuristic Method for Computing Graph Treewidth".
 * The algorithms expects two error parameters r and s: the number of restarts and the number of steps.
 * To find a tree-decomposition the algorithms starts upon a given permutation, then it will try to move a single
 * node to improve the decomposition and do this s times. If no node can be moved, a random node is moved and the 
 * process restarts. At most r restarts will be performed.
 * 
 * At the end, the best found tree-decomposition is returned.
 *
 * @param <T> the vertex type of the graph
 * @author Sebastian Berndt
 */
public class LocalSearchDecomposer<T extends Comparable<T>> implements TreeDecomposer<T>, Serializable {

	/** Jdrasils Logger */
	private final static Logger LOG = Logger.getLogger(JdrasilLogger.getName());

	private static final long serialVersionUID = 7245062311907240240L;

	/** The Graph that should be decomposed. */
	private final Graph<T> graph;

	/** The number of restarts. */
	private final int r;

	/** The number of steps per restarts. */
	private final int s;

	/** The current best permutation */
	List<T> permOpt;

	TreeDecomposition<T> tdOpt;

	/**
	 * Initialize the algorithm to decompose the given graph.
	 * @param graph to be decomposed
	 * @param r the number of restarts
	 * @param s the number of steps per restarts
	 */
	public LocalSearchDecomposer(Graph<T> graph, int r, int s, List<T> perm) {
		this.graph = GraphFactory.copy(graph);
		this.r = r;
		this.s = s;
		this.permOpt = perm;		
	}


	@Override
	public TreeDecomposition<T> call() throws Exception {

		// initialize the current optimal permutation
		// those variables only change if we find a strictly better permutation
		EliminationOrderDecomposer<T> dec = new EliminationOrderDecomposer<T>(
				graph, permOpt, TreeDecompositionQuality.Heuristic
				);
		tdOpt = dec.call();

		long evalOpt = evalPerm(permOpt);

		// initialize the current permutation that we will work on
		List<T> perm = new LinkedList<T>(permOpt);

		Queue<T> tabu = new LinkedList<T>();
		int rounds = r;

		while( rounds > 0){
			// compute the variables belonging to the current permutation
			dec = new EliminationOrderDecomposer<T>(
					graph, perm, TreeDecompositionQuality.Heuristic
					);

			dec.call();
			Map<T, Bag<T>> map = dec.eliminatedVertexToBag;
			Map<T,Integer> pos = toMap(perm);
			long eval = evalPerm(perm);

			// try to improve the current permutation for s steps
			for(int i = 0; i < s; i++){
				
				
				
				// the current best neighbour, its permutation and its score
				List<T> bestNeighbourPerm = null;
				T bestNeighbour = null;
				long evalTmp = Long.MAX_VALUE;

				// try to improve the current permutation by changing the position of one node
				for(T v: perm){
					/*
					 * Check if we have to terminate! 
					 */
					if(Heuristic.shutdownFlag)
						return tdOpt;
					// test only the allowed vertices
					if(! tabu.contains(v)){
						// find the minsucc and maxpred vertices of v, i.e. the most likely nodes
						// to improve the current permutation
						T minw = null;
						int min = pos.get(v);
						T maxw = null;
						int max = pos.get(v);
						for(T w: map.get(v).vertices){
							int pw = pos.get(w);
							if(pw < min){
								minw = w;
								min = pw;
							}
							if(pw > max){
								maxw = w;
								max = pw;
							}
						}

						// evaluate the permutations of the minsucc and maxpred vertices
						// and update the bestNeighbour values if necessary
						long evalMax = Long.MAX_VALUE;
						long evalMin = Long.MAX_VALUE;
						if(maxw != null){
							List<T> permMax = modifyPerm(perm, v, pos.get(v), max);
							evalMax = evalPerm(permMax);
							if (evalMax < evalTmp){
								evalTmp = evalMax;
								bestNeighbourPerm = permMax;
								bestNeighbour = maxw;
							}
						}
						if(minw != null){
							List<T> permMin = modifyPerm(perm, v, pos.get(v), min);
							evalMin = evalPerm(permMin);
							if (evalMin < evalTmp){
								evalTmp = evalMin;
								bestNeighbourPerm = permMin;
								bestNeighbour = minw;

							}
						}

					}
				}
				// we could improve our local permutation and thus need to update the current values
				if (evalTmp < eval){

					perm = bestNeighbourPerm;
					dec = new EliminationOrderDecomposer<T>(
							graph, perm, TreeDecompositionQuality.Heuristic
							);
					dec.call();
					map = dec.eliminatedVertexToBag;
					eval = evalPerm(perm);
					pos = toMap(perm);

					// add the moved neighbour to the tabu list and shrink the tabu list if necessary
					tabu.add(bestNeighbour);
					if(tabu.size() > 7){
						tabu.remove();
					}
				}
				else{
					// we are stuck in a local optimum

					break;
				}
			}

			// did we improve our global solution?
			if(eval < evalOpt){
				TreeDecomposition<T> tmp = new EliminationOrderDecomposer<T>(graph,perm,TreeDecompositionQuality.Heuristic).call();
				if(tmp.getWidth() < tdOpt.getWidth()){
					permOpt = perm;
					tdOpt = tmp;
					evalOpt = eval;
					LOG.info("new upper bound: " + tdOpt.getWidth());
				}
			}

			// shuffle a new permutation
			T v = anyItem(perm, tabu);
			int j = RandomNumberGenerator.nextInt(perm.size());
			perm = modifyPerm(perm,v,pos.get(v),j);
			rounds--;
			if(JdrasilProperties.timeout())
                          break;
		}
		tdOpt.connectComponents();
		return tdOpt;
	}

	/**
	 * Converts a permutation list into a Map for fast access
	 * @param perm the permutation 
	 * @return perm as Map
	 */
	public Map<T,Integer> toMap(List<T> perm){
		HashMap<T,Integer> pos = new HashMap<T,Integer>(perm.size());
		int i = 0;

		// store the permutation in a more efficient way. We want O(1) access to the positions.
		for(T v: perm){
			pos.put(v, i);
			i++;
		}
		return pos;
	}

	/**
	 * Chooses any item from perm\setminus tabu randomly
	 * @param perm a permutation of the nodes
	 * @param tabu the nodes that can not be used
	 * @return a random node
	 */
	public T anyItem(List<T> perm, Queue<T> tabu){
		while(true){
			int index = RandomNumberGenerator.nextInt(perm.size());
			T v = perm.get(index);
			if(!tabu.contains(v)){
				return v;
			}
		}
	}


	/**
	 * 	Modifies the permutation by  moving v from its current position posOfV to position j
	 * @param perm the current permutation
	 * @param v the node to be moved
	 * @param posOfV the current position of the node
	 * @param j the position where v is moved
	 * @return the modified permutation
	 */
	public List<T> modifyPerm(List<T> perm, T v, int posOfV,int j){
		List<T> nperm = new LinkedList<T>(perm);

		// remove v from its current position
		nperm.remove(posOfV);
		int offset = 0;
		if(posOfV < j){
			offset = -1;
		}

		// add v to its new position
		nperm.add(j+offset, v);
		return nperm;
	}

	
	/**
	 * Calculates the cost of a permutation by computing its treewidth and preferring tree decompositions with more smaller bags.
	 * @param perm The permutation
	 * @return the cost of perm
	 */
	public long evalPerm(List<T> perm) throws Exception{
		Map<T, Integer> pos = toMap(perm);
		Graph<T> g = GraphFactory.copy(graph);
		g.setLogEdgesInNeighbourhood(false);
		int maxBag = 0;
		long res = 0;
		int posOfV = 0;
		int maxDegreeSeenHere = 0;
		for(T v: perm){
			// only consider the neighbours with higher index. The others are already removed
			Set<T> bag = new HashSet<T>();
			if(g.getNeighborhood(v).size() > maxDegreeSeenHere)
				maxDegreeSeenHere = g.getNeighborhood(v).size();
			for(T u: g.getNeighborhood(v)){
				if(pos.get(u) > posOfV){
					bag.add(u);
				}
			}

			// update the treewidth if necessary
			int tmp = bag.size();
			if (tmp > maxBag){
				maxBag = tmp;
			}

			// add the value succ^2 to the result
			res = res + tmp*tmp;

			// connect every node in the bag
			for(T u: bag){
				for(T w: bag){
					if(!g.isAdjacent(u,w)){
						g.addEdge(u,w);
					}
				}
			}
			posOfV++;
		}

		// ensure that the tw dominates
		long l1 = maxBag * maxBag;
		long l2 = perm.size() * perm.size();
		
		return res+l1*l2;
	}


	@Override
	public TreeDecompositionQuality decompositionQuality() {
		return TreeDecompositionQuality.Heuristic;
	}

	@Override
	public TreeDecomposition<T> getCurrentSolution() {
		return tdOpt;
	}
}
