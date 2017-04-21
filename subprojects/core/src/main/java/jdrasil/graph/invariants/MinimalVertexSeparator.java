package jdrasil.graph.invariants;

import jdrasil.graph.Graph;
import jdrasil.graph.GraphFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * This class implements an algorithm to compute a minimal vertex separator between two sets \(S_A\), \(S_B\) in a given
 * subgraph \(G[W]\). The size of the separator can be bounded by a variable \(k\), i.e., we will find the smallest
 * separator less or equal to \(k\) or report that no such separator exists.
 *
 * Their is a special case when the separator is empty as this could mean that there is no separator or that there is actually
 * a separator of size 0. To distinguish these cases, getValue will return -1 if no separator was found.
 *
 * In detail, this class implements a bounded version of the Ford-Fulkerson algorithm running in time \(O(k(n+m))\).
 *
 * @author Max Bannach
 */
public class MinimalVertexSeparator<T extends Comparable<T>> extends Invariant<T, Integer, Boolean>  {

    /** The vertices of the subgraph in which we solve the problem. */
    private BitSet W;

    /** The first vertex set */
    private BitSet SA;

    /** The second vertex, i.e., the one which we want to separate A */
    private BitSet SB;

    /** The maximal size of a separator we search */
    private int k;

    /** The separator we try to compute */
    private BitSet separator;

    /** Indicates if the model is actually a separator. */
    private boolean isSeparator;

    /** Indicates whether or not the found separator is disjoint to SA and SB or not. */
    public void setDisjointToS(boolean disjointToS) {
        this.disjointToS = disjointToS;
    }

    /** Indicates whether or not the found separator is disjoint to SA and SB or not. */
    private boolean disjointToS;

    /** Bijection from V to {0,...,n-1} */
    private final Map<T, Integer> vertexToInt;
    private final Map<Integer, T> intToVertex;

    /**
     * Standard constructor initialize the data structures. Given is the graph in which the separator is searched, a set \(W\)
     * that defines a subgraph in which we search, two sets \(S_A\) and \(S_B\) that we wish to separate, and a value \(k\)
     * which is an upper bound on the separator size, i.e., if no separator of size at most \(k\) is found, the class will
     * stop and return null.
     * @param graph
     * @param W
     * @param SA
     * @param SB
     * @param k
     */
    public MinimalVertexSeparator(Graph<T> graph, Set<T> W, Set<T> SA, Set<T> SB, int k) {
        super(graph);
        vertexToInt = new HashMap<T, Integer>();
        intToVertex = new HashMap<Integer, T>();
        int i = 0;
        for (T v : graph) {
            vertexToInt.put(v, i);
            intToVertex.put(i,v);
            i = i+1;
        }
        BitSet Wb = new BitSet(i);
        for (T v : W) Wb.set(vertexToInt.get(v));
        this.W = Wb;
        BitSet Ab = new BitSet(i);
        for (T v : SA) Ab.set(vertexToInt.get(v));
        this.SA = Ab;
        BitSet Bb = new BitSet(i);
        for (T v : SB) Bb.set(vertexToInt.get(v));
        this.SB = Bb;
        this.k = k;
        setDisjointToS(false);
    }

    /**
     * Same as @see jdrasil.graph.invariants.MinimalVertexSeparator#MinimalVertexSeparator(jdrasil.graph.Graph, java.util.Set, java.util.Set, java.util.Set, int)
     * but with sets represented by BitSets. To this end, a bijection from \(V\) to \(\{0,\dots,n-1\}\) is required, too.
     * @param graph
     * @param vertexToInt
     * @param intToVertex
     * @param W
     * @param SA
     * @param SB
     * @param k
     */
    public MinimalVertexSeparator(Graph<T> graph, Map<T, Integer> vertexToInt, Map<Integer, T> intToVertex, BitSet W, BitSet SA, BitSet SB, int k) {
        super(graph);
        this.vertexToInt = vertexToInt;
        this.intToVertex = intToVertex;
        this.W = W;
        this.SA = SA;
        this.SB = SB;
        this.k = k;
        setDisjointToS(false);
    }

    //Mark: Invariant interface

    @Override
    protected Map<T, Boolean> computeModel() {
        separator = getSeparator(W, SA, SB, k);
        isSeparator = separator != null ? true : false;
        Map<T, Boolean> model = new HashMap<T, Boolean>();
        for (T v : graph) model.put(v, separator != null && separator.get(vertexToInt.get(v)));
        return model;
    }

    @Override
    protected Integer computeValue() {
        if (!isSeparator) return -1;
        return (int) getModel().entrySet().stream().filter( x -> x.getValue() ).count();
    }

    @Override
    public boolean isExact() { return true; }

    /**
     * Getter for the model as BitSet.
     * @return
     */
    public BitSet getSeparatorAsBitSet() {
        if (getValue() == -1) return null;
        return separator;
    }

    /**
     * Getter for the model in Set form.
     * @return
     */
    public Set<T> getSeparatorAsSet() {
        if (getValue() == -1) return null;
        return getModel().entrySet().stream().filter( x -> x.getValue() ).map( x -> x.getKey()).collect(Collectors.toSet());
    }

    //Mark: bounded Ford-Fulkerson

