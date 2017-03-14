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
package jdrasil.algorithms;

import jdrasil.algorithms.exact.CopsAndRobber;
import jdrasil.algorithms.exact.SATDecomposer;
import jdrasil.algorithms.lowerbounds.MinorMinWidthLowerbound;
import jdrasil.algorithms.preprocessing.GraphReducer;
import jdrasil.algorithms.upperbounds.StochasticGreedyPermutationDecomposer;
import jdrasil.graph.*;
import jdrasil.graph.invariants.ConnectedComponents;
import jdrasil.sat.Formula;
import jdrasil.utilities.JdrasilProperties;
import jdrasil.utilities.logging.JdrasilLogger;

import java.util.*;
import java.util.concurrent.RecursiveTask;
import java.util.logging.Logger;

/**
 * This class implements a general divide and conquer strategy to compute tree decompositions, descriped in
 * "Safe separators for treewidth" by Bodlaender and Koster.
 *
 * In particular, it will search for separators \(S\subseteq V\) that are safe for tree width, i.e., that can be removed
 * such that the tree width of the original graph is the maximum of the resulting components. The given graph is splitted
 * with such separators as long as we can find any. An unreducible graph is called an atom and is finally processed by
 * an actual tree decomposition algorithm.
 *
 * This class implements separators of size 0 (connected components), 1 (biconnected components), 2 (triconnected components),
 * 3 (only the ones that are safe, so this are not necessarily fourconnected components). Also minimal clique and almost clique
 * separators are implemented.
 *
 * The implementation makes use of Javas RecursiveTask interface, allowing the divide phase to be done in parallel.
 * If the "parallel" flag is set in JdrasilProperties, the forks will distributed to new threads.
 *
 * @param <T>
 */
public class SafeSeparatorDecomposer<T extends Comparable<T>> extends RecursiveTask<TreeDecomposition<T>> implements TreeDecomposer<T> {

    /** Jdrasils Logger */
    private final static Logger LOG = Logger.getLogger(JdrasilLogger.getName());

    /** Minimum number of vertices the graph has to have in order to be decomposed, otherwise we directly solve it. */
    private final int FORK_THRESHOLD = 10;

    /** If the graph has less then <value> vertices, then the instance may be solved by a game of Cops and Robber. */
    private final int COPS_VERTICES_THRESHOLD = 25;

    /** If the graph has a treewidth of less then <value>, then the instance may be solved by a game of Cops and Robber. */
    private final int COPS_TW_THRESHOLD = 8;

    /** Connectivity of the graph that is currently processed */
    private Connectivity mode;

    /** We may wish to separate only up to a certain point (i.g., only biconnected components) */
    private Connectivity targetConnectivity;

    /**
     * Different connectivities that a graph may have. The connectivity mainly tells the algorithm what to do next
     * (i.e., searching for separators of size 0,1,2,3,...).
     * If the graph is marked as ATOM no separator used by this class was found, then it should be handeld directly.
     */
    public enum Connectivity {
        DC,      // graph may not be connected -> compute connected components
        CC,      // graph is connected -> compute separator of size 1
        BCC,     // graph is biconnected -> compute separator of size 2
        TCC,     // graph is triconnected -> compute separator of size 3
        CLIQUE,  // graph is triconnected and has no safe separators of size 3 -> compute clique minimal separators
        ACLIQUE, // graph has no clique minimal separator -> search for almost clique minimal separators
        ATOM     // graph is an atom (not decomposable by above separators) -> compute tree decomposition
    }

    /** The graph to be decomposed. */
    private Graph<T> graph;

    /** A lower bound on the tree width of the graph. */
    private int low;

    /**
     * Standard constructor. This will set the connectivity to DC, i.e., connected components will be computed.
     * @param graph
     */
    public SafeSeparatorDecomposer(Graph<T> graph) {
        this(graph, Connectivity.DC, Connectivity.ATOM, 0);
    }

    /**
     * Standard constructor with given lower bound. This will set the connectivity to DC, i.e., connected components will be computed.
     * @param graph
     */
    public SafeSeparatorDecomposer(Graph<T> graph, int low) {
        this(graph, Connectivity.DC, Connectivity.ATOM, low);
    }

