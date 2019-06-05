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

import jdrasil.graph.*;
import jdrasil.graph.invariants.CliqueMinimalSeparator;
import jdrasil.graph.invariants.ConnectedComponents;
import jdrasil.graph.invariants.CutVertex;
import jdrasil.graph.invariants.MinorSafeSeparator;
import jdrasil.utilities.JdrasilProperties;
import jdrasil.utilities.logging.JdrasilLogger;

import java.util.*;
import java.util.concurrent.RecursiveTask;
import java.util.function.Function;
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
public class GraphSplitter<T extends Comparable<T>> extends RecursiveTask<TreeDecomposition<T>> implements TreeDecomposer<T> {

    /** Jdrasils Logger */
    private final static Logger LOG = Logger.getLogger(JdrasilLogger.getName());

    /** Minimum number of vertices the graph has to have in order to be decomposed, otherwise we directly solve it. */
    private final int FORK_THRESHOLD = 10;

    /** Connectivity of the graph that is currently processed */
    private Connectivity mode;

    /** We may wish to separate only up to a certain point (i.g., only biconnected components) */
    private Connectivity targetConnectivity;

    /** A Java function interface that is used to map atoms to tree decompositions, i.e., the tree decomposition function. */
    private Function<Graph<T>, TreeDecomposition<T>> handleAtom;

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
        MINOR,   // graph has non of the above separators -> search (greedy) for a seperator which is a labeled clique minor
        ATOM     // graph is an atom (not decomposable by above separators) -> compute tree decomposition
    }

    /** The graph to be decomposed. */
    private Graph<T> graph;

    /** A lower bound on the tree width of the graph. */
    private int low;

    /**
     * Standard constructor. This will set the connectivity to DC, i.e., connected components will be computed.
     * @param graph
     * @param handleAtom the function that computes a tree decomposition for atoms
     */
    public GraphSplitter(Graph<T> graph, Function<Graph<T>, TreeDecomposition<T>> handleAtom) {
        this(graph, handleAtom, Connectivity.DC, Connectivity.ATOM, 0);
    }

    /**
     * Standard constructor with given lower bound. This will set the connectivity to DC, i.e., connected components will be computed.
     * @param graph
     * @param handleAtom the function that computes a tree decomposition for atoms
     * @param low a lower bound on the tree width of the graph
     */
    public GraphSplitter(Graph<T> graph, Function<Graph<T>, TreeDecomposition<T>> handleAtom, int low) {
        this(graph, handleAtom, Connectivity.DC, Connectivity.ATOM, low);
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
     * @param handleAtom the function that computes a tree decomposition for atoms
     * @param low a lower bound on the tree width of the graph
     */
    public GraphSplitter(Graph<T> graph, Function<Graph<T>, TreeDecomposition<T>> handleAtom, Connectivity connectivity, Connectivity separateUpTo, int low) {
        super();
        this.graph = graph;
        this.mode = connectivity;
        this.targetConnectivity = separateUpTo;
        this.low = low;
        this.handleAtom = handleAtom;
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

        // if the graph fits in a single bag we have neither to separate it further nor to handle it as atom
        if (graph.getCopyOfVertices().size() <= low+1) {
            LOG.info("Atom fits in a single bag");
            TreeDecomposition<T> oneBag = new TreeDecomposition<T>(graph);
            oneBag.createBag(graph.getCopyOfVertices());
            return oneBag;
        }

        // if the graph has reached the target connectivity -> solve it
        if (mode.ordinal() >= targetConnectivity.ordinal()) mode = Connectivity.ATOM;

        // if connectivity is set to DC, we will only compute connected components (i.e., there is an empty separator).
        if (mode == Connectivity.DC && graph.getConnectedComponents().size() > 1) {
            LOG.info("separate into connected components");
            return forkOnSeparator(new HashSet<>(), Connectivity.CC);
        } else if (mode == Connectivity.DC) {
            mode = Connectivity.CC;
        }

        // if the graph is small, we will just solve it
        if (graph.getCopyOfVertices().size() < FORK_THRESHOLD) mode = Connectivity.ATOM;

        // if the graph is connected, we search for biconnected components, that is, we search a separator of size 1
        // such separators are safe since they are cliques
        if (mode == Connectivity.CC) {
            LOG.info("searching a separator of size one");
            T cutVertex = new CutVertex<>(graph).getValue();
            if (cutVertex == null) { // graph is biconnected
                mode = Connectivity.BCC;
                return compute(); // recursive with new mode
            } else { // just fork on the cut vertex, he is a safe separator
                LOG.info("found " + cutVertex);
                HashSet<T> S = new HashSet<T>();
                S.add(cutVertex);
                return forkOnSeparator(S, Connectivity.CC);
            }
        }

        // if the graph is biconnected, we search triconnected components, that is, we search a separator of size 2
        // such separators are safe somce they are almost cliques
        if (mode == Connectivity.BCC) {
            LOG.info("searching a separator of size two");
            Set<T> S = new HashSet<T>();
            for (T c1 : graph) { // guess a cut vertex
                S.clear();
                S.add(c1);
                T c2 = new CutVertex<>(graph, S).getValue(); // find second cut vertex
                if (c2 != null) { // found 2-vertex-separator
                    S.add(c2);
                    LOG.info("found " + S);
                    return forkOnSeparator(S, Connectivity.BCC);
                }
            }
            // not found a cut -> graph is triconnected
            mode = Connectivity.TCC;
            return compute(); // recursive with new mode
        }

        // if the graph is triconnected, we may search a separator of size 3, i.e., computing 4-connected components
        // note that these separators are safe because we assume the graph has tree width at least 4 (preprocessing)
        if (mode == Connectivity.TCC && graph.getNumVertices() <= 200) {
            LOG.info("searching a separator of size three");
            Set<T> S = new HashSet<T>();
            for (T c1 : graph) { // guess first cut vertex
                for (T c2 : graph) { // guess second cut vertex
                    if (c1.compareTo(c2) < 0) continue;
                    S.clear();
                    S.add(c1);
                    S.add(c2);
                    T c3 = new CutVertex<>(graph,S).getValue(); // compute third cut vertex
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
                        LOG.info("found " + S);
                        return forkOnSeparator(S, Connectivity.TCC);
                    }
                }
            }
            // not found a cut -> no safe separator of size 3, go on and search for minimal clique separators
            mode = Connectivity.CLIQUE;
            return compute(); // recursive with new mode
        } else if (mode == Connectivity.TCC) {
            mode = Connectivity.CLIQUE; // tcc is expansive, may skip directly to clique minimal separators
        }

        // We found all safe separators of size 0,1,2,3 so far. We will now search for clique minimal separators
        if (mode == Connectivity.CLIQUE) {
            LOG.info("searching a clique minimal separator");
            Set<T> S = new CliqueMinimalSeparator<>(graph).getSeparator();
            if (S != null) {
                LOG.info("found: " + S);
                return forkOnSeparator(S, Connectivity.CLIQUE);
            }
            // if we do not found one, we may search for almost clique minimal separators
            mode = Connectivity.ACLIQUE;
            return compute(); // recursive with new mode
        }

        // we found all clique minimal separators, we may now search for almost clique minimal separators
        if (mode == Connectivity.ACLIQUE && graph.getNumVertices() <= 200) {
            LOG.info("searching an almost clique minimal separator");
            Set<T> S = new HashSet<T>();
            for (T c1 : graph) { // guess first cut vertex (the "almost" part of the almost clique
                S.clear();
                S.add(c1);
                Set<T> clique = new CliqueMinimalSeparator<>(graph, S).getSeparator();
                if (clique == null) continue; // v is not part of an almost clique separator
                // if we found a clique separator in G\{v}, the clique + {v} is an almost clique separator
                clique.add(c1);

                // almost clique separators are only safe if they are inclusion minimal, that is, if each component
                // of G[V\S] is full
                boolean isSafe = true;
                for (Set<T> C : new ConnectedComponents<T>(graph, clique).getAsSets()) {
                    boolean full = true;
                    for (T v : S) {
                        boolean connected = false;
                        for (T w : graph.getNeighborhood(v)) {
                            if (C.contains(w)) { connected = true; break; }
                        }
                        if (!connected) { full = false; break; }
                    }
                    if (!full) { isSafe = false; break; }
                }
                if (!isSafe) continue;

                LOG.info("found: " + clique);
                return forkOnSeparator(clique, Connectivity.ACLIQUE);
            }
            // if we do not found one, we may search for labeled-minor separators
            mode = Connectivity.MINOR;
            return compute(); // recursive with new mode
        } else if (mode == Connectivity.ACLIQUE) {
            mode = Connectivity.MINOR; // almost clique minimal separator is expansive, we may skip it
        }

        // we also have found almost minimal clique separators, we not search for more general safe separators, i.e.,
        // separators which are clique labeled minors
        if (mode == Connectivity.MINOR) {
            LOG.info("searching a minor-safe separator");
            Set<T> S = new MinorSafeSeparator<>(graph).getSeparator();
            if (S != null) {
                LOG.info("found: " + S);
                return forkOnSeparator(S, Connectivity.MINOR);
            }
            // found none, done with splitting
            mode = Connectivity.ATOM;
            return compute(); // recursive with new mode
        }

        // no further separation possible -> decompose the atom using the provided function
        LOG.info("Handle atom of size " + graph.getCopyOfVertices().size());
        return handleAtom.apply(graph);
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
            GraphSplitter<T> task = new GraphSplitter<>(C, handleAtom, connectivity, targetConnectivity, low);
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
}
