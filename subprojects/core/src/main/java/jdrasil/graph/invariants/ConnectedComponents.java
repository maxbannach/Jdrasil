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
package jdrasil.graph.invariants;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;

import jdrasil.graph.Graph;
import jdrasil.graph.GraphFactory;

/**
 * The connected components of a graph are equivalence classes with respect to vertex relation "is reachable". 
 * This class computes connected components with a simple depth-first search, which maps vertices to numbers (which represent the connected component.
 * 
 * @author Max Bannach
 */
public class ConnectedComponents<T extends Comparable<T>> extends Invariant<T, Integer, Integer>{

	/**
	 * @param graph
	 */
	public ConnectedComponents(Graph<T> graph) {
		super(graph);
	}

	/* (non-Javadoc)
	 * @see jdrasil.graph.invariants.Invariant#computeModel()
	 */
	@Override
	protected Map<T, Integer> computeModel() {		
		Map<T, Integer> components = new HashMap<>();
		Stack<T> S = new Stack<>();
		int currentComponent = 0;
		
		// perform DFS to identify components
		for (T s : graph) {
			if (components.containsKey(s)) continue; // already visited
			S.push(s);
			components.put(s, ++currentComponent);
			while (!S.isEmpty()) {
				T v = S.pop();
				for (T w : graph.getNeighborhood(v)) {
					if (components.containsKey(w)) continue;
					S.push(w);
					components.put(w, currentComponent);
				}
			}
		}
		
		// done
		return components;
	}

	/* (non-Javadoc)
	 * @see jdrasil.graph.invariants.Invariant#computeValue()
	 */
	@Override
	protected Integer computeValue() {		
		return getModel().values().stream().max( (x,y) -> x-y ).get();
	}

	/* (non-Javadoc)
	 * @see jdrasil.graph.invariants.Invariant#isExact()
	 */
	@Override
	public boolean isExact() {
		return true;
	}
	
	/**
	 * Returns the model, i.e., the connected components, as sets of vertices (mapping the id of the component to an set of vertices).
	 * @return the connected components as map
	 */
	public Map<Integer, Set<T>> getComponents() {
		return getModel().entrySet().stream().collect(
				Collectors.groupingBy( 
						Map.Entry::getValue, 
						Collectors.mapping(Map.Entry::getKey, Collectors.toSet()) 
						));
	
	}
	
	/**
	 * Returns the model, i.e., the connected components, as sets of sets of vertices.
	 * @return the connected components as set
	 */
	public Set<Set<T>> getAsSets() {
		return getComponents().values().stream().collect(Collectors.toSet());
	}
	
	/**
	 * Returns the model, i.e., the connected components, as sets of graphs.
	 * @return the connected components as set of graphs
	 */
	public Set<Graph<T>> getAsSubgraphs() {
		return getComponents().values().stream().map( x -> GraphFactory.graphFromSubgraph(graph, x)).collect(Collectors.toSet());
	}

}
