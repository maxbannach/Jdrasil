package jdrasil.algorithms.postprocessing;

import jdrasil.graph.Bag;
import jdrasil.graph.Graph;
import jdrasil.graph.GraphFactory;
import jdrasil.graph.TreeDecomposition;
import jdrasil.graph.invariants.MinimalSeparator;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This postprocessor allows to improve the quality of the decomposition (whenever we know that we do not have an
 * optimal decomposition) with the separator technique of "Treewidth computations I. Upper bounds" by Bodlaender and Koster.
 *
 * @author Sebastian Berndt
 */
public class ImproveTreeDecomposition<T extends Comparable<T>> extends Postprocessor<T> {

    /**
     * The constructor just initialize some internal data structures and stores the tree-decomposition that should
     * be postprocessed.
     *
     * @param treeDecomposition The tree-decomposition to be postprocessed.
     */
    public ImproveTreeDecomposition(TreeDecomposition<T> treeDecomposition) {
        super(treeDecomposition);
    }

    @Override
    protected TreeDecomposition<T> postprocessTreeDecomposition() {
        improveDecomposition();
        return treeDecomposition;
    }

    /**
     * Improve the current decomposition by trying to find a suitable, improvable bag.
     */
    public void improveDecomposition(){
        boolean stop = false;
        do{
            stop = true;
            for(Bag<T> b: treeDecomposition.getTree().getCopyOfVertices()){
                Graph<T> g = toGraph(b);

                // if the graph is not a clique, improve it
                if(!g.isClique()){
                    improveBag(b);
                    stop = false;
                    break;
                }
            }

        }
        while(!stop);
    }

    /**
     * Improve a bag by computing a minimum separator for it and splitting it according to the separator.
     * See "Treewidth computations I. Upper bounds" by Bodlaender and Koster.
     *
     * @param b the bag to improve
     */
    public void improveBag(Bag<T> b){
        Graph<Bag<T>> tree = treeDecomposition.getTree();
        Set<Bag<T>> neighbours = tree.getNeighborhood(b);
        Graph<T> g = toGraph(b);

        // compute a minimal separator
        Set<T> sep = new MinimalSeparator<T>(g).getSeperator();

        // remove the separator from the graph
        for(T v: sep){
            g.removeVertex(v);
        }

        // compute the remaining connected components
        List<Set<T>> cs = g.getConnectedComponents();

        // remove the bag b from the decomposition
        tree.removeVertex(b);

        // replace b by a new bag containing the separator
        Bag<T> bsep = treeDecomposition.createBag(sep);
        tree.addVertex(bsep);

        // add more bags containing the separator and the connected components
        for(Set<T> set: cs){
            Set<T> tset = new HashSet<>();
            tset.addAll(set);

            set.addAll(sep);
            Bag<T> bset =  treeDecomposition.createBag(set);
            tree.addVertex(bset);
            tree.addEdge(bsep, bset);

            // connected the components to the outer bags
            for(Bag<T> bx: neighbours){
                Set<T> intersection = new HashSet<T>(sep); // use the copy constructor
                intersection.retainAll(bx.vertices);
                Set<T> union = new HashSet<T>(sep); // use the copy constructor
                union.addAll(tset);
                if(union.containsAll(intersection)){
                    tree.addEdge(bset, bx);
                }
            }
        }
    }

    /**
     * Construct a graph from a bag.
     * It consists of all vertices from the bag and all graph edges.
     * In addition, it also contains {u,v}, if there is another bag that contains u and v.
     *
     * @param b the bag
     * @return the graph constructed from the bag
     */
    public Graph<T> toGraph(Bag<T> b){
        Graph<T> g = GraphFactory.emptyGraph();
        for(T v: b.vertices){
            g.addVertex(v);
        }
        for(T v: b.vertices){
            for(T u: b.vertices){
                if(u != v){
                    if(treeDecomposition.getGraph().isAdjacent(u, v) || inAnotherBag(u,v,b)){
                        g.addEdge(u, v);
                    }
                }
            }
        }
        return g;
    }

    /**
     * Computes whether another bag exists that contains u and v.
     *
     * @param u the first node u
     * @param v the second node v
     * @param b the original bag that contains u and v
     * @return whether a bag b' != b exists that also contains u and v
     */
    private boolean inAnotherBag(T u, T v, Bag<T> b){
        boolean res = false;
        for(Bag<T> d : treeDecomposition.getTree().getCopyOfVertices()){
            if(d != b && d.contains(u) && d.contains(v)){
                res = true;
            }
        }
        return res;
    }

}
