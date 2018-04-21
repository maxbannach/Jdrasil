package jdrasil.workontd;

import jdrasil.algorithms.SmartDecomposer;
import jdrasil.algorithms.postprocessing.NiceTreeDecomposition;
import jdrasil.graph.Bag;
import jdrasil.graph.Graph;
import jdrasil.graph.TreeDecomposition;
import jdrasil.utilities.logging.JdrasilLogger;

import java.util.HashSet;
import java.util.Set;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class implements a \emph{general framework} for executing dynamic programs over (nice) tree decompositions.
 * Starting with a provided tree decomposition or with a decomposition computed by \JClass{SmartDecomposer}, this class will
 * optimize the tree decomposition and transform it into a nice one. Then it will traverse the decomposition in
 * post-order and simulate a stack machine while doing so. On leaf bags, the stack machine will push new
 * \JClass{StateConfigurations} using a provided \JClass{StateVectorFactory}. For all other bag types, the stack machine
 * will only use the top of the stack and the methods of \JClass{StateVector}.
 *
 * Note that the \emph{actual} program that will be executed is encoded in the \JClass{StateVector} class, which is provided
 * to the stack machine in form the the given \JClass{StateVectorFactory}. All operations performed by this class
 * universally apply to all dynamic programs over tree decompositions.
 *
 * @author Max Bannach
 */
public class DynamicProgrammingOnTreeDecomposition<T extends Comparable<T>> {

    /* Jdrasils Logger */
    private final static Logger LOG = Logger.getLogger(JdrasilLogger.getName());

    /**
     * The leaf factory is used to create new (usually empty) state vectors for
     * leaf bags.
     */
    private StateVectorFactory<T> leafFactory;

    /**
     * Indicates if the program runs on a \emph{very} nice tree decomposition, that is, a nice
     * tree decomposition that also uses edge bags.
     */
    private boolean worksOnVeryNiceTreeDecomposition;

    /**
     * The graph on which we actually work.
     */
    private Graph<T> graph;

    /**
     * The tree decomposition on which the dynamic program runs.
     */
    private TreeDecomposition<T> treeDecomposition;

    /**
     * The nice (or very nice) tree decomposition.
     */
    private NiceTreeDecomposition<T> niceTreeDecomposition;

    /**
     * The tree width of the nice (or very nice) tree decomposition.
     */
    private int tw;

    /**
     * While the tree decomposition is processed in a post-order traversal, we store the results of the individual bags
     * on a stack, which simulates the ``bubble-up'' of the dynamic program.
     */
    private Stack<StateVector<T>> stateVectorStack;

    /**
     * Initialize a new instance of a program that runs over a tree decomposition. The given leaf factory is used to
     * produce state vectors of the dynamic program for leafs. Note that this implies all other state vectors and, thus,
     * is essentially the program code.
     *
     * The program will be executed on a nice tree decomposition (which does not have edge bags).
     *
     * @param graph The graph for which the program shall be executed.
     * @param leafFactory A StateVectorFactory that generates (usually empty) state vectors for leafs.
     */
    public DynamicProgrammingOnTreeDecomposition(Graph<T> graph,
                                                 StateVectorFactory<T> leafFactory) {
        this(graph, leafFactory, false);
    }

    /**
     * Initialize a new instance of a program that runs over a tree decomposition. The given leaf factory is used to
     * produce state vectors of the dynamic program for leafs. Note that this implies all other state vectors and, thus,
     * is essentially the program code.
     *
     * The program may be executed on a \emph{very} nice tree decomposition (which does have edge bags).
     *
     * @param graph The graph for which the program shall be executed.
     * @param leafFactory A StateVectorFactory that generates (usually empty) state vectors for leafs.
     * @param veryNice Indicates if the program should be executed on a \emph{very} nice tree decomposition.
     */
    public DynamicProgrammingOnTreeDecomposition(Graph<T> graph,
                                                 StateVectorFactory<T> leafFactory, boolean veryNice) {
        this(graph, leafFactory, veryNice, null);
    }

