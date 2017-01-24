/*
 * Copyright (c) 2016-2017, Max Bannach, Sebastian Berndt, Thorsten Ehlers
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS
 * OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package jdrasil.algorithms;

import jdrasil.algorithms.approximation.RobertsonSeymourDecomposer;
import jdrasil.algorithms.preprocessing.GraphReducer;
import jdrasil.algorithms.preprocessing.GraphSeparator;
import jdrasil.graph.Graph;
import jdrasil.graph.GraphWriter;
import jdrasil.graph.TreeDecomposer;
import jdrasil.graph.TreeDecomposition;
import jdrasil.utilities.logging.JdrasilLogger;

import java.util.logging.Logger;

/**
 * Computes an approximate tree decomposition of width at most \(4k+4\) using the Robertson-Seymour algorithm
 * and preprocessing.
 *  *
 * @author Max Bannach
 */
public class ApproximationDecomposer<T extends Comparable<T>> implements TreeDecomposer<T> {

    /** Jdrasils Logger */
    private final static Logger LOG = Logger.getLogger(JdrasilLogger.getName());

    /** The graph that should be decomposed. */
    private Graph<T> graph;

    /**
     * Default constructor to initialize data structures.
     * @param graph – the graph that should be decomposed
     */
    public ApproximationDecomposer(Graph<T> graph) {
        this.graph = graph;
    }


    @Override
    public TreeDecomposition<T> call() throws Exception {

        // handle each connected component
        GraphSeparator<T> separator = new GraphSeparator<T>(graph);
        for (Graph<T> D : separator) {
            // reduce the graph
            GraphReducer<T> reducer = new GraphReducer<T>(D);
            for (Graph<T> reduced : reducer) {
                TreeDecomposition<T> td = new RobertsonSeymourDecomposer<T>(reduced).call();
                reducer.addbackTreeDecomposition(td);
            }
            separator.addbackTreeDecomposition(reducer.getTreeDecomposition());
        }

        return separator.getTreeDecomposition();
    }

    @Override
    public TreeDecomposition<T> getCurrentSolution() {
        return null;
    }

    @Override
    public TreeDecomposition.TreeDecompositionQuality decompositionQuality() {
        return TreeDecomposition.TreeDecompositionQuality.Approximation;
    }
}
