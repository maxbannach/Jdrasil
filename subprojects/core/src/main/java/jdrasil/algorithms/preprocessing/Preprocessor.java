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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import jdrasil.graph.Graph;
import jdrasil.graph.TreeDecomposition;

/**
 * A Preprocessor is a function that maps an arbitrary input graph to a collection of "easier" graphs. 
 * Easier here is meant with respect to computing a tree decomposition and often just means "smaller", but 
 * could also reefer to adding structures to the graph that improve pruning potential.
 * 
 * This class models a preprocessor by providing the methods @see computeGraphs(), @see addbackTreeDecomposition(), @see glueDecompositions(), and
 * @see Preprocessor#getTreeDecomposition() . The first method represents the actual preprocessing and computes a collection of graphs from the input graph.
 * The following two methods can be used to add a tree decomposition of one of the graphs produced by the first method back, and to combine these
 * tree decompositions to one for the input graph. The last method is a getter for this decomposition.
 * 
 * @author Max Bannach
 */
public abstract class Preprocessor<T extends Comparable<T>> implements Iterable<Graph<T>> {

	/** The graph that is preprocessed. */
	protected Graph<T> graph;
	
	/** The graphs computed by the algorithm. */
	protected Set<Graph<T>> processedGraphs;
	
	/** TreeDecompositions of the small Graphs. */
	protected Set<TreeDecomposition<T>> treeDecompositions;
	
	/** The tree decomposition of the input graph, computed by the preprocessor. */
	protected TreeDecomposition<T> treeDecomposition;
	
	/**
	 * The constructor of an Preprocessor will invoke the computation of the preprocessing (i.e., it may be time expensive).
	 * After the construction did finish, one can access the smaller graphs over the methods implemented by this class.
	 * @param graph that should be preproccessed
	 */
	public Preprocessor(Graph<T> graph) {
		this.graph = graph;
		this.processedGraphs = computeGraphs();
		this.treeDecompositions = new HashSet<>();
		this.treeDecomposition = new TreeDecomposition<T>(graph);
	}
	
	/**
	 * This method actually computes the preprocessing. It will use @see graph as input, and produce 
	 * a (eventually singleton) set of easier (or smaller) graphs.
	 * @return the set of smaller graphs
	 */
	protected abstract Set<Graph<T>> computeGraphs();
	
	/**
	 * This method should compute a tree decomposition of the input graph, using the tree decompositions for 
	 * the small graphs (@see treeDecompositions). This method will automatically be called, if
	 * the number of small tree decompositions equals the number of small graphs, i.e., if for every graph produced by this class
	 * a corresponding tree decompositions was added.
	 * This method should not be called manually.
	 * 
	 * @return tree decomposition of the input graph
	 */
	protected abstract TreeDecomposition<T> glueDecompositions();
	
	/**
	 * Add a tree decomposition of a graph produced by this class back.
	 * This method will call @see glueDecompositions() ones enough tree decompositions where added.
	 * @param decomposition the decomposition to be added back
	 */
	public void addbackTreeDecomposition(TreeDecomposition<T> decomposition) {
		this.treeDecompositions.add(decomposition);
		if (this.treeDecompositions.size() == this.processedGraphs.size()) {
			this.treeDecomposition = glueDecompositions();
		}
	}
	
	/**
	 * Get a tree decomposition of the input graph, if available, otherwise null will be returned.
	 * The decomposition is available, if @see addbackTreeDecomposition() was called for every graph produced 
	 * by the preprocessor, i.e., after @see glueDecompositions() was called.
	 * @return the tree decomposition
	 */
	public TreeDecomposition<T> getTreeDecomposition() {
		return this.treeDecomposition;
	}
	
	//MARK: an iterator
	
	/**
	 * Returns an iterator over the graphs produced by the preprocessing.
	 * @return graph iterator
	 */
	@Override
	public Iterator<Graph<T>> iterator() {
		return new GraphIterator<T>(this);
	}
	
	/**
	 * Iterator graph to make the iterator over @see processedGraphs available.
	 * @param <Z>
	 */
	class GraphIterator<Z extends Comparable<Z>> implements Iterator<Graph<Z>> {

		private final Iterator<Graph<Z>> itr;
		
		GraphIterator(Preprocessor<Z> preprocessor) {
			itr = preprocessor.processedGraphs.iterator();
		}
		
		@Override
		public boolean hasNext() {
			return itr.hasNext();
		}

		@Override
		public Graph<Z> next() {
			return itr.next();
		}		
	}
	
}
