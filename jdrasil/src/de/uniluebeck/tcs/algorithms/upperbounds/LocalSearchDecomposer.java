package de.uniluebeck.tcs.algorithms.upperbounds;

/**
 * LocalSearchDecomposer.java
 * @author berndt
 */
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Set;

import de.uniluebeck.tcs.App;
import de.uniluebeck.tcs.algorithms.EliminationOrderDecomposer;
import de.uniluebeck.tcs.graph.Bag;
import de.uniluebeck.tcs.graph.Graph;
import de.uniluebeck.tcs.graph.TreeDecomposer;
import de.uniluebeck.tcs.graph.TreeDecomposition;
import de.uniluebeck.tcs.graph.TreeDecomposition.TreeDecompositionQuality;

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
 */
public class LocalSearchDecomposer<T extends Comparable<T>> implements TreeDecomposer<T>, Serializable {


	private static final long serialVersionUID = 7245062311907240240L;

	/** The Graph that should be decomposed. */
	private final Graph<T> graph;

	/** The number of restarts. */
	private final int r;

	/** The number of steps per restarts. */
	private final int s;

	/** A random generator for the algorithm. */
	private final Random dice;

	/** The current best permutation */
	List<T> permOpt;

	TreeDecomposition<T> tdOpt;

	/**
	 * Initialize the algorithm to decompose the given graph.
	 * @param graph
	 * @param r the number of restarts
	 * @param s the number of steps per restarts
	 */
	public LocalSearchDecomposer(Graph<T> graph, int r, int s, List<T> perm) {
		this.graph = graph.copy();
		this.r = r;
		this.s = s;
		this.dice = App.getSourceOfRandomness();
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

		int evalOpt = evalPerm(permOpt);

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
			int eval = evalPerm(perm);

			// try to improve the current permutation for s steps
			for(int i = 0; i < s; i++){
				// the current best neighbour, its permutation and its score
				List<T> bestNeighbourPerm = null;
				T bestNeighbour = null;
				int evalTmp = Integer.MAX_VALUE;

				// try to improve the current permutation by changing the position of one node
				for(T v: perm){

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
						int evalMax = Integer.MAX_VALUE;
						int evalMin = Integer.MAX_VALUE;
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
					App.reportNewSolution(tdOpt.getWidth());
				}
			}

			// shuffle a new permutation
			T v = anyItem(perm, tabu);
			int j = dice.nextInt(perm.size());
			perm = modifyPerm(perm,v,pos.get(v),j);
			rounds--;
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
			int index = dice.nextInt(perm.size());
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
	 * @param the best known upper bound
	 * @return the cost of perm
	 */
	public int evalPerm(List<T> perm) throws Exception{
		Map<T, Integer> pos = toMap(perm);
		Graph<T> g = graph.copy();
		int maxBag = 0;
		int res = 0;
		int posOfV = 0;
		for(T v: perm){
			// only consider the neighbours with higher index. The others are already removed
			Set<T> bag = new HashSet<T>();
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
		return res+maxBag*maxBag*perm.size()*perm.size();
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
