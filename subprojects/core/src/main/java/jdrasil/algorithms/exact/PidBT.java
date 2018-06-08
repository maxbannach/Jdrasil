package jdrasil.algorithms.exact;

import jdrasil.graph.*;
import jdrasil.utilities.logging.JdrasilLogger;

import java.util.*;
import java.util.logging.Logger;

/**
 * An implementation of Tamakis positive-instance driven version of the algorithm from Bouchitt\'{e} and Todinca~\cite{Tamaki17}.
 * The algorithm uses the observation that many graphs seem to have just a few \emph{feasible} potential maximal cliques
 * (in contrast to the number of all such objects), which are then enumerated and used to find an optimal tree decomposition.
 */
public class PidBT<T extends Comparable<T>> implements TreeDecomposer<T> {

    /* Jdrasil Logger */
    private final static Logger LOG = Logger.getLogger(JdrasilLogger.getName());

    /* The graph that we decompose. */
    private BitSetGraph<T> graph;

    /* Lower- and upper-bounds. */
    private int lb;
    private int ub;
    private TreeDecomposition<T> ubDecomposition;

    /* Data structures used by the algorithm. */
    private Queue<BitSet> queue;
    private Queue<BitSet> pending;
    private Set<BitSet> IBlocks;
    private Set<BitSet> OBlocks;
    private Set<BitSet> buildablePMC;
    private Set<BitSet> feasiblePMC;

    /* Data structures used to reconstructed decomposition form winning-strategy. */
    private BitSet root;
    private Map<BitSet, List<BitSet>> strategy = new HashMap<>();
    private Map<BitSet, BitSet> cliqueOfComponent = new HashMap<>();

    /**
     * Initialize the algorithm for a graph without known lower- or upper-bounds.
     *
     * @param graph The graph to be decomposed.
     */
    public PidBT(Graph<T> graph) {
        this(graph, 1);
    }

    /**
     * Initialize the graph with a given lower-bound, which will be improved until a solution was found.
     * @param graph The graph to be decomposed.
     * @param lb A lower-bound on the tree width of the given graph.
     */
    public PidBT(Graph<T> graph, int lb) {
        this(graph, lb, graph.getNumVertices()-1, null);
    }

    /**
     * Initialize the graph with a given lower- and upper-bound. The lower-bound will be improved till it matches the upper-bound,
     * or till an solution was found.
     *
     * @param graph The graph to be decomposed.
     * @param lb A lower-bound on the tree width of the given graph.
     * @param ub A upper-bound on the tree width of the given graph.
     * @param ubDecomposition A decomposition witnessing the upper-bound.
     */
    public PidBT(Graph<T> graph, int lb, int ub,
                 TreeDecomposition<T> ubDecomposition) {
        this.graph = new BitSetGraph<>(graph);
        this.lb = lb;
        this.ub = ub;
        this.ubDecomposition = ubDecomposition;

        // initialize data structures for core algorithm
        queue = new PriorityQueue<>( (a,b) -> Integer.compare(b.cardinality(), a.cardinality()) );
        pending = new LinkedList<>();
        IBlocks = new HashSet<>();
        OBlocks = new HashSet();
        buildablePMC = new HashSet<>();
        feasiblePMC  = new HashSet<>();

        // initialize data structures to store decomposition
        strategy = new HashMap<>();
        cliqueOfComponent = new HashMap<>();
    }

    /**
     * Get OBlocks that are super sets of the given set.
     * @param S A set $S$.
     * @return A list of super sets of $S$ stored in the OBlocks.
     */
    private List<BitSet> getSuperSets(BitSet S) {
        List<BitSet> supersets = new LinkedList<>();
        for (BitSet D : OBlocks) {
            BitSet mask = (BitSet) S.clone(); // D\S
            mask.andNot(D);
            if (mask.cardinality() != 0) continue;
            supersets.add(D);
        }
        return supersets;
    }

    /**
     * Refresh the data structures for a new run of the core algorithm. Returns the next intex to which the algorithm
     * should be tested.
     *
     * @param lb The lower-bound that was tested in the last run.
     * @return A target width that should be tested next.
     */
    private int refresh(int lb) {
        queue.clear();
        pending.clear();
        queue.addAll(IBlocks);
        //IBlocks.clear();
        OBlocks.clear();
        //buildablePMC.clear();
        //feasiblePMC.clear();
        return lb + 1;
    }

