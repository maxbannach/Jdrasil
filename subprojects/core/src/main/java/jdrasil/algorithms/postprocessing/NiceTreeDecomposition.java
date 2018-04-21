package jdrasil.algorithms.postprocessing;

import jdrasil.graph.Bag;
import jdrasil.graph.Graph;
import jdrasil.graph.TreeDecomposition;

import java.util.*;

/**
 * A \emph{nice} tree decomposition is a \emph{rooted} tree decomposition in which each bag has one of the following types:
 *
 * \begin{enumerate}
 *  \item A \emph{leaf} bag has no children and stores and empty set.
 *  \item An \emph{introduce} bag has exactly one child bag and has the same content as its child up to one
 *    addional vertex (which was ``introduced'' at this bag).
 *  \item A \emph{forget} bag has exactly one child bag and contains all but one vertex of its child (it ``forgets''
 *    this vertex).
 *  \item A \emph{join} bag has two children which both have the same content as the join bag.
 *  \item An \emph{edge} bag has one child with the same content as that child and ``introduces'' one edge in that bag.
 *     It is required that every edge is introduces exactly once.
 * \end{enumerate}
 *
 * It is well known that to every tree decomposition there can be found a nice tree decomposition of the same width.
 * This class will perform this transformation for a given tree decomposition. The modifiction will be performed
 * \emph{inplace}, i.\,e., the given tree decomposition will be modified.
 *
 * @author Max Bannach
 */
public class NiceTreeDecomposition<T extends Comparable<T>> extends Postprocessor<T> {

    /**
     * The tree-index is a mapping $\phi\colon V\rightarrow\{0,\dots,\mathrm{tw}\}$ that maps every vertex of the graph
     * to an index such that no two vertices appearing in a bag share the same index. Therefore, the tree-index can be used
     * to index data structures when working on the tree decomposition.
     */
    public Map<T, Integer> treeIndex;

    /** Every bag in a nice tree decomposition has a specific type (leaf, join, introduce, forget, edge). */
    public enum BagType {
        LEAF,
        INTRODUCE,
        FORGET,
        JOIN,
        EDGE;
    }

    /**
     * Stores for every bag which type it has.
     */
    public Map<Bag<T>, BagType> bagType;

    /**
     * Introduce and forget bags also have a special vertex.
     */
    public Map<Bag<T>, T> specialVertex;

    /** Edge bags need two special vertices. */
    public Map<Bag<T>, T> secondSpecialVertex;

    /**
     * The root bag of the nice tree decomposition.
     */
    private Bag<T> root;

    /**
     * A very nice tree decomposition has edge bags as well.
     */
    private boolean isVeryNice;

    /**
     * The constructor just initializes some internal data structures and stores the tree decomposition that should
     * be postprocessed.
     *
     * @param treeDecomposition The tree decomposition to be postprocessed.
     */
    public NiceTreeDecomposition(TreeDecomposition<T> treeDecomposition) {
        this(treeDecomposition, false);
    }

    /**
     * The constructor just initializes some internal data structures and stores the tree decomposition that should
     * be postprocessed.
     *
     * @param treeDecomposition The tree decomposition to be postprocessed.
     * @param veryNice Decide whether edge bags should be computed or not.
     */
    public NiceTreeDecomposition(TreeDecomposition<T> treeDecomposition,
                                 boolean veryNice) {
        super(treeDecomposition);
        this.isVeryNice = veryNice;
    }

    @Override
    protected TreeDecomposition<T> postprocessTreeDecomposition() {
        // first simplify the decomposition
        new FlattenTreeDecomposition<T>(treeDecomposition).getProcessedTreeDecomposition();

        // find a root and root the decomposition on it
        Bag<T> suitableRoot = findSuitableRoot();
        this.root = makeNice(suitableRoot);

        // we may improve the quality of the decomposition for the DP
        optimizeDecomposition();

        // compute some meta-data
        classifyBags();
        computeTreeIndex();

        for (Bag<T> v : treeDecomposition.getBags()) if (treeDecomposition.getNeighborhood(v).size() == 0) System.out.println("ISOLATED");

        // eventually add even more details
        if (isVeryNice) computeEdgeBags();
        return treeDecomposition;
    }

    /**
     * A nice tree decomposition is computed from a chosen root bag down to the leafs. The choose of this root bag
     * may dramatically change the structure of the nice tree decomposition which may have an impact on the performance
     * of algorithms that later work on the decomposition.
     *
     * This method implements an heurictic that tries to find a ``good'' candidate for such an root.
     *
     * @return A suitable root bag from which on we can build a nice tree decomposition.
     */
    private Bag<T> findSuitableRoot() {
        return treeDecomposition.getTree().iterator().next();
    }