    /**
     * Initialize a new instance of a program that runs over a tree decomposition. The given leaf factory is used to
     * produce state vectors of the dynamic program for leafs. Note that this implies all other state vectors and, thus,
     * is essentially the program code.
     *
     * The program may be executed on a \emph{very} nice tree decomposition (which does have edge bags).
     *
     * The given tree decomposition will be transformed to an optimized nice (or very nice) tree decomposition on which
     * the program then is executed. The given tree decomposition may be null, in which case a new one will be computed.
     *
     * @param graph The graph for which the program shall be executed.
     * @param leafFactory A StateVectorFactory that generates (usually empty) state vectors for leafs.
     * @param veryNice Indicates if the program should be executed on a \emph{very} nice tree decomposition.
     * @param treeDecomposition A given tree decomposition on which the program shall run.
     */
    public DynamicProgrammingOnTreeDecomposition(Graph<T> graph,
                                                 StateVectorFactory<T> leafFactory, boolean veryNice,
                                                 TreeDecomposition<T> treeDecomposition) {
        this.leafFactory = leafFactory;
        this.worksOnVeryNiceTreeDecomposition = veryNice;
        this.graph = graph;
        this.treeDecomposition = treeDecomposition;
        initialize();
    }

    /**
     * Initialize the dynamic program. This will
     * \begin{enumerate}
     *  \item Compute a tree decomposition if no one is given;
     *  \item Make the decomposition nice or very nice;
     *  \item Heuristically optimize the decomposition.
     * \end{enumerate}
     */
    private void initialize() {
        LOG.info("Start initialization of dynamic program.");
        this.stateVectorStack = new Stack<>();
        if (this.treeDecomposition == null) {
            try {
                this.treeDecomposition = new SmartDecomposer<T>(graph).call();
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "Dynamic Program could not be exectued because there was an error during the computation of the tree decomposition.", e);
            }
        }
        this.niceTreeDecomposition = new NiceTreeDecomposition<>(treeDecomposition, this.worksOnVeryNiceTreeDecomposition);
        this.treeDecomposition = niceTreeDecomposition.getProcessedTreeDecomposition(); // actually compute nice tree decomposition
        this.tw = this.treeDecomposition.getWidth();
        LOG.info("Initialization of dynamic program completed.");
    }

    /**
     * Runs the dynamic program and ``bubbles-up'' state vectors from the leafs up towards the root.
     * Once all state vectors are computed, the state vector of the root is returned.
     *
     * @return The state vector computed for the root.
     */
    public StateVector<T> run() {

        // traverse the tree decomposition in post-order
        Set<Bag<T>> visited = new HashSet<>();
        Stack<Bag<T>> stack = new Stack<>();
        stack.push(niceTreeDecomposition.getRoot()); // start at the root of the tree decomposition

        // post-order DFS
        while (!stack.isEmpty()) {
            Bag<T> v = stack.peek();
            if (visited.contains(v)) {
                stack.pop();
                handleBag(v);
                continue;
            }
            visited.add(v);
            for (Bag<T> w : treeDecomposition.getNeighborhood(v)) {
                if (visited.contains(w)) continue;
                stack.push(w);
            }
        }

        // done, top of the stack is result for the root
        return stateVectorStack.pop();
    }

    /**
     * Handle a bag of the nice tree decomposition. This functions assumes that the children of the bag where
     * already handled and stored on the state vector stack.
     *
     * @param bag The bag to be processed.
     */
    private void handleBag(Bag<T> bag) {
        StateVector<T> sv;
        switch (niceTreeDecomposition.bagType.get(bag)) {
            case LEAF:
                stateVectorStack.push(leafFactory.createStateVectorForLeaf(tw));
                break;
            case INTRODUCE:
                sv = stateVectorStack.pop();
                sv = sv.introduce(bag, niceTreeDecomposition.specialVertex.get(bag), niceTreeDecomposition.treeIndex);
                stateVectorStack.push(sv);
                break;
            case FORGET:
                sv = stateVectorStack.pop();
                sv = sv.forget(bag, niceTreeDecomposition.specialVertex.get(bag), niceTreeDecomposition.treeIndex);
                stateVectorStack.push(sv);
                break;
            case JOIN:
                sv = stateVectorStack.pop();
                sv.join(bag, stateVectorStack.pop(), niceTreeDecomposition.treeIndex);
                stateVectorStack.push(sv);
                break;
            case EDGE:
                sv = stateVectorStack.pop();
                sv.edge(bag, niceTreeDecomposition.specialVertex.get(bag),
                        niceTreeDecomposition.secondSpecialVertex.get(bag),
                        niceTreeDecomposition.treeIndex);
                stateVectorStack.push(sv);
                break;
        }

        // eventually reduce the state vector
        sv = stateVectorStack.peek();
        if (sv.shouldReduce(bag, niceTreeDecomposition.treeIndex)) sv.reduce(bag, niceTreeDecomposition.treeIndex);
    }

}