    /**
     * The algorithm has found a place move of the cops and announces it~--~this information is later used to extract
     * the tree decomposition from a winning strategy of the cops.
     *
     * @param K A potential maximal clique.
     */
    private void announcePlaceMove(BitSet K) {
        List<BitSet> next = new LinkedList<>();
        next.add(K);
        strategy.put(graph.outlet(K), next);
    }

    /**
     * The algorithm has found a remove move of the cops and announces it~--~this information is later used to extract
     * the tree decomposition from a winning strategy of the cops.
     *
     * @param K A potential maximal clique.
     */
    private void announceRemoveMove(BitSet K) {
        Set<BitSet> next = new HashSet<>();
        for (BitSet A : graph.support(K)) next.add(cliqueOfComponent.get(A));
        strategy.put(K, new LinkedList<>(next));
    }

    /**
     * Insert the corresponding IBlock of the given potential maximal clique $K$ into the main queue. Eventually find
     * a move of the cops and announce it.
     *
     * @param K A potential maximal clique.
     */
    private void insert(BitSet K) {
        BitSet outlet = graph.outlet(K);
        if (outlet.cardinality() == 0) return;
        BitSet C = graph.crib(outlet, K);
        if (IBlocks.contains(C)) return;
        announcePlaceMove(K);
        cliqueOfComponent.put(C, K);
        IBlocks.add(C);
        queue.offer(C);
        LOG.finer("offered: " + C);
    }

    /**
     * Poll an IBlock from the main queue.
     * @return A IBLock to be processed next.
     */
    private BitSet poll() {
        BitSet C = queue.poll();
        LOG.finer("polled: " + C);
        return C;
    }

    /**
     * If a potential maximal clique is processed but not ready (it has inbound components that are not identified as IBlock yet),
     * we have to process it at a later point again. This method stores the potential maximal clique to the pending queue,
     * if necessary.
     *
     * @param K A potential maximal clique.
     */
    private void eventuallyPostponePotentialMaximalClique(BitSet K) {
        boolean isReady = true;
        for (BitSet block : graph.separate(K)) {
            if (graph.outbound(block)) continue;
            if (IBlocks.contains(block)) continue;
            isReady = false;
            break;
        }
        if (!isReady) {
            LOG.finer("pending: " + K);
            pending.offer(K);
        }
    }

    /**
     * Process a buildable potential maximal clique that was discovered by the algorithm. This method eventually inserts
     * an element to the main queue.
     *
     * If the processed potential maximal clique has no outlet, a tree decomposition of the target width was found.
     *
     * @param K A potential maximal clique.
     * @return True if a solution was found.
     */
    private boolean processPotentialMaximalClique(BitSet K) {
        if (feasiblePMC.contains(K)) return false;
        // check if the PMC is feasible
        boolean feasible = true;
        for (BitSet D : graph.support(K)) feasible &= IBlocks.contains(D);
        if (!feasible) return false;
        feasiblePMC.add(K);
        announceRemoveMove(K);

        // if the outlet is empty, we found the root and a proof for tw = k
        if (graph.outlet(K).cardinality() == 0) {
            root = K;
            return true;
        }

        // insert crib of K
        insert(K);
        return false;
    }

