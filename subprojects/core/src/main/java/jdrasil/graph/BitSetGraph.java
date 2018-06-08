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
package jdrasil.graph;

import java.util.*;

/**
 * A BitSetGraph stores a graph (with arbitrary vertices) as bitwise adjacency matrix, i.\,e., as array of BitSets where
 * the i'th BitSet corresponds to the i'th row of the adjacency matrix. This comes with all the advantages and disadvantages
 * of an adjacency matrix.
 *
 * The crucial advantage is that the graph is compact and that many operations can be performed quickly on the bit level.
 * In particular, dynamic programming over subgraphs can be implemented efficiently, as subgraphs are also just BitSets
 * that can efficiently be hashed.
 */
public class BitSetGraph<T extends Comparable<T>> {

    /** The underling ``real'' graph. */
    private final Graph<T> graph;

    /** The number of vertices in the graph. */
    private final int n;

    /** A bijection from $V$ to $\{0,...,n-1\}$ */
    private final Map<T, Integer> vToInt;
    private final Map<Integer, T> intToV;

    /** The graph as array of BitSets (aka.\, bit adjacency matrix). */
    private final BitSet[] bitSetGraph;

    /* Data Structures for memorization */
    private Map<BitSet, List<BitSet>> separateMemory;
    private Map<BitSet, BitSet> exteriorBorderMemory;
    private Map<BitSet, Boolean> pmcMemory;

    /**
     * Creates the BitSetGraph from the given graph by computing a bijection from the vertices to $\{0,...,n-1\}$
     * and storing edges in an adjacency matrix represented by an array of BitSets.
     * @param graph The graph to be represented as BitSetGraph.
     */
    public BitSetGraph(Graph<T> graph) {
        // parse given graph
        this.graph = graph;
        this.n = graph.getCopyOfVertices().size();

        // compute the bijection
        this.vToInt = new HashMap<>();
        this.intToV = new HashMap<>();
        int i = 0;
        for (T v : graph) {
            vToInt.put(v, i);
            intToV.put(i, v);
            i++;
        }

        // create the bitgraph
        bitSetGraph = new BitSet[n];
        for (int v = 0; v < n; v++) bitSetGraph[v] = new BitSet();
        for (T v : graph) {
            int x = vToInt.get(v);
            for (T w : graph.getNeighborhood(v)) {
                int y = vToInt.get(w);
                bitSetGraph[x].set(y);
            }
        }

        // initialize memorization
        separateMemory = new HashMap<>();
        exteriorBorderMemory = new HashMap<>();
        pmcMemory = new HashMap<>();
    }

    /**
     * Getter for the original graph used to create this BitSet graph.
     * @return The original graph.
     */
    public Graph<T> getGraph() {
        return this.graph;
    }

    /**
     * Getter for the number of vertices in the graph.
     * @return The number of vertices $|V|$.
     */
    public int getN() {
        return this.n;
    }

    /**
     * Getter for the BitSet graph: an array of n BitSets where the i'th BitSet corresponds to the i'th row
     * of the adjacency matrix of the graph.
     * @return The adjacency matrix of $G$ as Array of BitSets.
     */
    public BitSet[] getBitSetGraph() {
        return this.bitSetGraph;
    }

    /**
     * Get the used mapping that maps the vertices to $\{0,...,n-1\}$.
     * @return A map from vertices to ids.
     */
    public Map<T, Integer> getVToInt() {
        return this.vToInt;
    }

    /**
     * Get the reverse mapping, i.e., from $\{0,...,n-1\}$ to the vertices of the original graph.
     * @return A map from ids to vertices.
     */
    public Map<Integer, T> getIntToV() {
        return this.intToV;
    }

    /**
     * Computes a set of ``real'' vertices to the corresponding BitSet.
     * @param S A vertex set as BitSet.
     * @return The corresponding set of vertex objects.
     */
    public Set<T> getVertexSet(BitSet S) {
        Set<T> vertexSet = new HashSet<T>();
        for (int v = S.nextSetBit(0); v >= 0; v = S.nextSetBit(v+1)) {
            vertexSet.add(intToV.get(v));
        }
        return vertexSet;
    }

    /**
     * Computes a corresponding BitSet to a ``real'' vertex set.
     * @param vertexSet A vertex set as set of vertex objects.
     * @return The corresponding BitSet.
     */
    public BitSet getBitSet(Set<T> vertexSet) {
        BitSet S = new BitSet();
        for (T v : vertexSet) S.set(vToInt.get(v));
        return S;
    }

