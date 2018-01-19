package jdrasil.algorithms.postprocessing;

import jdrasil.graph.Bag;
import jdrasil.graph.TreeDecomposition;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

/**
 * A \emph{nice} tree-decomposition is \emph{rooted} tree-decomposition in which each bag has one of the following types:
 *
 * \begin{enumerate}
 *  \item There is exactly one \emph{root} bag that stores an empty set.
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
    private Map<T, Integer> treeIndex;

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
        // TODO: this is hacky and should be reimplemented
        if (treeDecomposition.getNumberOfBags()==0) {
            return null;
        }

        // remove dublicates
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
     * Computes the tree-index for the decomposition.
     */
    private void computeTreeIndex() {
    }

    /**
     * Returns the tree-index for this decomposition
     * @return A mapping $\phi\colon V\rightarrow\{0,\dots,\mathrm{tw}\}$.
     */
    public Map<T, Integer> getTreeIndex() {
        return treeIndex;
    }

    /**
     * Returns the root bag of the nice tree-decomposition.
     * @return The unique root bag.
     */
    public Bag<T> getRoot() {
        return root;
    }
}
