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
package jdrasil.algorithms.preprocessing;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import jdrasil.graph.Bag;
import jdrasil.graph.Graph;
import jdrasil.graph.TreeDecomposition;
import jdrasil.graph.invariants.ConnectedComponents;

/**
 * The GraphSeparator splits a graph into multiple components such that the tree decompositions of these components can
 * be glued to obtain an optimal decomposition for the input graph.
 * 
 * @author Max Bannach
 */
public class GraphSeparator<T extends Comparable<T>> extends Preprocessor<T> {

	/**
	 * @param graph
	 */
	public GraphSeparator(Graph<T> graph) {
		super(graph);
		// TODO Auto-generated constructor stub
	}

	//MARK: Preprocessor interface
	
	/* (non-Javadoc)
	 * @see jdrasil.algorithms.preprocessing.Preprocessor#computeGraphs()
	 */
	@Override
	protected Set<Graph<T>> computeGraphs() {
		ConnectedComponents<T> cc = new ConnectedComponents<T>(graph); // currently, we just compute connected components
		return cc.getAsSubgraphs();
	}

	/* (non-Javadoc)
	 * @see jdrasil.algorithms.preprocessing.Preprocessor#glueDecompositions()
	 */
	@Override
	protected TreeDecomposition<T> glueDecompositions() {
		TreeDecomposition<T> finalDecomposition = new TreeDecomposition<T>(this.graph);
		Bag<T> empty = finalDecomposition.createBag(new HashSet<>()); // add an empty bag
		
		// handle each decomposition
		for (TreeDecomposition<T> decomposition : treeDecompositions) {
			
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

}
