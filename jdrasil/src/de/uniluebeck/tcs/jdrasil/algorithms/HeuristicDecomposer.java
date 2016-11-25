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
package de.uniluebeck.tcs.jdrasil.algorithms;

/**
 * HeuristicDecomposer.java
 * @author Max Bannach
 */

import sun.misc.Signal;
import de.uniluebeck.tcs.jdrasil.App;
import de.uniluebeck.tcs.jdrasil.exact.SATDecomposer;
import de.uniluebeck.tcs.jdrasil.exact.SATDecomposer.Encoding;
import de.uniluebeck.tcs.jdrasil.graph.Graph;
import de.uniluebeck.tcs.jdrasil.graph.TreeDecomposer;
import de.uniluebeck.tcs.jdrasil.graph.TreeDecomposition;
import de.uniluebeck.tcs.jdrasil.graph.TreeDecomposition.TreeDecompositionQuality;
import de.uniluebeck.tcs.jdrasil.sat.solver.GlucoseParallelSATSolver;
import de.uniluebeck.tcs.jdrasil.sat.solver.GlucoseSATSolver;
import de.uniluebeck.tcs.jdrasil.upperbounds.LocalSearchDecomposer;
import de.uniluebeck.tcs.jdrasil.upperbounds.StochasticMinFillDecomposer;

/**
 * A full algorithm to compute a tree-decomposition heuristically.
 * This class also provides signal handling as required by the PACE challenge.
 * 
 * The algorithm runs in two phases: the min-fill phase and the sat-phase.
 * In the first, the stochastic min-fill algorithm is run a certain amount of time and (as it is anytime) provides consecutive improving solutions.
 * In the second phase, the best solution of the first phase is improved by a sat-solver. This is (kind of) anytime as well, as we construct formulaes for decressing k's.
 * Experiments have shown that the worst formula is k=tw-1, and that many formulas above can be solved quickly.
 * 
 * The reaction of the algorithm two different signals depend on the phase it is in.
 * 
 * @author Max Bannach
 * @author Sebastian Berndt
 * @author Thorsten Ehlers
 */
@SuppressWarnings("restriction")
public class HeuristicDecomposer<T extends Comparable<T>> implements TreeDecomposer<T>, sun.misc.SignalHandler {

	/** Preprocessor to reduce the graph size. */
	private final ReductionRuleDecomposer<T> preprocessor;
	
	/** the graph we wish to decompose */
	private final Graph<T> graph;
	
	/** This flag is set to true, if the algorithm runs in parallel mode. */
	private final boolean parallel;
	
	/** The stochastic min-fill decomposer used in the first phase */
	private StochasticMinFillDecomposer<T> minFillDecomposer;
	
	/** The sat-decomposer used in the second phase. */
	private SATDecomposer<T> satDecomposer;

	/** a local search algorithm */
    private LocalSearchDecomposer<T> lsdDecomposer;
    
	/** current upper bound on the tree-width */
	private int ub;
	
	/** The currently best known tree-decompostion. */
	private TreeDecomposition<T> currentDecomposition;
	
	/** The different phases of the algorithm */
	private enum Phase {
		Preprocessing,
		MinFillPhase,
		SatPhase,
		LocalPhase
	}
	
	/** The current phase of the algorithm. */
	private Phase currentPhase;
	
	/**
	 * Describe the signals handled by this class.
	 * There are two events the algorithm listens to:
	 * 	a) a request to output the current best ub
	 *  b) a request to terminate and output the whole decomposition
	 */
	 enum SignalType {
		CurrentSolution("USR2"),
		Terminate("TERM");
			
		// enum construction
		private String name;
		public String getName() { return name; }
		SignalType(String name) { this.name = name; }
	}
	
	/**
	 * Default constructor to initialize data structures. 
	 * @param graph
	 * @param parallel – should the graph be decomposed in parallel?
	 */
	public HeuristicDecomposer(Graph<T> graph, boolean parallel) {
		this.parallel = parallel;
		this.graph = graph;
		this.preprocessor = new ReductionRuleDecomposer<T>(graph);		
		
		// initialize trivial tree-decomposition
		this.ub = graph.getVertices().size()-1;
				
		/* Handle systems signals */
		sun.misc.Signal.handle( new sun.misc.Signal(SignalType.CurrentSolution.getName()), this );
		sun.misc.Signal.handle( new sun.misc.Signal(SignalType.Terminate.getName()), this );
	}
	