    /**
     * Special constructor that sets a specific connectivity, that is, it is already known if the graph is connected or
     * bi- / triconnected.
     *
     * This constructor also sets a known lower bound (recursion can be stopped at components that fit into a single bag)
     *
     * The separator may only separate the graph up to a certain connectivity, this is set by the last parameter.
     * To fully separate the graph, set this to ATOM.
     *
     * @param graph
     * @param connectivity
     * @param separateUpTo
     */
    public SafeSeparatorDecomposer(Graph<T> graph, Connectivity connectivity, Connectivity separateUpTo, int low) {
        super();
        this.graph = graph;
        this.mode = connectivity;
        this.targetConnectivity = separateUpTo;
        this.low = low;
    }

    /**
     * The separator may only separate the graph up to a certain connectivity, this is set by the last parameter.
     * To fully separate the graph, set this to ATOM (default).
     *
     * From separation perspective, the target connectivity is the first for which no further separation is performed.
     * For instances, if the target connectivity is BCC (biconnected) the graph will be separated exhaustive
     * by separators of size 1 until it is biconnected, but no separator of size 2 will be computed.
     *
     * The default value of the target connectivity is ATOM, which means that the graph is separated as much as possible.
     * Setting this value will decrease the amount of separation and, thus, increase the performance. However, the solver
     * then has to handle bigger atoms, which is probably worse.
     *
     * @param targetConnectivity
     */
    public void setTargetConnectivity(Connectivity targetConnectivity) {
        this.targetConnectivity = targetConnectivity;
    }

    //MARK: TreeDecomposer methods

    @Override
    public TreeDecomposition<T> call() throws Exception {
        return compute(); // just start the recursion
    }

    @Override
    public TreeDecomposition<T> getCurrentSolution() { return null; } // not anytime

    @Override
    public TreeDecomposition.TreeDecompositionQuality decompositionQuality() {  return TreeDecomposition.TreeDecompositionQuality.Exact; }

    //MARK: RecursiveTask methods

