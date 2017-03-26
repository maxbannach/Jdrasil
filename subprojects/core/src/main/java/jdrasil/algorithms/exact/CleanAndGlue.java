package jdrasil.algorithms.exact;

import jdrasil.algorithms.upperbounds.StochasticGreedyPermutationDecomposer;
import jdrasil.graph.*;
import jdrasil.utilities.BitSetTrie;
import jdrasil.utilities.logging.JdrasilLogger;

import java.util.*;
import java.util.logging.Logger;

/**
 * Created by bannach on 23.03.17.
 */
public class CleanAndGlue<T extends Comparable<T>> implements TreeDecomposer<T> {

    /** Jdrasils Logger */
    private final static Logger LOG = Logger.getLogger(JdrasilLogger.getName());

    /** The graph we wish to decompose. */
    private BitSetGraph graph;

    /** The number of vertices of the graph. */
    private int n;

    /**
     * Queue of cleaned subgraphs that can be further processed.
     * We we will work on big subgraphs first, as we wish to quickly clean the whole graph.
     */
    private PriorityQueue<BitSet> queue;

    /** Memorization of subgraphs that where already added to the queue. */
    private Set<BitSet> memory;

    /** Also store elements added to the queue in a trie to make supersets queries. */
    private BitSetTrie memoryTrie;

    /** For each vertex we store a collection of subgraphs that has v as neighbor. */
    private Map<Integer, BitSetTrie> tries;

    /** Each element added to the queue is glued from one or more previous cleaned subgraphs  */
    private Map<BitSet, BitSet[]> from;

    /**
     * Initialize data structures and transform the graph into a BitSetGraph.
     * @param graph
     */
    public CleanAndGlue(Graph<T> graph) {
        this.graph      = new BitSetGraph(graph);
        this.n          = this.graph.getN();
        this.queue      = new PriorityQueue<>( (a,b) -> Integer.compare(b.cardinality(), a.cardinality()) );
        this.memory     = new HashSet<>();
        this.memoryTrie = new BitSetTrie();
        this.from       = new HashMap<>();
        this.tries      = new HashMap<>();
    }

    /**
     * Checks if the last searcher move (yielding S by placing searchers on move) was valid in the sense that it
     * does not requires more then k+1 searchers.
     * If the move was valid and it is the first time we see S, S will be added to the queue as cleaned subgraph
     * and can be further processed.
     *
     * This method also checks if the move finishes the cleaning process and, if so, returns true (we can cancel the
     * search in this case, as we found a cleaning strategy). Otherwise this method returns always false (when we insert S
     * in the queue as well as if we do not).
     *
     * Finally, this method will also compute and store the corresponding bag for S and store in the bags map.
     * This bag can be used to extract a tree decomposition in case we find a cleaning strategy and S is part of it.
     *
     * @param S the subgraph that should be offered
     * @param k tree width that is tested
     * @param from array of cleaned graphs from which the current subgraph was glued (may be at least one)
     * @return
     */
    private boolean offer(BitSet S, int k, BitSet... from) {
        if (memory.contains(S)) return false; // already handled
//        if (memoryTrie.getSuperSets(S).iterator().hasNext()) return false;
        BitSet neighbors = graph.exteriorBorder(S);
        BitSet delta = (BitSet) S.clone();
        for (BitSet f : from) delta.andNot(f);
        if (neighbors.cardinality() + delta.cardinality() > k + 1) return false; // not enough searchers

        // we will add S, store how we have glued it
        this.from.put(S, from);

        // maybe the whole graph is already cleaned -> we found a decomposition of size k
        if (S.cardinality() >= n - k - 1) {
            // create extra bag for remaining vertices
            if (S.cardinality() < n) {
                BitSet all = new BitSet();
                all.set(0, n);
                this.from.put(all, new BitSet[]{S});
            }
            return true;
        }

        // register new cleaned subgraph
        queue.offer(S);
        memory.add(S);
        memoryTrie.insert(S);

        // done, but have to decompose further
        return false;
    }

