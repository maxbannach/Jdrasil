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
package jdrasil;

import jdrasil.algorithms.ExactDecomposer;
import jdrasil.algorithms.HeuristicDecomposer;
import jdrasil.graph.Graph;
import jdrasil.graph.GraphFactory;
import jdrasil.graph.GraphWriter;
import jdrasil.graph.TreeDecomposition;
import jdrasil.sat.Formula;
import jdrasil.utilities.JdrasilProperties;
import jdrasil.utilities.RandomNumberGenerator;
import jdrasil.utilities.logging.JdrasilLogger;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * Jdrasil is a program to compute a small tree-decomposition of a given graph.
 * It is developed at the Universitaet zu Luebeck in context of the PACE challenge (www.pacechallenge.wordpress.com).
 * 
 * @author Max Bannach
 * @author Sebastian Berndt
 * @author Thorsten Ehlers
 */
public class App {

	/** Jdrasils Logger */
	private final static Logger LOG = Logger.getLogger(JdrasilLogger.getName());

	/** Version of the program. */
	private static final float VERSION = 1f;

	/**
	 * Remember if the result has been written already
	 */
	private static boolean resultWritten;
	
	/**
	 * Entry point!
	 * @param args program arguments
	 */
	public static void main(String[] args) {

		// parsing arguments
		parseArguments(args);

		// if Jdrasil is used as standalone, use dimacs logging
		JdrasilLogger.setToDimacsLogging();
		
		// initialize the random number generator
		if (JdrasilProperties.containsKey("s")) {
			RandomNumberGenerator.seed(Long.parseLong(JdrasilProperties.getProperty("s")));
		}
		
		try{
			Graph<Integer> input = GraphFactory.graphFromStdin();
			
			/* Compute a explicit decomposition */
			long tstart = System.nanoTime();
			TreeDecomposition<Integer> decomposition = null;	

			if (JdrasilProperties.containsKey("heuristic")) {
				
				/* compute a tree-decomposition heuristically */				
				HeuristicDecomposer<Integer> heuristic = new HeuristicDecomposer<Integer>(input);
				decomposition = heuristic.call();
				
			} else {
				
				/* Default case: compute an exact tree-decomposition */	
				ExactDecomposer<Integer> exact = new ExactDecomposer<Integer>(input);				
				decomposition = exact.call();
			}
			long tend = System.nanoTime();
			
					
			/* present the result */
			if (JdrasilProperties.containsKey("tikz")) {
				GraphWriter.writeTikZ(input);
				System.out.println();
				GraphWriter.writeTreeDecompositionTikZ(decomposition);
			} else {
				if(shouldIWrite()) {
					System.out.print(decomposition);
					System.out.println();
				}
			}
			LOG.info("");
			LOG.info("Tree-Width: " + decomposition.getWidth());
			LOG.info("Used " + (tend-tstart)/1000000000 + " seconds");
			LOG.info("");
			
		} 
		catch (IOException e) {
			e.printStackTrace();
			System.out.println("c Could not read the graph file.");			
		}
		catch (Exception e) {
			System.out.println("c Error during the computation of the decomposition.");
			System.out.println();
			e.printStackTrace();
		}
	}

	/**
	 * Parsing the programs argument and store them in parameter map.
	 * @param args the arguments of the program
	 */
	public static void parseArguments(String[] args) {
		for (int i = 0; i < args.length; i++) {
			String a = args[i];
			if (a.charAt(0) == '-') {
				
				// help is a special case
				if (a.equals("-h")) {
					printHelp();
					continue;
				} 
				
				// catch format errors
				if (a.length() < 2 || (a.length() == 2 && i == args.length-1)) {
					System.err.println("Error parsing arguments.");
					System.exit(-1);
				}
				
				if (a.length() == 2) { // arguments of length one are followed by a value
					JdrasilProperties.setProperty(a.substring(1, a.length()), args[i+1]);
				} else { // others are just flags
					JdrasilProperties.setProperty(a.substring(1, a.length()), "");
				}
			}
		}
	}
	
	public static synchronized boolean shouldIWrite(){
		boolean rVal = true;
		if(resultWritten){
			rVal = false;
		}
		resultWritten = true;
		return rVal;
	}
	
	/**
	 * This method should be used by any-time-algorithms to report whenever they found a new solution.
	 * This is a requirement by the PACE challenge.
	 * @param tw new upper bound on the tree width found
	 */
	public static void reportNewSolution(int tw) {
		if (!JdrasilProperties.containsKey("heuristic")) return; // only for heuristic
		System.out.println("c status " + (tw+1) + " " + System.currentTimeMillis());
	}
	
	/**
	 * Print a static help message.
	 */
	public static void printHelp() {
		System.out.println("Jdrasil");
		System.out.println("Authors: Max Bannach, Sebastian Berndt, and Thorsten Ehlers");
		System.out.println("Version: " + VERSION);
		System.out.println();
		System.out.println("Parameters:");
		System.out.println("  -h : print this dialog");
		System.out.println("  -s <seed> : set a random seed");
		System.out.println("  -parallel : enable parallel processing");
		System.out.println("  -heuristic : compute a heuristic solution");
		System.out.println("  -log : enable log output");
		System.out.println("  -tikz : enable tikz output");
	}

}