    @Override
    protected TreeDecomposition<T> compute() {

        // if the graph has reached the target connectivity -> solve it
        if (mode.ordinal() >= targetConnectivity.ordinal()) mode = Connectivity.ATOM;

        // if connectivity is set to DC, we will only compute connected components (i.e., there is an empty separator).
        if (mode == Connectivity.DC) {
            return forkOnSeparator(new HashSet<>(), Connectivity.CC);
        }

        // if the graph is small, we will just solve it
        if (graph.getVertices().size() < FORK_THRESHOLD) mode = Connectivity.ATOM;

        // if the graph is connected, we search for biconnected components, that is, we search a separator of size 1
        // such separators are safe since they are cliques
        if (mode == Connectivity.CC) {
            T cutVertex = getCutVertex();
            if (cutVertex == null) { // graph is biconnected
                mode = Connectivity.BCC;
                return compute(); // recursive with new mode
            } else { // just fork on the cut vertex, he is a safe separator
                HashSet<T> S = new HashSet<T>();
                S.add(cutVertex);
                return forkOnSeparator(S, Connectivity.CC);
            }
        }

        // if the graph is biconnected, we search triconnected components, that is, we search a separator of size 2
        // such separators are safe somce they are almost cliques
        if (mode == Connectivity.BCC) {
            Set<T> S = new HashSet<T>();
            for (T c1 : graph) { // guess a cut vertex
                S.clear();
                S.add(c1);
                T c2 = getCutVertex(S); // find second cut vertex
                if (c2 != null) { // found 2-vertex-separator
                    S.add(c2);
                    return forkOnSeparator(S, Connectivity.CC);
                }
            }
            // not found a cut -> graph is triconnected
            mode = Connectivity.TCC;
            if (mode.ordinal() >= targetConnectivity.ordinal()) mode = Connectivity.ATOM;
        }

        // if the graph is triconnected, we may search a separator of size 3, i.e., computing 4-connected components
        // note that these separators are safe because we assume the graph tha have tree width at least 4 (preprocessing)
        if (mode == Connectivity.TCC && graph.getVertices().size() <= 200) {
            Set<T> S = new HashSet<T>();
            for (T c1 : graph) { // guess first cut vertex
                for (T c2 : graph) { // guess second cut vertex
                    if (c1.compareTo(c2) < 0) continue;
                    S.clear();
                    S.add(c1);
                    S.add(c2);
                    T c3 = getCutVertex(S); // compute third cut vertex
                    if (c3 != null) { // found 3-vertex-separator
                        S.add(c3);

                        // Not all 3-vertex-separators are safe!
                        // S is not safe <=> it does not contain an edge, it splits G in only 2 components, and one of the components as size 1
                        boolean isSafe = false;

                        // if S contains an edge it is safe (it is an almost clique or a clique)
                        if (graph.isAdjacent(c1, c2)) isSafe = true;
                        if (graph.isAdjacent(c1, c3)) isSafe = true;
                        if (graph.isAdjacent(c2, c3)) isSafe = true;

                        // S is safe if there is no vertex v with N(v)=S
                        if (!isSafe) {
                            boolean hasDegreeThreeNeighbor = false;
                            for (T v : graph) {
                                if (graph.getNeighborhood(v).size() != 3) continue;
                                if (graph.getNeighborhood(v).containsAll(S)) { hasDegreeThreeNeighbor = true; break; }
                            }
                            if (!hasDegreeThreeNeighbor) isSafe = true;
                        }

                        // if S splits the graph in more then 2 components it is safe as well
                        if (!isSafe) {
                            ConnectedComponents<T> cc = new ConnectedComponents<T>(graph, S);
                            if (cc.getValue() > 2) isSafe = true;
                        }

                        if (!isSafe) continue; // S is no safe separator
                        return forkOnSeparator(S, Connectivity.CC);
                    }
                }
            }
            // not found a cut -> no safe separator of size 3, go on and search for minimal clique separators
            mode = Connectivity.CLIQUE;
            if (mode.ordinal() >= targetConnectivity.ordinal()) mode = Connectivity.ATOM;
        }

        // We found all safe separators of size 0,1,2,3 so far. We will now search for clique minimal separators
        if (mode == Connectivity.CLIQUE) {
            Set<T> S = getCliqueMinimalSeparator();
            if (S != null) return forkOnSeparator(S, Connectivity.CC);
            // if we do not found one, we may search for almost clique minimal separators
            mode = Connectivity.ACLIQUE;
            if (mode.ordinal() >= targetConnectivity.ordinal()) mode = Connectivity.ATOM;
        }

        // we found all clique minimal separators, we may now search for almost clique minimal separators
        if (mode == Connectivity.ACLIQUE) {
            Set<T> S = new HashSet<T>();
            for (T c1 : graph) { // guess first cut vertex (the "almost" part of the almost clique
                S.clear();
                S.add(c1);
                Set<T> clique = getCliqueMinimalSeparator(S);
                if (clique == null) continue; // v is not part of an almost clique separator
                // if we found a clique separator in G\{v}, the clique + {v} is an almost clique separator
                clique.add(c1);
                return forkOnSeparator(clique, Connectivity.BCC);
            }
            // if we do not found one, we may search for almost clique minimal separators
            mode = Connectivity.ATOM;
            if (mode.ordinal() >= targetConnectivity.ordinal()) mode = Connectivity.ATOM;
        }

        // no further separation possible -> decompose the atom
        return handleAtom();
    }

    //MARK: Decomposition methods