    /**
     * This method tries to compute a separator that disconnected \(S_A\) and \(S_B\) in \(G[w]\)
     * and has size at most \(k\). If a separator of this size is found, it is returned, other wise
     * this method will return null.
     *
     * In detail, this method implements a restricted version of ford-folkerson with running time
     * \(O(k\cdot(n+m))=O(k^2n)\).
     *
     * @param W the subgraph in which we search the separator
     * @param SA first vertex set we wish to separate
     * @param SB second vertex set we wish to separate
     * @return a separator separating \(S_A\) and \(S_B\) in \(G[W]\) or null, if non of size \(k+1\) exists
     */
    private BitSet getSeparator(BitSet W, BitSet SA, BitSet SB, int k) {

        // redisual graph of ford-fulkerson
        Map<Integer, Map<Integer, Integer>> weights = new HashMap<>();
        Graph<Integer> residual = constructResidualGraph(W, weights);
        int flow = 0; // the flow we compute
        int n = graph.getCopyOfVertices().size();

        // store the separation of the graph
        HashSet<Integer> visited = new HashSet<>();
        HashMap<Integer, Integer> prev = new HashMap<>();

        // ford-fulkerson
        while (true) {
            // find a path from X to Y with BFS
            Queue<Integer> Q = new LinkedList<>();
            visited.clear();
            prev.clear();
            Integer p = null; // end point of the path

            // initialize bfs queue
            for (int v = SA.nextSetBit(0); v >= 0; v = SA.nextSetBit(v+1)) {
                Q.offer(v);
                visited.add(v);
                prev.put(v, v);
            }

            // search graph until path to Y is found (or graph is exhausted)
            bfs: while (!Q.isEmpty()) {
                Integer v = Q.poll();
                for (Integer w : residual.getNeighborhood(v)) {
                    if (visited.contains(w)) continue; // vertex already used
                    visited.add(w);
                    Q.offer(w);
                    prev.put(w, v);
                    if (SB.get(w)) {
                        p = w;
                        break bfs;
                    }
                }
            }

            // no augmenting path left, exit
            if (p == null) break;

            // augmenting path found, update flow
            flow = flow + 1;

            // if the flow exceeds k, the size of the separator does so as well -> we can cancel
            if (flow == k + 1) return null;

            // update residual graph
            while (!prev.get(p).equals(p)) {

                // decrease flow by one
                weights.get(prev.get(p)).put(p, weights.get(prev.get(p)).get(p)-1);
                if (weights.get(prev.get(p)).get(p) == 0) {
                    residual.removeDirectedEdge(prev.get(p), p);
                }

                // increase flow
                if (!weights.get(p).containsKey(prev.get(p))) {
                    weights.get(p).put(prev.get(p), 0);
                }
                weights.get(p).put(prev.get(p), weights.get(p).get(prev.get(p))+1);
                if (weights.get(p).get(prev.get(p)) == 1) {
                    residual.addDirectedEdge(p, prev.get(p));
                }

                // walk the path
                p = prev.get(p);
            }
        }

        /*
         * If we reach this point, the vertex-flow between SA and SB is at most k and the residual graph
         * represents a corresponding partition of the graph.
         * We can now extract the separator from it.
         */

        // the separator that separates the two sets
        BitSet separator = new BitSet();

        // go through the visited vertices, that are the vertices reachable by augmenting paths
        for (Integer v : visited) {
            if (v < n && visited.contains(v+n)) continue;
            if (v >= n && visited.contains(v-n)) continue;
            separator.set(v < n ? v : v-n);
        }

        // done
        return separator;
    }

    /**
     * Construct a residual graph for a minimum vertex-cut. This will create a (Integer) graph of the given subgraph,
     * in which each vertex \(v\) is replaced by \(v_{in}\rightarrow v_{out}\), i.e., the standard construction for
     * computing vertex-disjoint paths.
     * @param W the subgraph that we are looking at
     * @param weights a reference to a hashmap, which will later contain the weights of the residual graph
     * @return residual graph that can be used to find vertex-disjoint paths
     */
    private Graph<Integer> constructResidualGraph(BitSet W, Map<Integer, Map<Integer, Integer>> weights) {

        // the residual graph in construction, and its edge weights
        Graph<Integer> residual = GraphFactory.emptyGraph();
        weights.clear();
        int n = graph.getCopyOfVertices().size();

        // copy vertices that are needed
        for (T v : graph) {
            int id = vertexToInt.get(v);
            if (!W.get(id)) continue; // just copy vertices of the subgraph
            residual.addVertex(id); // in-vertex
            residual.addVertex(n + id); // out-vertex
            residual.addDirectedEdge(id, n + id);
            weights.put(id, new HashMap<>());
            weights.put(n + id, new HashMap<>());

            if (disjointToS && (SA.get(id) || SB.get(id)) ) { // if SA / SB should not be part of the separator weight them with "infinity"
                weights.get(id).put(n + id, Integer.MAX_VALUE/2);
            } else { // other edges just have unit weights
                weights.get(id).put(n + id, 1);
            }
        }

        // copy edges
        for (T v : graph) {
            if (!W.get(vertexToInt.get(v))) continue;
            for (T w : graph.getNeighborhood(v)) {
                if (!W.get(vertexToInt.get(w))) continue;
                int vid = vertexToInt.get(v);
                int wid = vertexToInt.get(w);

                // edge from v_out to w_in
                residual.addDirectedEdge(vid+n, wid);
                weights.get(vid+n).put(wid, Integer.MAX_VALUE/2); // somewhat infinity
            }
        }

        // done
        return residual;
    }

}
