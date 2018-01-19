package jdrasil.algorithms.postprocessing;

import jdrasil.graph.Bag;
import jdrasil.graph.Graph;
import jdrasil.graph.TreeDecomposition;

import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

/**
 * A tree-decomposition may contain useless bags. For instance, two adjacent bags with the same content, or an empty bag.
 * This Postprocessor will \emph{flatten} the given tree-decomposition by removing such useless bags.
 *
 */
public class FlattenTreeDecomposition<T extends Comparable<T>> extends Postprocessor<T> {

    /**
     * The constructor just initialize some internal data structures and stores the tree-decomposition that should
     * be postprocessed.
     *
     * @param treeDecomposition The tree-decomposition to be postprocessed.
     */
    public FlattenTreeDecomposition(TreeDecomposition<T> treeDecomposition) {
        super(treeDecomposition);
    }

    @Override
    protected TreeDecomposition<T> postprocessTreeDecomposition() {
        contractDuplicateBags();
        return treeDecomposition;
    }

    /**
     * Contract adjacent bags with same content.
     * TODO: This is very hacky and should be reimplemented
     */
    private void contractDuplicateBags() {
        Graph<Bag<T>> tree = treeDecomposition.getTree();
        if (treeDecomposition.getNumberOfBags() == 0) { return; }

        Set<Bag<T>> visited=new HashSet<>();
        Stack<Bag<T>> S = new Stack<>();

        Bag<T> root = tree.iterator().next();
        S.push(root);
        visited.add(root);
        while (!S.isEmpty()) {
            Bag<T> parent = S.pop();
            boolean modified=true;
            while (modified) {
                modified=false;
                for (Bag<T> child : tree.getNeighborhood(parent)) {
                    if (!visited.contains(child)) {
                        // TODO contraction is inconsequent!
                        if (child.vertices.equals(parent.vertices)
                                || (parent.vertices.containsAll(child.vertices) && (tree.getNeighborhood(child).size()<=2)) ) {
                            tree.contract(parent, child);
                            modified=true;
                            break;
                        } else {
                            S.push(child);
                            visited.add(child);
                        }
                    }
                }
            }
        }

        treeDecomposition.setNumberOfBags(tree.getCopyOfVertices().size());
        int id=1;
        for (Bag<T> v : tree) {
            v.id=id++;
        }
    }

}
