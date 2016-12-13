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
/**
 * @author Max Bannach
 */

import java.math.BigInteger;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jdrasil.App;
import jdrasil.algorithms.exact.CopsAndRobber;
import jdrasil.algorithms.exact.SATDecomposer;
import jdrasil.algorithms.exact.SATDecomposer.Encoding;
import jdrasil.algorithms.lowerbounds.MinorMinWidthLowerbound;
import jdrasil.algorithms.upperbounds.StochasticMinFillDecomposer;
import jdrasil.graph.Bag;
import jdrasil.graph.Graph;
import jdrasil.graph.GraphFactory;
import jdrasil.graph.TreeDecomposer;
import jdrasil.graph.TreeDecomposition;
import jdrasil.graph.TreeDecomposition.TreeDecompositionQuality;
import jdrasil.sat.Formula;

/**
 * This class implements a hand-crafted algorithm that uses various of the other algorithms to compute an optimal
 * tree-decomposition. 
 * 
 * Using call() will invoke the following:
 *   a) the graph will be partitioned into its connected components, which are treated separately in the following
 *   b) the graph will be reduced by preprocessing
 *   c) and upper bound will be computed by stochastic-min fill
 *   d) a lower bound will be computed by minor-min-width
 *   e) if ub == lb the computation is done
 *   f) if n^ub is smaller then some threshold the optimal solution will be computed by the dynamic cops-and-robber game
 *   g) otherwise the optimal solution will be computed by a SAT-Encoding, either serial or parallel
 *   h) finally, all computed decompositions are merged
 *   
 * @param <T>
 * @author Max Bannach
 */
public class ExactDecomposer<T extends Comparable<T>> implements TreeDecomposer<T> {

	/** The graph that we wish to decompose. */
	private final Graph<T> graph;
	
	/** This flag is set to true, if the algorithm runs in parallel mode. */
	private final boolean parallel;
	
	/** If the graph has less then <value> vertices, then the instance may be solved by a game of Cops and Robber. */
	private final int COPS_VERTICES_THRESHOLD = 25;
	
	/** If the graph has a treewidth of less then <value>, then the instance may be solved by a game of Cops and Robber. */
	private final int COPS_TW_THRESHOLD = 8;
	
	/**
	 * Default constructor to initialize data structures. 
	 * @param graph
	 * @param parallel – should the graph be decomposed in parallel?
	 * @param timeout – timeout for the sat-solver
	 */
	public ExactDecomposer(Graph<T> graph, boolean parallel) {
		this.graph = graph;
		this.parallel = parallel;
	}
	
	/**
	 * Compute a tree-decomposition of a connected component of the graph.
	 * This method is the core of the algorithm invoked by @see call(), it will
	 * a) use preprocessing to reduce the size of the component
	 * b) compute lower and upper bounds of the component
	 * c) compute an optimal tree-decomposition via the cops-and-robber game if the component is small enough
	 * d) compute an optimal tree-decomposition via a SAT-encoding
	 * @param component
	 * @return
	 * @throws Exception
	 */
	private TreeDecomposition<T> computeTreeDecompositionOfComponent(Graph<T> component) throws Exception {
		ReductionRuleDecomposer<T> preprocessor = new ReductionRuleDecomposer<T>(component);
		
		// reduce the graph, if this yields an empty graph we are already done
		if (preprocessor.reduce()) {
			App.log("Graph was fully reduced");
			return preprocessor.getTreeDecomposition();
		}
						
		// otherwise we have to handle the rest
		Graph<T> reduced = preprocessor.getReducedGraph();
		int n = reduced.getVertices().size();
		App.log("Reduced the graph from " + component.getVertices().size() + " to " + n + " vertices");
		
		// first compute lower and upper bounds on the tree-width
		int lb = new MinorMinWidthLowerbound<>(reduced).call();
		App.log("Computed lower bound: " + lb);
		TreeDecomposition<T> ubDecomposition = new StochasticMinFillDecomposer<T>(reduced).call();
		int ub = ubDecomposition.getWidth();		
		App.log("Computed upper bound: " + ub);
		
		// if they match, we are done as well
		if (lb == ub) {
			App.log("The bounds match, extract decomposition");
			preprocessor.glueTreeDecomposition(ubDecomposition);
			return preprocessor.getTreeDecomposition();
		}

		BigInteger freeMemory = new BigInteger(""+ Runtime.getRuntime().freeMemory());
		BigInteger expectedMemory = App.binom(new BigInteger(""+n), new BigInteger(""+ub)).multiply(new BigInteger(""+(n+32)/8));
		App.log("Free Memory: " + freeMemory);		
		App.log("Expected Memory: " + expectedMemory);
		
		// otherwise check if the instance is small enough for the dynamic cops-and-robber game
		// the algorithm has running time O(n choose k), so we check the size of n choose k
		// This is also used if no SAT-Solver is available
		/*
		if (!Formula.canRegisterSATSolver() || (n <= COPS_VERTICES_THRESHOLD && ub <= COPS_TW_THRESHOLD && expectedMemory.compareTo(freeMemory) < 0)) {	
			App.log("Solve with a game of Cops and Robbers");
			TreeDecomposition<T> decomposition = new CopsAndRobber<>(reduced).call();
			preprocessor.glueTreeDecomposition(decomposition);
			return preprocessor.getTreeDecomposition();
		}
		*/
			
		/* If everything above does not work, we solve the problem using a SAT-encoding */
		App.log("Solve with a SAT-Solver");
		TreeDecomposition<T> decomposition = new SATDecomposer<>(reduced, Encoding.IMPROVED, lb, ub).call();		
		preprocessor.glueTreeDecomposition(decomposition);
		return preprocessor.getTreeDecomposition();
	}
	
