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

package jdrasil;

import jdrasil.algorithms.GraphSplitter;
import jdrasil.algorithms.approximation.RobertsonSeymourDecomposer;
import jdrasil.algorithms.lowerbounds.MinorMinWidthLowerbound;
import jdrasil.algorithms.preprocessing.GraphReducer;
import jdrasil.graph.Graph;
import jdrasil.graph.GraphFactory;
import jdrasil.graph.TreeDecomposition;
import jdrasil.utilities.JdrasilProperties;
import jdrasil.utilities.logging.JdrasilLogger;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * @author Max Bannach
 */
public class Approximation {

    /** Jdrasils Logger */
    private final static Logger LOG = Logger.getLogger(JdrasilLogger.getName());

    /**
     * Entry point to Jdrasil in approximation mode. The program, started with this method, will read a graph from standard
     * input and compute an tree decomposition of width at most 4k+4.
     * @param args Program arguments, see manual.
     */
    public static void main(String[] args) {

        // parsing arguments
        JdrasilProperties.parseArguments(args);

        // if Jdrasil is used as standalone, use dimacs logging
        JdrasilLogger.setToDimacsLogging();

        try {
            // read graph from stdin
            Graph<Integer> input = GraphFactory.graphFromStdin();

			/* Compute a explicit decomposition */
            long tstart = System.nanoTime();
            TreeDecomposition<Integer> decomposition = null;

            /* use reduction rules to reduce the graph */
            GraphReducer<Integer> reducer = new GraphReducer<Integer>(input);
            Graph<Integer> H = reducer.getProcessedGraph();
            if (H.getCopyOfVertices().size() == 0) {
                decomposition = reducer.getTreeDecomposition();
            } else {
                int lb = new MinorMinWidthLowerbound<>(H).call();
                if (lb < 4) lb = 4; // we know this from preprocessing

                // use the separator based decomposer, i.e., split the graph using safe seperators and decompose the atoms
                GraphSplitter<Integer> splitter = new GraphSplitter<Integer>(H, atom -> {
                    TreeDecomposition<Integer> td;
                    try { // use ExactDecomposer to handle atoms
                        td = new RobertsonSeymourDecomposer<>(atom).call();
                    } catch (Exception e) { // something went wrong, provide trivial decomposition
                        td = new TreeDecomposition<>(atom);
                        td.createBag(atom.getCopyOfVertices());
                    }
                    return td;
                }, lb);
                splitter.setTargetConnectivity(GraphSplitter.Connectivity.ATOM);

                // glue to final decomposition
                reducer.addbackTreeDecomposition(splitter.call());
                decomposition = reducer.getTreeDecomposition();
            }

            long tend = System.nanoTime();
            System.out.print(decomposition);
            System.out.println();

            LOG.info("");
            LOG.info("Tree-Width: " + decomposition.getWidth());
            LOG.info("Used " + (tend-tstart)/1000000000 + " seconds");
            LOG.info("");

        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("c Could not read the graph file.");
        } catch (Exception e) {
            System.out.println("c Error during the computation of the decomposition.");
            System.out.println();
            e.printStackTrace();
        }

    }

}
