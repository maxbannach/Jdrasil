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
package jdrasil.algorithms.exact;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import jdrasil.algorithms.EliminationOrderDecomposer;
import jdrasil.graph.Graph;
import jdrasil.graph.TreeDecomposer;
import jdrasil.graph.TreeDecomposition;
import jdrasil.graph.TreeDecomposition.TreeDecompositionQuality;

/**
 * This class computes an optimal tree-decomposition by "brute-forcing" all n! possible elimination orders
 * and by taking the best resulting tree-decomposition.
 * 
 * @author Max Bannach
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
		List<T> a = new ArrayList<>(graph.getCopyOfVertices());
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