	/**
	 * Computes a single, connected tree-decomposition from a set of decompositions.
	 * This is archived by merging the decompositions, adding an empty bag, and by adding a edge from the empty bag
	 * to one bag of each decomposition.
	 * @param decompositions
	 * @return
	 */
	private TreeDecomposition<T> glueDecompositions(Set<TreeDecomposition<T>> decompositions) {
		TreeDecomposition<T> finalDecomposition = new TreeDecomposition<T>(this.graph);
		Bag<T> empty = finalDecomposition.createBag(new HashSet<>()); // add an empty bag
		
		// handle each decomposition
		for (TreeDecomposition<T> decomposition : decompositions) {
			
			// compute mapping from the bags of the T to bags of the new decomposition
			Map<Bag<T>, Bag<T>> oldToNew = new HashMap<>();
			for (Bag<T> oldBag : decomposition.getBags()) {
				Bag<T> newBag = finalDecomposition.createBag(oldBag.vertices);
				oldToNew.put(oldBag, newBag);
			}
			
			// map edges
			for (Bag<T> s : decomposition.getBags()) {
				for (Bag<T> t : decomposition.getNeighborhood(s)) {
					if (s.compareTo(t) < 0) {
						finalDecomposition.addTreeEdge(oldToNew.get(s), oldToNew.get(t));
					}
				}
			}
			
			// add edge to the new empty bag
			if (oldToNew.size() > 0) {
				Bag<T> someBag = oldToNew.values().iterator().next();
				if (someBag != null) finalDecomposition.addTreeEdge(someBag, empty);
			}
		}
		
		// done
		return finalDecomposition;
	}
	
	@Override
	public TreeDecomposition<T> call() throws Exception {
		App.log("");
		
		// get the connected components
		List<Set<T>> connectedComponents = graph.getConnectedComponents();
		App.log("Found " + connectedComponents.size() + " connected components");
		
		// we will compute a decomposition for every component, in parallel mode this has to be synchronized
		Set<TreeDecomposition<T>> decompositions = parallel ? Collections.synchronizedSet(new HashSet<>()) : new HashSet<>();
		
		// we will handle each component separately
		for (Set<T> component : connectedComponents) {
			Graph<T> G = GraphFactory.graphFromSubgraph(graph, component);
			TreeDecomposition<T> T = computeTreeDecompositionOfComponent(G);
			decompositions.add(T);
		}
		
		// glue all the tree-decompositions together
		TreeDecomposition<T> decomposition = glueDecompositions(decompositions);
		
		// done
		App.log("");
		return decomposition;
	}

	@Override
	public TreeDecomposition<T> getCurrentSolution() {
		return null;
	}

	@Override
	public TreeDecompositionQuality decompositionQuality() {
		return TreeDecompositionQuality.Exact;
	}

}
