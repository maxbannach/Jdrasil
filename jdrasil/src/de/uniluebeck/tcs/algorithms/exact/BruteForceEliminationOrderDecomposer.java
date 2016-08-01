package de.uniluebeck.tcs.algorithms.exact;

/**
 * BruteForceEliminationOrder.java
 * @author bannach
 */

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import de.uniluebeck.tcs.algorithms.EliminationOrderDecomposer;
import de.uniluebeck.tcs.graph.Graph;
import de.uniluebeck.tcs.graph.TreeDecomposer;
import de.uniluebeck.tcs.graph.TreeDecomposition;
import de.uniluebeck.tcs.graph.TreeDecomposition.TreeDecompositionQuality;

/**
 * This class computes an optimal tree-decomposition by "brute-forcing" all n! possible elimination orders
 * and by taking the best resulting tree-decomposition.
 */
public class BruteForceEliminationOrderDecomposer<T extends Comparable<T>> implements TreeDecomposer<T>, Serializable {

	private static final long serialVersionUID = 3867852159850272050L;
	private final Graph<T> graph;
	private TreeDecomposition<T> bestDecomposition = null;
	
	public BruteForceEliminationOrderDecomposer(Graph<T> graph) {
		this.graph = graph;
	}
	
	@Override
	public TreeDecomposition<T> call() throws Exception {
		List<T> a = new ArrayList<>(graph.getVertices());
		int n = a.size();
		
		// we try to optimize this
		int bestWidth = Integer.MAX_VALUE;
				
		// QuickPerm @see www.quickperm.org
		int[] p = new int[n+1];
        int i, j;
        T tmp;
        for (i = 0; i < n+1; i++) {
                p[i] = i;
            }
        i = 1;

        while (i < n) {
        	if (Thread.currentThread().isInterrupted()) throw new Exception();
        	
            // we have a permutation, look what we got
        	EliminationOrderDecomposer<T> dec = new EliminationOrderDecomposer<T>(
        			graph, a, TreeDecompositionQuality.Heuristic
        			);
        	TreeDecomposition<T> t = dec.call();
        	if (t.getWidth() < bestWidth) {
        		bestWidth = t.getWidth();
        		bestDecomposition = t;
        	}
            
            p[i]--;
            j = i % 2 == 1 ? p[i] : 0;

            tmp = a.get(j);
            a.set(j, a.get(i));
            a.set(i, tmp);
            
            i = 1;
            while (p[i] == 0) {
                p[i] = i;
                i++;            
            }
        }
        // do something with the last permutation
    	EliminationOrderDecomposer<T> dec = new EliminationOrderDecomposer<T>(
    			graph, a, TreeDecompositionQuality.Heuristic
    			);
    	TreeDecomposition<T> t = dec.call();
    	if (t.getWidth() < bestWidth) {
    		bestWidth = t.getWidth();
    		bestDecomposition = t;
    	}
		
		
		return bestDecomposition;
	}

	@Override
	public TreeDecompositionQuality decompositionQuality() {
		return TreeDecompositionQuality.Exact;
	}

	@Override
	public TreeDecomposition<T> getCurrentSolution() {
		return bestDecomposition;
	}
	
}