    /**
     * Make the tree decomposition nice starting at the given root. Note that the root bag may not be the given bag,
     * but will lead by a path of forget-bags to it.
     *
     * @param suitableRoot A bag at which we shall root the tree decomposition.
     * @return The actual root bag of the constructed nice tree decomposition.
     */
    private Bag<T> makeNice(Bag<T> suitableRoot) {

        // add a new root (roots need to be leafs)
        Graph<Bag<T>> tree = treeDecomposition.getTree();
        Bag<T> root = treeDecomposition.createBag(new HashSet<>());
        treeDecomposition.addTreeEdge(root, suitableRoot);

        // perform a DFS to add "nice" bags
        Stack<Bag<T>> stack = new Stack<>();
        Set<Bag<T>> visited = new HashSet();
        stack.push(root);

        while (!stack.isEmpty()) {
            Bag<T> v = stack.pop();
            visited.add(v);

            // compute number of non-visited neighbors
            long k = treeDecomposition.getNeighborhood(v).stream().filter( x -> !visited.contains(x) ).count();
            if (k == 0 && !v.vertices.isEmpty()) {
                // we have no children, but we are not empty too
                Bag<T> leaf = treeDecomposition.createBag(new HashSet<>());
                treeDecomposition.addTreeEdge(v, leaf);
                stack.push(v); // continue on v
            } else if (k == 1) {
                // one child -> get it
                Bag<T> w = treeDecomposition.getNeighborhood(v).stream().filter( x -> !visited.contains(x) ).findAny().get();

                // if it is the same, contract the edge and continue
                if (v.vertices.equals(w.vertices)) {
                    tree.contract(v, w);
                    stack.push(v);
                    continue;
                }

                // otherwise we have to reduce the distance
                Set<T> newBagVertices = null;
                Set<T> delta = new HashSet<>(v.vertices);
                delta.removeAll(w.vertices);
                if (delta.size() > 0) {
                    newBagVertices = new HashSet<>(v.vertices);
                    newBagVertices.remove(delta.iterator().next());
                } else {
                    delta = new HashSet<>(w.vertices);
                    delta.removeAll(v.vertices);
                    newBagVertices = new HashSet<>(v.vertices);
                    newBagVertices.add(delta.iterator().next());
                }
                Bag<T> newBag = treeDecomposition.createBag(newBagVertices);
                tree.removeEdge(v, w);
                tree.addEdge(v, newBag);
                tree.addEdge(newBag, w);
                stack.push(newBag);
            } else if (k >= 2) {
                Bag<T> left = treeDecomposition.createBag(v.vertices);
                Bag<T> right = treeDecomposition.createBag(v.vertices);
                Set<Bag<T>> neighbors = new HashSet<>();
                treeDecomposition.getNeighborhood(v).stream().filter( x -> !visited.contains(x)).forEach(x -> neighbors.add(x));
                for (Bag<T> w : neighbors) tree.removeEdge(v, w);
                tree.addEdge(v, left);
                tree.addEdge(v, right);
                int i = 0;
                for (Bag<T> w : neighbors) {
                    if (i < neighbors.size()/2) {
                        tree.addEdge(left, w);
                    } else {
                        tree.addEdge(right, w);
                    }
                    i = i + 1;
                }
                stack.push(left);
                stack.push(right);
            }
        }
        treeDecomposition.setNumberOfBags(tree.getCopyOfVertices().size());
        int id = 1;
        for (Bag<T> v : tree) v.id = id++;
        return root;
    }

    /**
     * The structure of a nice tree decomposition is crucial for the performance of algorithms working on it.
     * This method tries to optimize the structure by, for instance, rearranging join bags.
     */
    private void optimizeDecomposition() {
    }

    /**
     * In a \emph{nice} tree decomposition, the bags are partitioned into $\{\text{join}, \text{introduce}, \text{forget}, \text{leafs}\}$
     * bags. This function computes, given a nice tree decomposition, the type of each bag.
     */
    private void classifyBags() {
        bagType = new HashMap<>();
        specialVertex = new HashMap<>();

        Set<Bag<T>> visited = new HashSet();
        Stack<Bag<T>> stack = new Stack<>();
        visited.add(root);
        stack.push(root);

        while (!stack.isEmpty()) {
            Bag<T> v = stack.pop();
            if (treeDecomposition.getNeighborhood(v).size() == 3) bagType.put(v, BagType.JOIN);
            for (Bag<T> w : treeDecomposition.getNeighborhood(v)) {
                if (visited.contains(w)) continue; // parent
                if (!bagType.containsKey(v)) {
                    Set<T> tmp = new HashSet<>(v.vertices);
                    tmp.removeAll(w.vertices);
                    if (tmp.size() == 1) {
                        bagType.put(v, BagType.INTRODUCE);
                        specialVertex.put(v, tmp.stream().findFirst().get());
                    } else {
                        tmp = new HashSet<>(w.vertices);
                        tmp.removeAll(v.vertices);
                        bagType.put(v, BagType.FORGET);
                        specialVertex.put(v, tmp.stream().findFirst().get());
                    }
                }

                visited.add(w);
                stack.push(w);
            }
            if (!bagType.containsKey(v)) bagType.put(v, BagType.LEAF);
        }

    }