    /**
     * Tamakis core algorithm that tests if the tree width of the input graph is at most $k$.
     * If this method is invoked an returns true, a tree decomposition can be extracted from the stored strategy.
     *
     * @param k The target tree width.
     * @return True if the input graph has tree width at most $k$.
     */
    private boolean solve(int k) {

        // initialize
        for (int v = 0; v < graph.getN(); v++) {
            BitSet K = (BitSet) graph.getBitSetGraph()[v].clone();
            K.set(v);
            if (K.cardinality() > k + 1) continue;
            if (!graph.isPotentialMaximalClique(K)) continue;
            buildablePMC.add(K);
            if (!graph.support(K).isEmpty()) continue;
            feasiblePMC.add(K);
            insert(K);
        }

        // main loop
        while (true) {
            while (!queue.isEmpty()) {
                BitSet C = poll();

                // temporary data
                Set<BitSet> newOBlocks = new HashSet<>();
                Set<BitSet> newBuildablePMC = new HashSet<>();

                // (iii)
                for (BitSet B : getSuperSets(C)) {
                    BitSet K = (BitSet) graph.exteriorBorder(B).clone();
                    K.or(graph.computeExteriorBorder(C));
                    if (K.cardinality() > k + 1) continue;
                    if (graph.isPotentialMaximalClique(K)) newBuildablePMC.add(K);
                    if (K.cardinality() > k) continue;
                    for (BitSet A : graph.fullComponents(K)) newOBlocks.add(A);
                }

                // (iv)
                for (BitSet A : graph.fullComponents(graph.exteriorBorder(C))) newOBlocks.add(A);

                // (v)
                for (BitSet A : newOBlocks) {
                    BitSet NA = graph.exteriorBorder(A);
                    for (int v = NA.nextSetBit(0); v >= 0; v = NA.nextSetBit(v+1)) {
                        BitSet K = (BitSet) A.clone();
                        K.and(graph.getBitSetGraph()[v]);
                        K.or(NA);
                        if (K.cardinality() > k + 1) continue;
                        if (graph.isPotentialMaximalClique(K)) newBuildablePMC.add(K);
                    }
                }

                // (vi)
                for (BitSet K : newBuildablePMC) {
                    eventuallyPostponePotentialMaximalClique(K);
                    if (processPotentialMaximalClique(K)) return true;
                }

                // copy temporary data
                OBlocks.addAll(newOBlocks);
                buildablePMC.addAll(newBuildablePMC);
            }
            if (pending.isEmpty()) break;
            while (!pending.isEmpty()) {
                BitSet K = pending.poll();
                if (processPotentialMaximalClique(K)) return true;
            }
        }

        // not solution found
        return false;
    }

    /**
     * Turns a winning-strategy of the cops into a tree decomposition.
     *
     * @param root The root of the tree decomposition (or the current bag).
     * @param treeDecomposition The tree decomposition that is constructed.
     * @return The bag that was created for the given root set.
     */
    private Bag<T> extractDecompositionFromStrategy(BitSet root,
                                       TreeDecomposition<T> treeDecomposition) {
        Bag<T> bag = treeDecomposition.createBag(graph.getVertexSet(root));
        if (!strategy.containsKey(root)) return bag;
        for (BitSet child : strategy.get(root)) treeDecomposition.addTreeEdge(bag, extractDecompositionFromStrategy(child, treeDecomposition));
        return bag;
    }

    @Override
    public TreeDecomposition<T> call() throws Exception {
        LOG.info("running PID-BT core on graph with |V| = " + graph.getN() + " and |E| = " + graph.getGraph().getNumberOfEdges());
        LOG.info("current lb = " + lb);
        LOG.info("current ub = " + ub);

        // increase lower bound till optimum is found
        while ( (lb < ub) && !solve(lb) ) {
            LOG.info("tw > " + lb);
            lb = refresh(lb);
        }
        LOG.info("tw = " + lb);

        // some debug information
        LOG.info("#IBlocks: " + IBlocks.size());
        LOG.info("#OBlocks: " + OBlocks.size());
        LOG.info("#BuildablePMC: " + buildablePMC.size());
        LOG.info("#FeasiblePMC: " + feasiblePMC.size());

        // we did nothing
        if (lb == ub && ubDecomposition != null) return ubDecomposition;

        // build the decomposition
        TreeDecomposition<T> treeDecomposition = new TreeDecomposition<>(graph.getGraph());
        if (lb == graph.getN()-1) { // trivial case -> single bag
            treeDecomposition.createBag(graph.getGraph().getCopyOfVertices());
        } else { // extract optimal tree decomposition from the strategy
            extractDecompositionFromStrategy(root, treeDecomposition);
        }

        // done
        return treeDecomposition;
    }

    @Override
    public TreeDecomposition<T> getCurrentSolution() {
        return null;
    }

    @Override
    public TreeDecomposition.TreeDecompositionQuality decompositionQuality() {
        return TreeDecomposition.TreeDecompositionQuality.Exact;
    }

}