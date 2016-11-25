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
package jdrasil.lowerbounds;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import jdrasil.App;
import jdrasil.graph.Graph;

/**
 * It is a well known fact that for every minor H of G the following holds: tw(H) <= tw(G). To obtain
 * a lowerbound on the tree-width of G it is thus sufficient to find good lowerbounds for minors of G.
 * The minor-min-width heuristic devoloped by Gogate and Dechter for the QuickBB algorithm 
 * (see "A complete Anytime Algorithm for Treewidth") does exactly this. It computes a lowerbound for 
 * a minor of G and tries heuristically to find a good minor for this task.
 * 
 * @param <T>
 * @author Max Bannach
 */
public class MinorMinWidthLowerbound<T extends Comparable<T>> implements Lowerbound<T>, Serializable {


	private static final long serialVersionUID = -7729782858493633708L;

	/** The graph for which we wish to find a lowerbound. */
	private final Graph<T> graph;
	
	/** Source of randomness. */
	private final Random dice;
	
	/**
	 * The algorithm is initialized with a graph that should be decomposed and a seed for randomness.
	 * @param graph
	 */
	public MinorMinWidthLowerbound(Graph<T> graph) {
		this.graph = graph.copy();
		this.dice = App.getSourceOfRandomness();
	}
	
	/**
	 * The algorithm is initialized with a graph that should be decomposed and a seed for randomness.
	 * @param graph
	 * @param dice
	 */
	public MinorMinWidthLowerbound(Graph<T> graph, Random dice) {
		this.graph = graph.copy();
		this.dice = dice;
	}
	
	@Override
	public Integer call() throws Exception {	 
		int lb = 0;
		
		// as long as the graph has vertices we can still contract some
		while (graph.getVertices().size() > 0) {
			
			// search vertex of min degree
			int min = Integer.MAX_VALUE;
			List<T> nextV = new LinkedList<>();
			for (T v : graph) {
				int deg = graph.getNeighborhood(v).size();
				if (deg < min) {
					min = deg;
					nextV.clear();
					nextV.add(v);
				} else if (deg == min) { // break ties randomly
					nextV.add(v);
				}
			}
			T v = nextV.size() > 0 ? nextV.get(dice.nextInt(nextV.size())) : null;
			
			// search min degree neighbor of nextV
			min = Integer.MAX_VALUE;
			List<T> nextU = new LinkedList<T>();
			for (T u : graph.getNeighborhood(v)) {
				int deg = graph.getNeighborhood(u).size();
				if (deg < min) {
					min = deg;
					nextU.clear();
					nextU.add(u);
				} else if (deg == min) { // break ties randomly
					nextU.add(u);
				}
			}
			T u = nextU.size() > 0 ? nextU.get(dice.nextInt(nextU.size())) : null;
			
			// update lowerbound			
			lb = Math.max(lb, graph.getNeighborhood(v).size());
				
			// no edge left to contract
			if (v == null || u == null) break;
			
			// contract edge
			graph.contract(v, u);
		}
		
		return lb;
	}

	@Override
	public Integer getCurrentSolution() {
		return null;
	}
	
}