    /**
     * Gets the interior border of $C$, i.\,e., all vertices $v$ in $C$ that have at least one neighbor outside of $C$.
     * @param C A vertex set $C\subseteq V$.
     * @return $N(N(C))\cap C$, i.\,e., the vertices inside $C$ that have an outgoing edge.
     */
    public BitSet interiorBorder(BitSet C) {
        BitSet border = new BitSet();
        BitSet outside = (BitSet) C.clone();
        outside.flip(0,n);
        for (int v = C.nextSetBit(0); v >= 0; v = C.nextSetBit(v+1)) {
            if (bitSetGraph[v].intersects(outside)) border.set(v);
        }
        return border;
    }

    /**
     * Gets the exterior border of $C$, i.e., all vertices $v$ in $G[V\setminus C]$ that have at least one neighbor in $C$.
     * @param C A vertex set $C\subseteq V$.
     * @return $N(C)$, i.\,e., the vertices that are reachable from $C$ with a single edge.
     */
    public BitSet computeExteriorBorder(BitSet C) {
        BitSet border = new BitSet();
        for (int v = C.nextSetBit(0); v >= 0; v = C.nextSetBit(v+1)) {
            border.or(bitSetGraph[v]);
        }
        border.andNot(C);
        return border;
    }

    /**
     * Gets the exterior border of $C$, i.e., all vertices $v$ in $G[V\setminus C]$ that have at least one neighbor in $C$.
     * @param C A vertex set $C\subseteq V$. Cashes the result for further use.
     * @return $N(C)$, i.\,e., the vertices that are reachable from $C$ with a single edge.
     */
    public BitSet exteriorBorder(BitSet C) {
        if (exteriorBorderMemory.containsKey(C)) return exteriorBorderMemory.get(C);
        BitSet border = computeExteriorBorder(C);
        exteriorBorderMemory.put(C, border);
        return border;

    }

    /**
     * Saturates the subgraph $C$ by adding all vertices $v\in N(C)$ of $V\setminus C$ that have all neighbors in $C$ or $N(C)$.
     * @param C A vertex set $C\subseteq V$ that should be saturated.
     */
    public void saturate(BitSet C) {
        BitSet neighbors = computeExteriorBorder(C);
        BitSet Sprime = (BitSet) C.clone();
        Sprime.or(neighbors);
        for (int v = neighbors.nextSetBit(0); v >= 0; v = neighbors.nextSetBit(v+1)) {
            BitSet tmp = (BitSet) bitSetGraph[v].clone();
            tmp.andNot(Sprime);
            if (tmp.cardinality() == 0) C.set(v);
        }
    }

    /**
     * Finds and returns an arbitrary vertex $v\in N(C)$ that has only neighbors in $C$ or $N(C)$. Returns $-1$ if no such
     * vertex exists.
     * @param C A vertex set $C\subseteq V$.
     * @return An absorbable vertex $v\in C$, or $-1$ if non exists.
     */
    public int absorbable(BitSet C) {
        BitSet neighbors = computeExteriorBorder(C);
        BitSet outside = (BitSet) C.clone();
        outside.or(neighbors);
        outside.flip(0,n);
        for (int v = neighbors.nextSetBit(0); v >= 0; v = neighbors.nextSetBit(v+1)) {
            if (!bitSetGraph[v].intersects(outside)) return v;
        }
        return -1;
    }

    /**
     * Compute the connected components of $G[V\setminus S]$, $S$ will not be included in any of these components.
     * @param S A separator $S\subseteq V$.
     * @return A list of the connected components of $G[V\setminus S]$, each represented as BitSet.
     */
    public List<BitSet> separate(BitSet S) {
        if (separateMemory.containsKey(S)) return separateMemory.get(S);
        List<BitSet> components = new ArrayList<>(5);
        BitSet visited = new BitSet();
        visited.or(S);

        // start dfs on each vertex
        Stack<Integer> stack = new Stack<>();
        for (int s = 0; s < n; s++) {
            if (visited.get(s)) continue; // old component
            BitSet component = new BitSet();
            component.set(s);
            visited.set(s);
            stack.add(s);
            while (!stack.isEmpty()) {
                int v = stack.pop();
                for (int w = 0; w < n; w++) {
                    if (!bitSetGraph[v].get(w)) continue;
                    if (visited.get(w)) continue;
                    component.set(w);
                    visited.set(w);
                    stack.push(w);
                }
            }
            components.add(component);
        }

        // done
        separateMemory.put(S, components);
        return components;
    }

