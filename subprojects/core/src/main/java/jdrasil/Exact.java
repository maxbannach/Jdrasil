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

import jdrasil.algorithms.ExactDecomposer;
import jdrasil.graph.Graph;
import jdrasil.graph.GraphFactory;
import jdrasil.graph.TreeDecomposition;
import jdrasil.utilities.JdrasilProperties;
import jdrasil.utilities.logging.JdrasilLogger;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * Jdrasil is a program to compute a small tree-decomposition of a given graph.
 * It is developed at the Universitaet zu Luebeck in context of the PACE challenge (www.pacechallenge.wordpress.com).
 *
 * <p>
 * This class provides an entry point to the program for computing exact (optimal) tree decompositions.
 * </p>
 *
 * @author Max Bannach
 * @author Sebastian Berndt
 * @author Thorsten Ehlers
 */
public class Exact {

    /** Jdrasils Logger */
    private final static Logger LOG = Logger.getLogger(JdrasilLogger.getName());

    /**
     * Entry point to Jdrasil in exact mode. The program, started with this method, will read a graph from standard
     * input and compute an exact tree decomposition.
     * @param args
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

            /* compute an exact tree-decomposition */
            ExactDecomposer<Integer> exact = new ExactDecomposer<Integer>(input);
            decomposition = exact.call();

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
