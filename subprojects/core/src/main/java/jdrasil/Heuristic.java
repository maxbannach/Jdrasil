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
import jdrasil.algorithms.HeuristicDecomposer;
import jdrasil.graph.Graph;
import jdrasil.graph.GraphFactory;
import jdrasil.graph.TreeDecomposition;
import jdrasil.utilities.JdrasilProperties;
import jdrasil.utilities.logging.JdrasilLogger;
import sun.misc.Signal;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * Jdrasil is a program to compute a small tree-decomposition of a given graph.
 * It is developed at the Universitaet zu Luebeck in context of the PACE challenge (www.pacechallenge.wordpress.com).
 *
 * <p>
 * This class provides an entry point to the program for computing heuristical tree decompositions (no guaranteed or
 * bound is provided).
 * </p>
 *
 * <p>
 *     Unless the parameter -instant is given, the program will run until it explicitly receives a SIGTERM kill.
 * </p>
 *
 * @author Max Bannach
 * @author Sebastian Berndt
 * @author Thorsten Ehlers
 */
public class Heuristic implements sun.misc.SignalHandler {

    /** Jdrasils Logger */
    private final static Logger LOG = Logger.getLogger(JdrasilLogger.getName());

    /** The decomposer used to compute the tree decomposition. */
    private HeuristicDecomposer<Integer> heuristicDecomposer;

    /**
     * Entry point to Jdrasil in heuristic mode. The program, started with this method, will read a graph from standard
     * input and compute an heurstic decomposition.
     * @param args
     */
    public static void main(String[] args) {

        // parsing arguments
        JdrasilProperties.parseArguments(args);

        // if Jdrasil is used as standalone, use dimacs logging
        JdrasilLogger.setToDimacsLogging();

        // initialize the heuristic, this will also handle system signals
        new Heuristic();
    }

    /**
     * Constructor for the heuristic class.
     * This class use an @see jdrasil.algorithms.HeuristicDecomposer while handling system signals.
     */
    private Heuristic() {

        /* Handle systems signals */
        sun.misc.Signal.handle( new sun.misc.Signal("TERM"), this );

        try {
            // read graph from stdin
            Graph<Integer> input = GraphFactory.graphFromStdin();

			/* Compute a explicit decomposition */
            long tstart = System.nanoTime();
            TreeDecomposition<Integer> decomposition = null;

            /* compute an exact tree-decomposition */
            heuristicDecomposer = new HeuristicDecomposer<>(input);
            decomposition = heuristicDecomposer.call();

            while (true) {

                Thread.sleep(1);
                if (false) break;
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

    @Override
    public void handle(Signal arg0) {
        if (this.heuristicDecomposer == null) return;
        System.out.println(this.heuristicDecomposer.getCurrentSolution());
    }

}