    /**
     * Test if the given set of vertices is a potential maximal clique, i.\,e., a maximal clique in some minimal triangulation
     * of the graph.
     * This method uses the local characterisation of potential maximal cliques found by Bouchitte and Todinca ``Treewidth
     * and Minimum Fill-In: Grouping th Minimal Separators''.
     * @param K A subgraph $K\subseteq V$.
     * @return True if $K$ is a potential maximal clique.
     */
    public boolean isPotentialMaximalClique(BitSet K) {
        if (pmcMemory.containsKey(K)) return pmcMemory.get(K);
        List<BitSet> components = separate(K);

        // first test, K may not have a full component
        for (BitSet component : components) {
            if (computeExteriorBorder(component).cardinality() == K.cardinality()) return false;
        }

        // second test, K must be cliquish
        for (int v = K.nextSetBit(0); v >= 0; v = K.nextSetBit(v+1)) {
            for (int w = K.nextSetBit(v+1); w >= 0; w = K.nextSetBit(w+1)) {
                if (bitSetGraph[v].get(w)) continue; // already an edge
                boolean completable = false;
                for (BitSet C : components) {
                    if (bitSetGraph[v].intersects(C) && bitSetGraph[w].intersects(C)) {
                        completable = true;
                        break;
                    }
                }
                if (!completable) {
                    pmcMemory.put(K, false);
                    return false;
                }
            }
        }

        // done
        pmcMemory.put(K, true);
        return true;
    }

    /**
     * Compute the unconfined components of $G[V\setminus K]$ with respect to $S$, i.\,e., the components $C$ with $N(C)\not\subseteq S$.
     * @param S A vertex set $S\subseteq K$.
     * @param K A vertex set $K\subset V$.
     * @return The set $\mathrm{unconf}(S,K)$ as list of BitSets.
     */
    public List<BitSet> unconf(BitSet S, BitSet K) {
        List<BitSet> result = new LinkedList<>();
        List<BitSet> components = separate(K);

        // not S
        BitSet mask = new BitSet();
        mask.set(0,n);
        mask.andNot(S);

        // unconf N(C) subseteq S -> N(C) cap not S = empty
        for (BitSet C : components) {
            if (!computeExteriorBorder(C).intersects(mask)) continue;
            result.add(C);
        }

        return result;
    }

    /**
     * Compute the crib of $K$ with respect to $S$.
     * @param S A vertex set $S\subseteq K$.
     * @param K A vertex set $K\subset V$.
     * @return $(K\setminus S)\cup\bigcup_{D\in\mathrm{unconf(S,K)}}D$ as BitSet.
     */
    public BitSet crib(BitSet S, BitSet K) {
        BitSet crib = (BitSet) K.clone();
        crib.andNot(S);

        for (BitSet D : unconf(S, K)) {
            crib.or(D);
        }

        return crib;
    }

    /**
     * Check if the given component is outbound, that is, if it is the minimum of all components with neighborhood $N(C)$.
     * Minimum is defined with respect to the natural order of the vertices and a component is smaller then another
     * if it contains a ``smaller'' vertex.
     * @param C A vertex set $C\subseteq V$.
     * @return True if $C$ is outbound.
     */
    public boolean outbound(BitSet C) {
        BitSet NC = computeExteriorBorder(C);
        List<BitSet> components = separate(NC);
        for (BitSet component : components) {
            if (computeExteriorBorder(component).cardinality() != NC.cardinality()) continue;
            if (component.nextSetBit(0) < C.nextSetBit(0)) return false;
        }
        return true;
    }

    /**
     * Compute the outlet of the given set $K$.
     * @param K A vertex set $K\subseteq V$.
     * @return The maximum set $N(A)\subseteq K$ over all non-full and outbound components $A$ associated with $K$.
     */
    public BitSet outlet(BitSet K) {
        BitSet S = new BitSet();
        List<BitSet> components = separate(K);
        for (BitSet component : components) {
            if (!outbound(component)) continue;
            BitSet NC = computeExteriorBorder(component);
            if (NC.cardinality() == K.cardinality()) continue;
            if (NC.cardinality() > S.cardinality()) S = NC;
        }
        return S;
    }

    /**
     * Compute the support of $K$.
     * @param K A vertex set $K\subseteq V$.
     * @return $\mathrm{unconf}(\mathrm{outlet}(K),K)$ as BitSet.
     */
    public List<BitSet> support(BitSet K) {
        return unconf(outlet(K), K);
    }

    /**
     * Compute the full-components associated with $K$.
     * @param K A vertex set $K\subseteq V$.
     * @return A list of all components $A\in G[V\setminus K]$ with $N(A)=K$.
     */
    public List<BitSet> fullComponents(BitSet K) {
        List<BitSet> components = separate(K);
        List<BitSet> fullComponents = new LinkedList<>();
        for (BitSet D : components) {
            if (computeExteriorBorder(D).cardinality() == K.cardinality()) fullComponents.add(D);
        }
        return fullComponents;
    }

}