    /**
     * Given a bag of a tree decomposition, we may which to index data using the elements of this bag. For instance, we
     * which to to know which vertex is ``the first vertex in the bag'', since we do not want to allocate space in the range
     * of $O(n)$ per bag.
     *
     * This function computes a so called \emph{tree-index}, which is a mapping from the vertices of the graph to
     * $\{0,\dots,k\}$, where $k$ is the width of the given tree decomposition. It guarantees that in no bag two vertices
     * have the same tree-index, i.\,e., it can be used to access data.
     *
     * The tree-index can be computed by the following simple algorithm (which also shows that something like the tree-index
     * exists at all): Start at the root with a pool of available indices ($\{0,\dots,k\}$). Whenever you encounter a
     * forget bag (say for $v$), pop a index from the queue and assign it to vertex $v$. In every child-branch, if you encounter
     * an introduce bag for this vertex, add the index back to the queue. Observe that there is only one forget bag for every vertex
     * and, therefore, every vertex optains just one index. Since there can not be a chain of more then $k$ forget vertices,
     * we also have always an index in the queue.
     *
     */
    private void computeTreeIndex() {
        treeIndex = new HashMap<>();

        // queue of available indices
        Stack<Integer> indices = new Stack<>();
        int i = treeDecomposition.getWidth()+1; while (i --> 0) indices.push(i);

        // assign indices via DFS
        Stack<Bag<T>> stack = new Stack<>();
        Set<Bag<T>> visited = new HashSet<>();
        Set<Bag<T>> finished = new HashSet<>();
        stack.push(root);
        visited.add(root);
        while (!stack.isEmpty()) {
            Bag<T> v = stack.peek();
            if (finished.contains(v)) {
                stack.pop();
                if (bagType.get(v) == BagType.FORGET) {
                    indices.push(treeIndex.get(specialVertex.get(v)));
                } else if (bagType.get(v) == BagType.INTRODUCE) {
                    indices.pop();
                }
                continue;
            }
            finished.add(v);

            if (bagType.get(v) == BagType.FORGET) {
                treeIndex.put(specialVertex.get(v), indices.pop());
            } else if (bagType.get(v) == BagType.INTRODUCE) {
                indices.push(treeIndex.get(specialVertex.get(v)));
            }

            for (Bag<T> w : treeDecomposition.getNeighborhood(v)) {
                if (visited.contains(w)) continue;
                stack.push(w);
                visited.add(w);
            }
        }
    }

    /**
     * Compute all edge bags for the decomposition.
     * This will change the tree decomposition and the labeling of bags function.
     * However, it will not change the tree-index.
     */
    private void computeEdgeBags() {
        secondSpecialVertex = new HashMap<>();

        // compute introduce edge bags by DFS
        Stack<Bag<T>> stack = new Stack<>();
        Set<Bag<T>> visited = new HashSet<>();
        stack.push(getRoot());
        visited.add(getRoot());

        while (!stack.isEmpty()) {
            Bag<T> v = stack.pop();

            // we only have to do something for forget bags, as we will introduce all remaining edges before this bag
            if (bagType.get(v) == BagType.FORGET) {

                T x = specialVertex.get(v); // vertex that was forgotten
                Bag<T> child = null;
                for (Bag<T> w : treeDecomposition.getNeighborhood(v)) { // we have exactly one child, get it!
                    if (!visited.contains(w)) { child = w; break; }
                }

                // go through neighbors, if they are in the child bag we have to introduce the ege
                for (T y : treeDecomposition.getGraph().getNeighborhood(x)) {
                    if (child.vertices.contains(y)) {
                        Bag<T> newChild = treeDecomposition.createBag(child.vertices);
                        bagType.put(newChild, BagType.EDGE);
                        specialVertex.put(newChild, x);
                        secondSpecialVertex.put(newChild, y);
                        treeDecomposition.addTreeEdge(child, newChild);
                        treeDecomposition.addTreeEdge(newChild, v);
                        treeDecomposition.getTree().removeEdge(child, v);
                        child = newChild;
                    }
                }
            }

            // continue DFS
            for (Bag<T> w : treeDecomposition.getNeighborhood(v)) {
                if (visited.contains(w)) continue;
                visited.add(w);
                stack.push(w);
            }
        }
    }

    /**
     * Returns the root bag of the nice tree decomposition.
     *
     * @return The unique root bag.
     */
    public Bag<T> getRoot() {
        return root;
    }
}
