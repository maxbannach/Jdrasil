package jdrasil.algorithms.postprocessing;

import jdrasil.graph.Bag;
import jdrasil.graph.TreeDecomposition;

import java.util.*;

import static com.sun.org.apache.xalan.internal.xsltc.compiler.util.Type.Int;

/**
 * A \emph{nice} tree-decomposition is \emph{rooted} tree-decomposition in which each bag has one of the following types:
 *
 * \begin{enumerate}
 *  \item A \emph{leaf} bag has no children and stores and empty set.
 *  \item An \emph{introduce} bag has exactly one child bag and has the same content as its child up to one
 *    addional vertex (which was ``introduced'' at this bag.
 *  \item A \emph{forget} bag has exactly one child bag and contains all but one vertex of its child (it ``forgets''
 *    this vertex).
 *  \item A \emph{join} bag has two children which both have the same content as the join bag.
 * \end{enumerate}
 *
 * It is well known that to every tree-decomposition there can be found a nice tree-decomposition of the same width.
 * This class will perform this transformation for a given tree-decomposition. The modifiction will be performed
 * \emph{inplace}, i.\,e., the given tree-decomposition will be modified.
 *
 * @author Max Bannach
 */
public class NiceTreeDecomposition<T extends Comparable<T>> extends Postprocessor<T> {

    /**
     * The tree-index is a mapping $\phi\colon V\rightarrow\{0,\dots,\mathrm{tw}\}$ that maps every vertex of the graph
     * to an index such that no two vertex appearing in a bag share the same index. Therefore, the tree-index can be used
     * to index data structures when working on the tree-decomposition.
     */
    public Map<T, Integer> treeIndex;

    /** Every bag in a nice tree-decomposition has a specific type (leaf, join, introduce, forget). */
    public enum BagType {
        LEAF,
        INTRODUCE,
        FORGET,
        JOIN;
    }

    /**
     * Stores for every bag which type it has.
     */
    public Map<Bag<T>, BagType> bagType;

    /**
     * Introduce and forget bags also have a special vertex.
     */
    public Map<Bag<T>, T> specialVertex;

    /**
     * The root bag of the nice tree-decomposition.
     */
    private Bag<T> root;

    /**
     * The constructor just initialize some internal data structures and stores the tree-decomposition that should
     * be postprocessed.
     *
     * @param treeDecomposition The tree-decomposition to be postprocessed.
     */
    public NiceTreeDecomposition(TreeDecomposition<T> treeDecomposition) {
        super(treeDecomposition);

    }

    @Override
    protected TreeDecomposition<T> postprocessTreeDecomposition() {
        Bag<T> suitableRoot = findSuitableRoot();
        this.root = makeNice(suitableRoot);
        optimizeDecomposition();
        classifyBags();
        computeTreeIndex();
        return treeDecomposition;
    }

    /**
     * A nice tree-decomposition is computed from a chosen root bag down to the leafs. The choose of this root bag
     * may dramatically change the structure of the nice tree-decomposition which may have an impact on the performance
     * of algorithms that later work on the decomposition.
     *
     * This method implements an heurictic that tries to find a ``good'' candidate for such an root.
     *
     * @return A suitable root bag from which on we can build a nice tree-decomposition.
     */
    private Bag<T> findSuitableRoot() {
        return null;
    }