    /**
     * Given a separator \(S\) (which is assumed to be safe for tree width), this method will split the graph at \(S\) and
     * fork on the connected components of \(G[V\S]\), to which \(S\) is added as a clique. The recursively computed
     * tree decompositions will glue on a new bag containing only \(S\).
     *
     * This method may handle the forks in parallel, if the "parallel" flag is set in JdrasilProperties.
     *
     * The connectivity flag can be used by the caller to define which kind of separator should be computed in the next
     * recursion (this method will just pipe it).
     *
     * @param S
     * @param connectivity
     * @return
     */
    private TreeDecomposition<T> forkOnSeparator(Set<T> S, Connectivity connectivity) {
        LOG.info("Forking on cut of size: " + S.size());

        // 1. compute connected components of G[V\S]
        Set<Graph<T>> components = new ConnectedComponents<T>(graph, S).getAsSubgraphs();

        // 2. add the separator S as clique to each component
        for (Graph<T> C : components) {
            for (T v : S) { C.addVertex(v); } // add vertices
            for (T v : S) { // add original edges
                for (T w : graph.getNeighborhood(v)) {
                    if (C.containsNode(w)) C.addEdge(v, w);
                }
            }
            for (T v : S) { // make S a clique
                for (T w : S) {
                    if (v.compareTo(w) < 0) C.addEdge(v, w);
                }
            }
        }

        // 3. fork on the obtained components and recursively compute tree decompositions for them
        List<RecursiveTask<TreeDecomposition<T>>> tasks = new ArrayList<>();
        for (Graph C : components) {
            SafeSeparatorDecomposer<T> task = new SafeSeparatorDecomposer<>(C, connectivity, targetConnectivity, low);
            tasks.add(task);
            if (JdrasilProperties.containsKey("parallel")) { // either handle children parallel or sequential
                task.fork();
            } else {
                task.invoke();
            }
        }

        // 4. join the recursively computed tree decompositions
        TreeDecomposition<T> finalDecomposition = new TreeDecomposition<T>(this.graph);
        Bag<T> empty = finalDecomposition.createBag(S); // add a bag for the separator, we will glue here

        // handle each decomposition
        for (RecursiveTask<TreeDecomposition<T>> task : tasks) {
            TreeDecomposition<T> decomposition = task.join();

            // compute mapping from the bags of the T to bags of the new decomposition
            Map<Bag<T>, Bag<T>> oldToNew = new HashMap<>();
            for (Bag<T> oldBag : decomposition.getBags()) {
                Bag<T> newBag = finalDecomposition.createBag(oldBag.vertices);
                oldToNew.put(oldBag, newBag);
            }

            // map edges
            for (Bag<T> s : decomposition.getBags()) {
                for (Bag<T> t : decomposition.getNeighborhood(s)) {
                    if (s.compareTo(t) < 0) {
                        finalDecomposition.addTreeEdge(oldToNew.get(s), oldToNew.get(t));
                    }
                }
            }

            // find a suitable glue bag
            for (Bag<T> bag : decomposition.getBags()) {
                if (bag.containsAll(S)) { // bag contains the separator, we can glue here
                    finalDecomposition.addTreeEdge(empty, oldToNew.get(bag));
                    break;
                }
            }
        }

        // done
        return finalDecomposition;
    }

    //MARK: Hopcroft-Tarjan for computing biconnected components

    // helper variable for the DFS of Hopcroft and Tarjan
    private int count;

    /**
     * This method computes a cut vertex (aka articulation point) of the given graph using the algorithm of
     * Hopcroft and Tarjan. This implementation uses \(O(V+E)\) time, but will not output all components, but just
     * the first cut vertex it finds.
     *
     * The given set of vertices will be ignored during the search (i.e., as if they are deleted).
     *
     * @return a cut vertex if there is one
     */
    private T getCutVertex(Set<T> forbidden) {
        // search an arbitrary start vertex that is not forbidden
        T s = null;
        for (T v : graph.getVertices()) {
            if (!forbidden.contains(v)) s = v;
        }
        if (s == null) return null; // empty graph

        // start algorithm
        count = 0;
        return getCutVertex(s, s, new HashMap<T, Integer>(), new HashMap<T, Integer>(), forbidden);
    }

    /**
     * @see SafeSeparatorDecomposer#getCutVertex(Set<T>) without forbidden vertices
     * @return
     */
    private T getCutVertex() {
        return getCutVertex(new HashSet<T>());
    }

