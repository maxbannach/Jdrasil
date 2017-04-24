package jdrasil.graph.invariants;

import jdrasil.algorithms.upperbounds.GreedyPermutationDecomposer;
import jdrasil.graph.Bag;
import jdrasil.graph.Graph;
import jdrasil.graph.GraphFactory;
import jdrasil.graph.TreeDecomposition;
import jdrasil.utilities.RandomNumberGenerator;
import jdrasil.utilities.logging.JdrasilLogger;

import java.util.*;
import java.util.logging.Logger;

/**
 * A separator \(S\) is safe for tree width, if for each connected component \(C\) of \(G[V\setminus S]\) the tree width
 * of the graph\(G[C\cup S]\) with \(S\) as clique is bounded by the tree width of \(G\).
 *
 * Bodlaender and Koster ("Safe separators for treewidth") have shown that a separator \(S\) is safe in these terms if
 * for each component \(C\) in \(G[V\setminus S]\) the graph \(R=G[V\setminus C]\) contains a clique on the vertices of
 * \(S\) as labeled minor. We call such \(S\) minor-safe.
 *
 * This class implements a heuristic to find a minor-safe separator. Whenever the heuristic outputs a separator, it will
 * be guaranteed that it actually is a minor-safe separator, however, if the heuristic does not find such a separator there
 * still could be one contained in \(G\).
 *
 * @author Max Bannach
 */
public class MinorSafeSeparator<T extends Comparable<T>> extends Invariant<T, Integer, Boolean> {

    /** Jdrasils logger */
    private final static Logger LOG = Logger.getLogger(JdrasilLogger.getName());

    /** The safe separator that we try to compute. */
    private Set<T> safeSeparator;

    /**
     * The constructor of the invariant for a fixed graph G.
     * This method will just initialize some data structures, the computation happens the first time @see getModel()
     * or @see getValue() is called.
     *
     * @param graph
     */
    public MinorSafeSeparator(Graph<T> graph) {
        super(graph);
    }

    /**
     * Greedily searches a minor-safe separator by the following heuristic:
     *
     * 1) Compute a set of potential safe separators. This is done by computing a tree decomposition with a heuristic
     *    (edges correspond to separators in the graph).
     * 2) For each separator check if it is safe by the following heuristic: Contract edges in the remaining graph in order
     *    to turn S into a clique.
     *
     * @return
     */
    private Set<T> searchMinorSafeSeparator() throws Exception {

        // Compute a set of potential minor-safe separators by extracting separators from a heuristic tree decomposition
        List<Set<T>> potentialSeparators = new LinkedList<>();
        TreeDecomposition<T> td = new GreedyPermutationDecomposer<T>(graph).call();
        for (Bag<T> b1 : td.getBags()) {
            for (Bag<T> b2 : td.getTree().getNeighborhood(b1)) {
                if (b1.id >= b2.id) continue;
                if (b1.vertices.containsAll(b2.vertices)) continue;
                if (b2.vertices.containsAll(b1.vertices)) continue;

                Set<T> S = new HashSet<>();
                S.addAll(b1.vertices);
                S.retainAll(b2.vertices);

                // check if the separator actually is minor-safe
                if (minorSafe(S)) return S;
            }
        }

        // no minor-safe separator was found
        return null;
    }

    /**
     * Checks if the given separator is minor-safe, that is, if a clique on S is contained in G[V\C] for each component C
     * in G[V\S]. This method is greedy and makes false-negative errors, i.e., if S is not minor-safe this will always be
     * detected, however, if S is minor-safe this method may return false.
     *
     * @param S
     * @return
     */
    private boolean minorSafe(Set<T> S) {

        // compute connected components separators by S
        Set<Set<T>> components = new ConnectedComponents<T>(graph, S).getAsSets();

        // check if the condition is satisfied for every component
        for (Set<T> C : components) {
            Set<T> R = new HashSet<>();
            R.addAll(graph.getVertices());
            R.removeAll(C);
            Graph<T> tmp = GraphFactory.graphFromSubgraph(graph, R);

            // iterate over all edges {v,w} in G[V\C] that are not connected to S
            for (T v : graph.getVertices()) {
                if (C.contains(v) || S.contains(v)) continue;
                for (T w : graph.getNeighborhood(v)) {
                    if (S.contains(w) || v.compareTo(w) >= 0) continue;
                    if (tmp.getVertices().contains(v) && tmp.getVertices().contains(w)) tmp.contract(v,w);
                }
            }

            // iterate over all non-edges {v,w} in S
            for (T v : S) {
                for (T w : S) {
                    if (tmp.isAdjacent(v, w) || v.compareTo(w) >= 0) continue;
                    List<T> common = new LinkedList<>();
                    common.addAll(tmp.getNeighborhood(v));
                    common.retainAll(tmp.getNeighborhood(w));
                    common.removeAll(S);
                    if (common.size() == 0) return false; // found a component in which we can not turn S into a clique
                    T x = common.get(RandomNumberGenerator.nextInt(common.size()));
                    if (RandomNumberGenerator.nextBoolean()) {
                        tmp.contract(v,x);
                    } else {
                        tmp.contract(w,x);
                    }
                }
            }

        }

        // we found labeled minors for each component -> S is safe
        return true;
    }

    @Override
    protected Map<T, Boolean> computeModel() {
        try {
            this.safeSeparator = searchMinorSafeSeparator();
        } catch (Exception e) {
            return null;
        }

        // extract the model
        Map<T, Boolean> model = new HashMap<>();
        if (this.safeSeparator != null)
            for (T v : graph.getVertices()) model.put(v, this.safeSeparator.contains(v));
        return model;
    }

    @Override
    protected Integer computeValue() {
        return this.safeSeparator == null ? 0 : this.safeSeparator.size();
    }

    @Override
    public boolean isExact() { return false; }

    /**
     * Getter for the actual safe separator.
     * @return
     */
    public Set<T> getSeparator() {
        if (getValue() == 0) return null; // also invokes eventual computation
        return this.safeSeparator;
    }

}