    /**
     * Tries do decompose the given graph into a tree decomposition of width k.
     * This method will use k+1 searchers to clean the graph in the following way: iteratively growing subgraphs are cleaned
     * by teams of size k+1 (corresponding to the classic path decomposition cleaning game). Additionally, cleaned subgraphs
     * may be glued together to create bigger cleaned subgraphs (corresponding to branch nodes in the tree decomposition).
     *
     * Cleaned subgraphs that should be further processed are stored in a priority queue that delivers the biggest subgraph.
     * This increases the change that we quickly find a cleaning strategy for the whole graph.
     *
     * @param k
     * @return
     */
    private boolean decompose(int k) {
        // init data structures
        queue.clear();
        memory.clear();
        memoryTrie = new BitSetTrie();
        from.clear();
        tries.clear();
        for (int v = 0; v < n; v++) tries.put(v, new BitSetTrie());

        // pre-fill the queue with subgraphs that can be cleaned trivially
        for (int v = 0; v < n; v++) {
            BitSet S = new BitSet();
            S.set(v);
            graph.saturate(S);
            if (offer(S, k)) return true;
        }

        // handle the queue
        while (!queue.isEmpty()) {
            // get currently largest cleaned graph
            BitSet S = queue.poll();
            BitSet delta = graph.exteriorBorder(S);

            // handle the neighbors of S
            for (int v = delta.nextSetBit(0); v >= 0; v = delta.nextSetBit(v+1)) {

                // 1. add S to the trie of v
                tries.get(v).insert(S);

                // 2. try to extend S by placing a searcher on v
                BitSet newS = (BitSet) S.clone();
                newS.set(v);
                graph.saturate(newS);
                if (offer(newS, k, S)) return true;

                // 3. try to glue S to other cleaned subgraphs
                BitSet mask = (BitSet) S.clone();
                mask.flip(0, n); // search in V\S
                mask.andNot(delta);
                for (BitSet toGlue : tries.get(v).getMaxSubSets(mask)) {
                    newS = (BitSet) toGlue.clone();
                    BitSet glueDelta = graph.exteriorBorder(newS);
                    glueDelta.or(delta);
                    if (glueDelta.cardinality() > k+1) continue; // not enough searchers to glue
                    // glue with S
                    newS.or(S);
                    graph.saturate(newS);
                    if (offer(newS, k, S, toGlue)) return true;

                    // glue with S and add v
                    newS = (BitSet) newS.clone();
                    newS.set(v);
                    graph.saturate(newS);
                    if (offer(newS, k, S, toGlue)) return true;
                }
            }
        }

        // queue empty -> failed to clean -> tree width is larger then k
        return false;
    }

    /**
     * Extract a tree decomposition from a cleaning strategy of the searchers.
     * Should be called after a run of @see decompose that has returned true
     * @param S
     * @param td
     * @return
     */
    private Bag<T> extractTreeDecomposition(BitSet S, TreeDecomposition<T> td) {
        // 1. create current bag
        BitSet bagVertices = (BitSet) S.clone();
        for (BitSet f : from.get(S)) bagVertices.andNot(f); // reduce to delta
        bagVertices.or(graph.exteriorBorder(S));            // add neighbors
        Bag<T> bag = td.createBag(graph.getVertexSet(bagVertices));

        // 2. compute children and glue to them
        for (BitSet child : from.get(S)) {
            Bag<T> childBag = extractTreeDecomposition(child, td);
            td.addTreeEdge(bag, childBag);
        }

        // 3. return bag
        return bag;
    }

    @Override
    public TreeDecomposition<T> call() throws Exception {

        int k = 1;
        while (!decompose(k)) {
            LOG.info("tree width >= " + k);
            k++;
        }
        LOG.info("tree width == " + k);

        // extract constructed tree decomposition
        TreeDecomposition<T> td = new TreeDecomposition<T>(graph.getGraph());
        BitSet all = new BitSet();
        all.set(0,n);
        extractTreeDecomposition(all, td);

        // done
        return td;
    }

    @Override
    public TreeDecomposition<T> getCurrentSolution() { return null; }

    @Override
    public TreeDecomposition.TreeDecompositionQuality decompositionQuality() {
        return TreeDecomposition.TreeDecompositionQuality.Exact;
    }
}
