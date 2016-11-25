package de.uniluebeck.tcs.jdrasil.algorithms;

/**
 * EliminationOrderDecomposer.java
 * @author bannach
 */

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.uniluebeck.tcs.jdrasil.graph.Bag;
import de.uniluebeck.tcs.jdrasil.graph.Graph;
import de.uniluebeck.tcs.jdrasil.graph.TreeDecomposer;
import de.uniluebeck.tcs.jdrasil.graph.TreeDecomposition;
import de.uniluebeck.tcs.jdrasil.graph.TreeDecomposition.TreeDecompositionQuality;

/**
 * The EliminationOrderDecomposer implements the elimination order algorithm.
 * Given a permutation pi of the vertices of the given graph, the algorithm computes a 
 * tree-decomposition by eliminating the vertices in this order. Eliminating a vertex means:
 *  a) make its neighborhood a clique,
 *  b) create a bag with the eliminated vertex and its neighbors,
 *  c) delete the vertex.
 *
 * @param <T> the vertex type of the graph
 */
public class EliminationOrderDecomposer<T extends Comparable<T>> implements TreeDecomposer<T> {

	/** The graph that should be decomposed. This is copy and can be modified. */
	private final Graph<T> graph;
	
	/** The original graph that should be decomposed. This  can not be modifed. */
	private final Graph<T> original;
	
	/** A permutation of the vertices of the graph. */
	public final List<T> permutation;
	
	/** Quality of this tree-decomposition actually depends on the quality of the permutation. */
	private final TreeDecompositionQuality qualityOfPermutation;
	
	/** Maps a vertex to bag constructed when the vertex was eliminated. */
	public final Map<T, Bag<T>> eliminatedVertexToBag;

	/**
	 * Default constructor. The algorithms is initialized with an undirected graph and with
	 * and permutation of the vertices of this graph.
	 * @param graph
	 * @param permutation
	 */
	public EliminationOrderDecomposer(Graph<T> graph, List<T> permutation, TreeDecompositionQuality qualityOfPermutation) {
		this.graph = graph.copy();
		this.permutation = new LinkedList<T>(permutation);		
		this.qualityOfPermutation = qualityOfPermutation;
		this.eliminatedVertexToBag = new HashMap<>();
		original = graph;
	}

	/**
	 * Compute a tree-decomposition from a given permutation.
	 * See Bodlaender and Koster - Treewidth computations I.
	 * 
	 * @param perm
	 * @return a tree-decomposition
	 */
	private TreeDecomposition<T> permutationToTreeDecomposition(List<T> perm) {

		// end of recursion is a single back
		if (perm.size() == 1) {
			TreeDecomposition<T> decomposition = new TreeDecomposition<>(graph);
			Bag<T> bag = decomposition.createBag(new HashSet<>(perm));
			
			eliminatedVertexToBag.put(perm.get(0), bag);
			return decomposition;
		}
		
		// remove first element from permutation
		T v = perm.get(0);
		perm.remove(0);
		
		// create bag for v
		Set<T> bagVertices = new HashSet<T>();
		bagVertices.add(v);
		for (T u : graph.getNeighborhood(v)) {
			bagVertices.add(u);
		}
	
		// eliminate v
		graph.eliminateVertex(v);
		
		// search the next vertex of the bag that will be eliminated
		T vj = null;
		for (T u : perm) {
			if (bagVertices.contains(u)) {
				vj = u;
				break;
			}
		}
		
		// recurse
		TreeDecomposition<T> decomposition = permutationToTreeDecomposition(perm);
				
		// add edge
		Bag<T> bag = decomposition.createBag(bagVertices);
		eliminatedVertexToBag.put(v, bag);
		decomposition.addTreeEdge(eliminatedVertexToBag.get(v), eliminatedVertexToBag.get(vj));
		
		decomposition.setGraph(original);
		// done
		return decomposition;
	}
	
	@Override
	public TreeDecomposition<T> call() throws Exception {
		int n = graph.getVertices().size();
		TreeDecomposition<T> decomposition = permutationToTreeDecomposition(permutation);
		decomposition.setN(n);
		decomposition.setCreatedFromPermutation(true);
		return decomposition;
	}

	@Override
	public TreeDecompositionQuality decompositionQuality() {
		return qualityOfPermutation;
	}

	@Override
	public TreeDecomposition<T> getCurrentSolution() {
		return null;
	}
	
}