	@Override
	public TreeDecomposition<T> call() throws Exception {
		
		/* start preprocessing phase */
		this.currentPhase = Phase.Preprocessing;
		App.log("starting preprocessing");
		if (this.preprocessor.reduce()) return preprocessor.getTreeDecomposition();
		App.log("reduced to from " + graph.getVertices().size() + " to " + this.preprocessor.getReducedGraph().getVertices().size() + " vertices");
		
		/* start the first phase */
		App.log("starting minFill phase");
		this.currentPhase = Phase.MinFillPhase;
		minFillDecomposer = new StochasticMinFillDecomposer<T>(preprocessor.getReducedGraph());
		currentDecomposition = minFillDecomposer.call();
		ub = currentDecomposition.getWidth();
				
		if(preprocessor.getReducedGraph().getVertices().size() < 60){
			/* start the second phase */
			App.log("starting SAT phase");
			this.currentPhase = Phase.SatPhase;
			if (parallel) {
				satDecomposer = new SATDecomposer<T>(preprocessor.getReducedGraph(), new GlucoseSATSolver(), Encoding.IMPROVED, 0, ub-1);
			} else {
				satDecomposer = new SATDecomposer<T>(preprocessor.getReducedGraph(), new GlucoseParallelSATSolver(), Encoding.IMPROVED, 0, ub-1);
			}
			TreeDecomposition<T> from_SAT =satDecomposer.call(); 
			if(from_SAT != null && from_SAT.getWidth() < currentDecomposition.getWidth())
				currentDecomposition =from_SAT; 
			ub = currentDecomposition.getWidth();
		}
		else{
		    /* start the alternative second phase */
			App.log("starting local search phase");
		    this.currentPhase = Phase.LocalPhase;
		    lsdDecomposer = new LocalSearchDecomposer<T>(preprocessor.getReducedGraph(),Integer.MAX_VALUE,30,minFillDecomposer.permutation);
		    TreeDecomposition<T> from_lsd = lsdDecomposer.call();
			if(from_lsd != null && from_lsd.getWidth() < currentDecomposition.getWidth())
				currentDecomposition =from_lsd; 
			ub = currentDecomposition.getWidth();
		}
		
		// done
		preprocessor.glueTreeDecomposition(currentDecomposition);
		return currentDecomposition;
	}

	@Override
	public TreeDecomposition<T> getCurrentSolution() {
		TreeDecomposition<T> newDecomposition = null;
		
		// source of current solution depends on the phase of the algorithm
		switch (currentPhase) {
		case MinFillPhase:
			newDecomposition = minFillDecomposer.getCurrentSolution();
			break;
		case SatPhase:
			newDecomposition = satDecomposer.getCurrentSolution();
			break;
		case LocalPhase:
			newDecomposition = lsdDecomposer.getCurrentSolution();
			break;
		default:			
			break;	
		}
		
		// update current solution only, if the current phase has found a new one
		if (newDecomposition != null && newDecomposition.getWidth() < currentDecomposition.getWidth()){
		    currentDecomposition = newDecomposition;
		}
		return currentDecomposition;
	}

	@Override
	public TreeDecompositionQuality decompositionQuality() {
		return TreeDecompositionQuality.Heuristic;
	}

	@Override
	public void handle(Signal arg0) {

		// depending on the signal, either output the current solution or the decomposition and terminate
		if (arg0.getName().equals(SignalType.CurrentSolution.getName())) {
			ub = Math.max(preprocessor.getTreeDecomposition().getWidth(), getCurrentSolution().getWidth());
			System.out.println(ub+1);
		} else if (arg0.getName().equals(SignalType.Terminate.getName())) {
			if(App.shouldIWrite()) {
				if (currentDecomposition == null) { // no decomposition found yet. Try to get one from minfill
                                    switch (currentPhase){
                                    case MinFillPhase:
                                        this.currentDecomposition = minFillDecomposer.getCurrentSolution();
                                        break;
                                    default:
                                        break;
                                    }
                                }
                                if (currentDecomposition == null) { // no decomposition found, output trivial decomposition
                                        this.currentDecomposition = new TreeDecomposition<T>(graph);
					this.currentDecomposition.createBag(graph.getVertices());
                                }
                                else {
					preprocessor.glueTreeDecomposition(currentDecomposition);
					System.out.println(preprocessor.getTreeDecomposition());
				}
			}
			System.exit(0);
		}
		
	}

}
