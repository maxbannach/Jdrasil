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
import java.util.stream.Stream;

import jdrasil.graph.Graph;
import jdrasil.graph.TreeDecomposition;
import jdrasil.utilities.JdrasilProperties;

/**
 * A Preprocessor is a function that maps an arbitrary input graph to an "easier" graph.
 * Easier here is meant with respect to computing a tree decomposition and often just means "smaller", but 
 * could also reefer to adding structures to the graph that improve pruning potential.
 * 
 * This class models a preprocessor by providing the methods @see preprocessGraph(), @see addbackTreeDecomposition(), @see computeTreeDecomposition(), and
 * @see Preprocessor#getTreeDecomposition() . The first method represents the actual preprocessing and computes a smaller graph from the input graph.
 * The following two methods can be used to add a tree decomposition of the reduced graph produced by the first method back, and to use this
 * tree decomposition to create one for the input graph. The last method is a getter for this decomposition.
 * 
 * @author Max Bannach
 */
public abstract class Preprocessor<T extends Comparable<T>> {

	/** The graph that is preprocessed. */
	protected Graph<T> graph;
	
	/** The graph computed by the algorithm. */
	protected Graph<T> processedGraph;
	
	/** TreeDecomposition of the easier Graph. */
	protected TreeDecomposition<T> processedTreeDecomposition;
	
	/** The tree decomposition of the input graph, computed by the preprocessor. */
	protected TreeDecomposition<T> treeDecomposition;
	
	/**
	 * The constructor just initialize some internal data structures and stores the graph that should be preprocessed.
	 * @param graph that should be preprocessed
	 */
	public Preprocessor(Graph<T> graph) {
		this.graph = graph;
		this.treeDecomposition = new TreeDecomposition<T>(graph);
		this.processedGraph = null;
	}
	
	/**
	 * This method actually computes the preprocessing. It will use @see graph as input, and produce 
	 * an easier (or smaller) graph.
	 * @return the set of smaller graphs
	 */
	protected abstract Graph<T> preprocessGraph();
	
	/**
	 * This method should compute a tree decomposition of the input graph, using the tree decomposition of the
	 * the smaller graphs (@see processedTreeDecomposition). This method will automatically be called, if
	 * the tree decompositions of the smaller graph is added.
	 * This method should not be called manually.
	 * 
	 * @return tree decomposition of the input graph
	 */
	protected abstract TreeDecomposition<T> computeTreeDecomposition();

	/**
	 * Get the preprocessed graph. If the graph was not computed yet, this method will invoke the preprocessing algorithm,
	 * i.e., @see jdrasil.algorithms.preprocessing.Preprocessor#preprocessGraph()
	 * @return
	 */
	public Graph<T> getProcessedGraph() {
		if (this.processedGraph == null) this.processedGraph = preprocessGraph();
		return processedGraph;
	}

	/**
	 * Add a tree decomposition of a graph produced by this class back.
	 * This method will call @see computeTreeDecomposition()
	 * @param decomposition the decomposition to be added back
	 */
	public void addbackTreeDecomposition(TreeDecomposition<T> decomposition) {
		this.processedTreeDecomposition = decomposition;
		this.treeDecomposition = computeTreeDecomposition();
	}
	
	/**
	 * Get a tree decomposition of the input graph, if available, otherwise null will be returned.
	 * The decomposition is available, if @see addbackTreeDecomposition() was called.
	 * @return the tree decomposition
	 */
	public TreeDecomposition<T> getTreeDecomposition() {
		return this.treeDecomposition;
	}

}