    /**
     * @see SafeSeparatorDecomposer#getCutVertex(Set<T>)
     *
     * @param u
     * @param v
     * @param low
     * @param depth
     * @param forbidden
     * @return
     */
    private T getCutVertex(T u, T v, Map<T, Integer> low, Map<T, Integer> depth, Set<T> forbidden) {

        // previsit
        count++;
        int nChildren = 0;
        low.put(v, count);
        depth.put(v, count);

        // traversal
        for (T w : graph.getNeighborhood(v)) {
            if (forbidden.contains(w)) continue; // ignore forbidden vertices
            if (!depth.containsKey(w)) { // unvisited
                nChildren++;
                T tmp = getCutVertex(v, w, low, depth, forbidden);
                if (tmp != null) return tmp; // already found one
                low.put(v, Math.min(low.get(v), low.get(w)));
                if (low.get(w) >= depth.get(v) && u != v) return v; // we found a cut vertex
            } else if (w != u && depth.get(w) < depth.get(v)) { // if we w is not the parent, update low
                low.put(v, Math.min(low.get(v), depth.get(w)));
            }
        }

        // root of getCutVertex tree is only cut vertices if it has more then 1 child
        if (u == v && nChildren > 1) return v;

        // not cut vertex found
        return null;
    }

    //MARK: compute clique minimal separator

    /**
     * Computes a clique minimal separator of the graph, that is, a set \(S\subseteq V\) such that \(G[S]\) is a clique,
     * \(G[V\setminus S]\) as more components then G, and such that \(S\) is a minimal separator for some vertices \(a,b\in V\).
     *
     * This method takes \(O(nm)\) time and implements the algorithm described in "An Introduction to Clique Minimal Separator Decomposition"
     * by Berry et al.
     * In short, it does the following:
     * a) compute a minimal triangulation of the graph
     * b) find minimal separators of the triangulation
     * c) check which of these separators are cliques in the graph
     *
     * @return
     */
    private Set<T> getCliqueMinimalSeparator() {
        return getCliqueMinimalSeparator(new HashSet<T>());
    }

    /**
     * Implementation of @see jdrasil.algorithms.SafeSeparatorDecomposer#getCliqueMinimalSeparator() but with a
     * forbidden set of vertices. The forbidden vertices can be seen "as deleted" in the graph.
     * @param forbidden
     * @return
     */
    private Set<T> getCliqueMinimalSeparator(Set<T> forbidden) {

        // create a copy of the graph (will be modifed)
        Graph<T> G = GraphFactory.copy(graph);
        for (T v : forbidden) G.removeVertex(v);
        // triangulation of G
        Graph<T> H = GraphFactory.copy(graph);

        // minimal elimination order
        List<T> alpha = new ArrayList<T>(graph.getVertices().size());

        // set of generators of minimal separators in H
        Set<T> X = new HashSet<T>();

        // initialize labels for all vertices
        Map<T, Integer> label = new HashMap<T, Integer>();
        for (T v : G) label.put(v, 0);
        int s = -1;

        // compute the elimination order
        for (int n = graph.getVertices().size()-forbidden.size(), i = 0; i < n; i++) {
            // choose a vertex with minimal label
            T x = G.getVertices().stream().max( (a,b) -> Integer.compare(label.get(a), label.get(b))).get();
            Set<T> Y = new HashSet<T>(G.getNeighborhood(x));

            // may add x to the generators
            if (label.get(x) <= s) {
                X.add(x);
            }
            s = label.get(x);

            // mark x as reached and all other vertices as unreached
            Set<T> reached = new HashSet<T>();
            reached.add(x);
            Map<Integer, Set<T>> reach = new HashMap<>();
            for (int j = 0; j < n; j++) reach.put(j, new HashSet<T>());

            // reach N(x) in G'
            for (T y : G.getNeighborhood(x)) {
                reached.add(y);
                reach.get(label.get(y)).add(y);
            }

            // compute reached vertices
            for (int j = 0; j < n; j++) {
                while (!reach.get(j).isEmpty()) {
                    // remove a vertex from reach(j)
                    T y = reach.get(j).iterator().next();
                    reach.get(j).remove(y);
                    for (T z : G.getNeighborhood(y)) {
                        if (reached.contains(z)) continue; // only consider unreached vertices
                        reached.add(z);
                        if (label.get(z) > j) {
                            Y.add(z);
                            reach.get(label.get(z)).add(z);
                        } else {
                            reach.get(j).add(z);
                        }
                    }
                }
            }

            // add triangulation edges to H
            for (T y : Y) {
                H.addEdge(x, y);
                label.put(y, label.get(y) + 1);
            }

            // update elimination order
            alpha.add(0, x);
            G.removeVertex(x);
        }

        /* H now stores a triangulation of the graph and alpha a minimal elimination order.
         * Furthermore, X stores the set of vertices which generate a minimal separator of H.
         * We can, finally, use this data to find a clique minimal separator in the original graph
         */
        for (T x : alpha) {
            // x is in X it is a minimal separator in H and it may be a clique minimal separator in G
            if (X.contains(x)) {
                // separator in H
                Set<T> S = new HashSet<T>(H.getNeighborhood(x));
                // check if it is a clique in G
                boolean isClique = true;
                testClique: for (T a : S) {
                    for (T b : S) {
                        if (a.compareTo(b) >= 0) continue;
                        if (!graph.isAdjacent(a,b)) {
                            isClique = false;
                            break testClique;
                        }
                    }
                }
                // if S is a clique in G, it is a clique minimal separator in G -> return it
                if (isClique) return S;
            }
            // remove the vertex from H
            H.removeVertex(x);
        }

        // no clique minimal separator found
        return null;
    }