    /**
     * Make the tree-decomposition \emph{nice} starting at the given root. Note that the root bag may not be the given bag,
     * but will lead by a path of forget-bags to it.
     * @param root A bag at which we shall root the tree-decomposition.
     * @return The actual root bag of the constructed nice tree-decomposition.
     */
    private Bag<T> makeNice(Bag<T> root) {
        if (treeDecomposition.getNumberOfBags()==0) {
            return null;
        }

        // remove duplicates
        new FlattenTreeDecomposition<T>(treeDecomposition).getProcessedTreeDecomposition();

        Set<Bag<T>> visited=new HashSet<>();
        Stack<Bag<T>> S = new Stack<>();

        // find a leaf as root, so that path decompositions remain paths
        root=null;
        for (Bag<T> v : treeDecomposition.getTree()) {
            if (treeDecomposition.getTree().getNeighborhood(v).size()<=1) {
                root=v;
                break;
            }
        }
        S.push(root);
        visited.add(root);

        // Add bags: from root bag to empty bag one by one
        if (root.vertices.size() > 0) {
            Set<T> currentRootSet=new HashSet<>(root.vertices);
            Bag<T> prevRoot=root;
            for (T v : root.vertices) {
                currentRootSet.remove(v);
                Bag<T> b=treeDecomposition.createBag(new HashSet<T>(currentRootSet));
                treeDecomposition.addTreeEdge(b, prevRoot);
                prevRoot=b;
                visited.add(b); // these nodes are already nice
            }
            root = prevRoot;
        }

        while (!S.isEmpty()) {
            Bag<T> parent = S.pop();
            Set<Bag<T>> childs=new HashSet<>();
            for (Bag<T> v : treeDecomposition.getTree().getNeighborhood(parent)) {
                if (!visited.contains(v)) {
                    childs.add(v);
                }
            }
            int childsToCome=childs.size();
            // if leaf, then add bags: remove vertices one by one
            if (childsToCome==0) {
                Set<T> currentSet=new HashSet<>(parent.vertices);
                Bag<T> prevParent=parent;
                for (T v : parent.vertices) {
                    currentSet.remove(v);
                    Bag<T> b=treeDecomposition.createBag(new HashSet<T>(currentSet));
                    treeDecomposition.addTreeEdge(prevParent, b);
                    prevParent=b;
                    //visited.add(b); not needed
                }
            }
            // else iterate over childs
            for (Bag<T> child : childs) {
                // remove edges; we probably will create new nodes in between.
                treeDecomposition.getTree().removeEdge(parent, child);
                //if (child.vertices.equals(parent.vertices)) {
                //	System.out.println("error on " + parent);
                //}
            }
            for (Bag<T> child : childs) {
                Bag<T> leftParent=parent;
                Bag<T> rightParent=null;
                // if there is more than one child left, make the node a join node
                if (childsToCome>1) {
                    leftParent=treeDecomposition.createBag(new HashSet<T>(parent.vertices));
                    treeDecomposition.addTreeEdge(parent, leftParent);
                    visited.add(leftParent);
                    rightParent=treeDecomposition.createBag(new HashSet<T>(parent.vertices));
                    treeDecomposition.addTreeEdge(parent, rightParent);
                    visited.add(rightParent);
                }

                // Append the child (to the left parent, if join node; otherwise,
                // leftParent is the right parent from the iteration before).
                // Do this nicely, so remove or add one vertex at a time.

                Set<T> removedVertices=new HashSet<>(parent.vertices);
                removedVertices.removeAll(child.vertices);
                Set<T> addedVertices=new HashSet<>(child.vertices);
                addedVertices.removeAll(parent.vertices);
                int diffSize=removedVertices.size()+addedVertices.size();

                Set<T> currentSet=new HashSet<>(parent.vertices);
                // in the following, at least one for loop will be executed once.
                Bag<T> newChild=null;
                for (T v : removedVertices) {
                    currentSet.remove(v);
                    diffSize--;
                    if (diffSize==0) {
                        newChild=child;
                    } else {
                        newChild=treeDecomposition.createBag(new HashSet<T>(currentSet));
                        visited.add(newChild);
                    }
                    treeDecomposition.addTreeEdge(leftParent, newChild);
                    leftParent=newChild;
                }
                for (T v : addedVertices) {
                    currentSet.add(v);
                    diffSize--;
                    if (diffSize==0) {
                        newChild=child;
                    } else {
                        newChild=treeDecomposition.createBag(new HashSet<T>(currentSet));
                        visited.add(newChild);
                    }
                    treeDecomposition.addTreeEdge(leftParent, newChild);
                    leftParent=newChild;
                }

                // In the following, newChild is the same as child.
                S.push(newChild);
                visited.add(newChild);

                parent=rightParent;
                childsToCome--;
            }
        }

        return root;
    }

    /**
     * The structure of a nice tree-decomposition is crucial for the performance of algorithms working on it.
     * This method tries to optimize the structure by, for instance, rearranging join bags.
     */
    private void optimizeDecomposition() {
    }

    /**
     * In a \emph{nice} tree-decomposition, the bags are partitioned into $\{\text{join}, \text{introduce}, \text{forget}, \text{leafs}\}$
     * bags. This function computes, given a nice tree-decomposition, the type of each bag.
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
     * Given a bag of a tree-decomposition, we may which to index data using the elements of this bag. For instance, we
     * which to to know which vertex is ``the first vertex in the bag'', since we do not want to allocate space in the range
     * of $O(n)$ per bag.
     *
     * This function computes a so called \emph{tree-index}, which is a mapping from the vertices of the graph to
     * $\{0,\dots,k\}$, where $k$ is the width of the given tree-decomposition. It guarantees, that in no bag two vertices
     * have the same tree-index, i.\,e., it can be used to access data.
     *
     * The tree-index can be computed by the following simple algorithm (which also shows that something like the tree-index
     * exists at all): Start at the root with a pool of available indices ($\{0,\dots,k\}$). Whenever you encounter a
     * forget bag (say for $v$), pop a index from the queue and assign it to vertex $v$. In every child-branch, if you encounter
     * a join bag for this vertex, add the index back to the queue. Observe that there is only one forget bag for every vertex
     * such that every vertex optains just one index, and since there can not be a chain of more then $k$ forget vertex,
     * we also have always a index in the queue.
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
     * Returns the root bag of the nice tree-decomposition.
     * @return The unique root bag.
     */
    public Bag<T> getRoot() {
        return root;
    }
}
