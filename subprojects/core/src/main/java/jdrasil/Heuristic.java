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

import jdrasil.algorithms.preprocessing.GraphReducer;
import jdrasil.algorithms.upperbounds.LocalSearchDecomposer;
import jdrasil.algorithms.upperbounds.PaceGreedyDegreeDecomposer;
import jdrasil.algorithms.upperbounds.StochasticGreedyPermutationDecomposer;
import jdrasil.graph.Graph;
import jdrasil.graph.GraphFactory;
import jdrasil.graph.TreeDecomposition;
import jdrasil.utilities.JdrasilProperties;
import jdrasil.utilities.logging.JdrasilLogger;
import sun.misc.Signal;

import java.io.IOException;
import java.util.List;
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

    /** Start and end of the computation. */
    private long tstart, tend;

    /** The graph to be decomposed. */
    private Graph<Integer> input;

    /** The decomposition in construction. */
    private TreeDecomposition<Integer> decomposition;

    /** The reducer used to preprocess the graph. */
    private GraphReducer<Integer> reducer;

    /** The stochastic greedy permutation decomposer used in the first phase */
    private StochasticGreedyPermutationDecomposer<Integer> greedyPermutationDecomposer;

    /** The local search decomposer used in the third phase */
    private LocalSearchDecomposer<Integer> localSearchDecomposer;

    public static volatile boolean shutdownFlag;

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


        shutdownFlag = false;
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
        sun.misc.Signal.handle( new sun.misc.Signal("INT"), this );
        try {
            // read graph from stdin
            input = GraphFactory.graphFromStdin();
            int upperBound = input.getNumVertices();
            boolean needsPostProcessing = false;
            List<Integer> perm = null;
            for(int i = 0 ; i < 30 && !JdrasilProperties.timeout() && !Heuristic.shutdownFlag ; i++){
                PaceGreedyDegreeDecomposer pcdd = new PaceGreedyDegreeDecomposer(input);
                TreeDecomposition<Integer> td =  pcdd.computeTreeDecomposition(upperBound);
                if(td != null && td.getWidth() < upperBound){
                    this.decomposition = td;
                    upperBound = td.getWidth();
                    if(i > 3 && upperBound < 1000)
                        break;
                }
            }
            if(!Heuristic.shutdownFlag && !JdrasilProperties.containsKey("instant")){
                /* Compute a explicit decomposition */
                tstart = System.nanoTime();

                LOG.info("reducing the graph");
                reducer = new GraphReducer<>(input);
                Graph<Integer> reduced = reducer.getProcessedGraph();
                if (reduced.getCopyOfVertices().size() > 0) {
                    LOG.info("reduced the graph to " + reduced.getCopyOfVertices().size() + " vertices");

                    // temporary tree decomposition to avoid raise conditions
                    TreeDecomposition<Integer> tmp;

                    LOG.info("Starting greedy permutation phase");
                    greedyPermutationDecomposer = new StochasticGreedyPermutationDecomposer<>(reduced);
                    //greedyPermutationDecomposer.setUpper_bound(upperBound);
                    tmp = greedyPermutationDecomposer.call();
                    synchronized (this) {
                        if(this.decomposition == null ||
                                (tmp != null && tmp.getWidth() < this.decomposition.getWidth())){
                            this.decomposition = tmp;
                            needsPostProcessing = true;
                        }
                    }
                    //                if(!JdrasilProperties.timeout() ){
                    //	                LOG.info("Improving the decomposition");
                    //	                tmp = this.decomposition.copy();
                    //	                tmp.improveDecomposition();
                    //	                synchronized (this) {
                    //	                    this.decomposition = tmp;
                    //	                }
                    //                }

                    // we may skip the local search phase
                    if (!Heuristic.shutdownFlag &&  !JdrasilProperties.timeout() &&  !JdrasilProperties.containsKey("instant")) {

                        LOG.info("Starting local search phase");
                        if(greedyPermutationDecomposer.getPermutation() != null)
                            perm = greedyPermutationDecomposer.getPermutation();
                        localSearchDecomposer = new LocalSearchDecomposer<>(reduced, Integer.MAX_VALUE, 30, perm);
                        tmp = localSearchDecomposer.call();
                        synchronized (this) {
                            if(this.decomposition == null ||
                                    (tmp != null && tmp.getWidth() < this.decomposition.getWidth())){
                                this.decomposition = tmp;
                                needsPostProcessing = true;
                            }
                        }

                    }
                }
            }
            // print and exit
            printSolution(this.decomposition, needsPostProcessing);

        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("c Could not read the graph file.");
        } catch (Exception e) {
            System.out.println("c Error during the computation of the decomposition.");
            System.out.println();
            e.printStackTrace();
        }
    }

    /**
     * This method undoes the preprocessing to create a final tree decompositions and prints this decomposition to std.out.
     * This method will exit the program.
     * @param td a tree decomposition of the reduced graph
     */
    private synchronized void printSolution(TreeDecomposition<Integer> td, boolean needsPostProcessing) {
        if(needsPostProcessing){
            if (reducer.getProcessedGraph().getCopyOfVertices().size() != 0) reducer.addbackTreeDecomposition(td);
            this.decomposition = reducer.getTreeDecomposition();
        }
        this.decomposition.connectComponents();
        tend = System.nanoTime();
        System.out.println(this.decomposition);
        LOG.info("");
        LOG.info("Tree-Width: " + decomposition.getWidth());
        LOG.info("Used " + (tend-tstart)/1000000000 + " seconds");
        LOG.info("");
        System.exit(0);
    }

    @Override
    public void handle(Signal arg0) {


        Heuristic.shutdownFlag = true;
//        // catch super early abort
//        if (input == null) {
//            LOG.warning("Did not finish reading the graph!");
//            System.exit(1);
//        }
//
//        // catch abort during reduction phase
//        if (reducer == null) {
//            LOG.warning("Did not finish reducing the graph!");
//            this.decomposition = new TreeDecomposition<>(input);
//            Bag<Integer> singleBag = this.decomposition.createBag(input.getCopyOfVertices());
//            System.out.println(this.decomposition);
//            System.exit(0);
//        }
//
//        // if local search has started, look if it has a solution
//        if (localSearchDecomposer != null) {
//            TreeDecomposition<Integer> td = localSearchDecomposer.getCurrentSolution();
//            if (td != null) this.decomposition = td;
//        }
//
//        // if we have no decomposition at all, the greedy permutation heuristic did not finish
//        // but maybe she has a subsolution?
//        if (this.decomposition == null) {
//            this.decomposition = greedyPermutationDecomposer.getCurrentSolution();
//        }
//
//        // if not, create trivial decomposition with a single bag
//        if (this.decomposition == null) {
//            Graph<Integer> H = reducer.getProcessedGraph();
//            this.decomposition = new TreeDecomposition<>(H);
//            Bag<Integer> singleBag = this.decomposition.createBag(H.getCopyOfVertices());
//        }
//
//        // print the decomposition
//        printSolution(decomposition);
    }

}