    //MARK: handle atoms

    /**
     * Computes the tree decomposition of the graph by reducing it and then calling
     * @see SafeSeparatorDecomposer#handleAtom(jdrasil.graph.Graph)
     * @return
     */
    private TreeDecomposition<T> handleAtom() {
        LOG.info("handle atom of size " + graph.getVertices().size());
        GraphReducer<T> reducer = new GraphReducer<T>(graph);
        for (Graph<T> H : reducer) {
            LOG.info("reduced atom to size " + H.getVertices().size());
            reducer.addbackTreeDecomposition(handleAtom(H));
        }
        return reducer.getTreeDecomposition();
    }

    /**
     * Directly computes a tree decomposition for the given graph.
     * @return
     */
    private TreeDecomposition<T> handleAtom(Graph H) {

        // if the atom is smaller then the lower bound, just put it in a single bag
        if (H.getVertices().size() <= low + 1) {
            LOG.info("atom fits in a single bag");
            TreeDecomposition<T> onebag = new TreeDecomposition<T>(H);
            onebag.createBag(H.getVertices());
            return onebag;
        }

        // otherwise decompose it
        try {
            // first compute lower and upper bound
            int lb = new MinorMinWidthLowerbound<>(H).call();
            if (low < lb) lb = low;
            LOG.info("Computed lower bound: " + lb);
            TreeDecomposition<T> ubDecomposition = new StochasticGreedyPermutationDecomposer<T>(H).call();
            int ub = ubDecomposition.getWidth();
            LOG.info("Computed upper bound: " + ub);

            // if they match, we are done
            if (lb >= ub) {
                LOG.info("The bounds match, extract decomposition");
                return ubDecomposition;
            }

            // we we have no SAT solver, or the graph is small enough for Cops and Robber
            if (!Formula.canRegisterSATSolver() || (H.getVertices().size() <= COPS_VERTICES_THRESHOLD && ub <= COPS_TW_THRESHOLD)) {
                LOG.info("Solve with a game of Cops and Robbers");
                TreeDecomposition<T> decomposition = new CopsAndRobber<>(H).call();
                return decomposition;
            }

            // otherwise use a SAT solver
            LOG.info("Solve with a SAT solver [" + Formula.getExpectedSignature() + "]");
            TreeDecomposition<T> decomposition = new SATDecomposer<>(H, SATDecomposer.Encoding.IMPROVED, lb, ub).call();
            return decomposition;

        } catch (Exception e) { // if something went wrong, we will at least create a valid decomposition
            // create trivial tree decomposition
            LOG.warning("Something went wrong during the computation of the decomposition: fall back to trival decomposition");
            TreeDecomposition<T> trivial = new TreeDecomposition<T>(H);
            trivial.createBag(H.getVertices());
            return trivial;
        }
    }

}
