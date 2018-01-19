package jdrasil.algorithms.postprocessing;

import jdrasil.graph.Bag;
import jdrasil.graph.TreeDecomposition;

import java.util.*;

/**
 * A \emph{very nice} tree-decomposition is a nice tree-decomposition that has additionally introduce-edge bags. Such
 * a bag is labeled with an edge and has exactly one child with the same content. The interpretation is, that introduce
 * bag only introduce the isolated vertex, while the edge first arises at the inroduce edge bag.
 *
 * Furthermore, every edge is introduced exactly once.
 *
 * @author Max Bannach
 */
public class VeryNiceTreeDecomposition<T extends Comparable<T>> extends NiceTreeDecomposition<T> {

    /** Introduce edge bags need two special vertices. */
    public Map<Bag<T>, T> secondSpecialVertex;

    /**
     * The constructor just initialize some internal data structures and stores the tree-decomposition that should
     * be postprocessed.
     *
     * @param treeDecomposition The tree-decomposition to be postprocessed.
     */
    public VeryNiceTreeDecomposition(TreeDecomposition<T> treeDecomposition) {
        super(treeDecomposition);
    }

    @Override
    protected TreeDecomposition<T> postprocessTreeDecomposition() {
        super.postprocessTreeDecomposition(); // make it nice in the first place
        computeIntroduceEdgeBags();
        return treeDecomposition;
    }

    /**
     * Compute all introduce edge bags for the decomposition.
     * This will change the tree-decomposition and the labeling of bags function.
     * However, it will not change the tree-index.
     */
    private void computeIntroduceEdgeBags() {
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
                        bagType.put(newChild, BagType.INTRODUCE_EDGE);
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

}
