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
package jdrasil.datastructures;

import java.util.*;

/**
 * A set-trie is a trie that stores ordered sets - essentially by interpreting the set as string and the elements as symbols.
 * This class implements a set-trie over BitSets, which are a natural representation of ordered sets.
 *
 * With a set-trie we can efficiently check if a set is in the collection, and we can quickly iterate over stored sub- and
 * super sets of a given set.
 *
 * BitSets are an efficient and compact way to store and work with sets. However, as they are bitmasks, we pay the price
 * that set operation costs \(O(|U|)\) where \(U\) is the universe and not the set (however, these costs are mainly a
 * single bit-level iteration which is fast in practice).
 *
 * @author Max Bannach
 * @author Sebastian Berndt
 */
public class BitSetTrie {

    /* the root of the trie */
    private Node root;

    /* flag that specifies if the trie stores the empty set or not */
    private boolean containsEmptySet;

    /*
     * Size of the universe, i.e., the size of the largest bitset stored in the trie.
     * Needed for superset iteration in the trie, will be updated by the insert method, but not by removing, i.e.,
     * during the usage of a trie this value is monotone growing.
     */
    private int universeSize;

    /**
     * The constructor creates and initializes an empty trie with current universe size 0.
     */
    public BitSetTrie() {
        this(0);
    }

    /**
     * The constructor creates and initializes an empty trie.
     * This method sets the expected universe size to a known value (this is just optimization will and will
     * automatically updated if bigger sets are inserted).
     */
    public BitSetTrie(int universeSize) {
        this.universeSize = universeSize;
        this.clear();
    }

    /**
     * Clears the BitSetTrie, that is, removes all elements stored in the trie.
     * This method will just delete a pointer and, thus, runs in constant time independent of the size of the trie.
     */
    public void clear() {
        root = new Node(null, -1);
        containsEmptySet = false;
    }

    /**
     * Insert the given set to the trie by crawling down the trie and, eventually, create nodes on the path.
     * The running time is \(O(|U|)\) where \(U\) is the universe and \(s\) the set to be inserted.
     *
     * Note that there are actually just \(O(|s|)\) real operations are performed, however, since we BitSets we actually
     * have to iterate over the whole bitmask which gives us the \(O(|U|)\) term (which is still fast because its a
     * simple bit iteration).
     *
     * @param s The bitset we add.
     */
    public void insert(BitSet s) {
        if (s.cardinality() == 0) { containsEmptySet = true; return; } // empty set is special
        if (s.size() > universeSize) universeSize = s.size(); // update universe size

        // crawl to the node containing s
        Node crawler = root;
        int e = -1;
        while ( (e = s.nextSetBit(e+1)) != -1 ) { // while there are elements in s
            if (crawler.children.containsKey(e)) { // label already there -> follow
                crawler = crawler.children.get(e);
            } else { // create a new child
                Node child = new Node(crawler, e);
                crawler.children.put(e, child);
                crawler = child;
            }
        }

        // mark the node containing s
        crawler.marked = true;
    }

    /**
     * Checks whether or not the given trie stores the given set \(s\) by traversing the trie and searching for a corresponding
     * marked node.
     * The running time is \(O(|U|)\) where \(U\) is the universe.
     *
     * Note that there are actually just \(O(|s|)\) real operations are performed, however, since we BitSets we actually
     * have to iterate over the whole bitmask which gives us the \(O(|U|)\) term (which is still fast because its a
     * simple bit iteration).
     *
     * @param s The bitset we test.
     * @return True if the bitset is contained in the tree.
     */
    public boolean contains(BitSet s) {
        if (s.cardinality() == 0) return containsEmptySet; // handle empty set

        // crawl to a node that would contain s
        Node crawler = root;
        int e = -1;
        while ( (e = s.nextSetBit(e+1)) != -1 ) { // while there are elements in s
            if (!crawler.children.containsKey(e)) return false;
            crawler = crawler.children.get(e);
        }

        // check if the node stores something
        return crawler.marked;
    }

    /**
     * Removes the given set \(s\) from the trie by traversing the trie and unmarking the corresponding node.
     * If the node was a leaf the method shrinks the trie by cutting of the branch of \(s\) up to the first node which
     * is either marked or has multiple children.
     * The running time is \(O(|U|)\) where \(U\) is the universe.
     *
     * Note that there are actually just \(O(|s|)\) real operations are performed, however, since we BitSets we actually
     * have to iterate over the whole bitmask which gives us the \(O(|U|)\) term (which is still fast because its a
     * simple bit iteration).
     *
     * @param s The bitset we want to remove.
     */
    public void remove(BitSet s) {
        if (s.cardinality() == 0) { containsEmptySet = false; return; } // handle empty set

        // crawl to a node that would contain s
        Node crawler = root;
        int e = -1;
        while ( (e = s.nextSetBit(e+1)) != -1 ) { // while there are elements in s
            if (!crawler.children.containsKey(e)) return; // s is not in the trie
            crawler = crawler.children.get(e);
        }

        // remove s from the trie
        crawler.marked = false;

        // try to shrink the trie
        while (!crawler.marked && crawler.children.size() == 0) {
            // crawler is unmarked and has no children -> delete it
            Node parent = crawler.parent;
            for (int element : parent.children.keySet()) {
                if (parent.children.get(element) == crawler) {
                    parent.children.remove(element);
                    break;
                }
            }
            crawler = parent;
        }
    }

    //Mark: Subset Iterator

    /**
     * Returns an iterator over the subsets of the given set s that are stored in the trie.
     * If the given set is stored in the trie, the iterator will iterate over it as well.
     * @param s The bitset we query.
     * @return An iterator over contained subsets.
     */
    public Iterable<BitSet> getSubSets(BitSet s) {
        return new Iterable<BitSet>() {
            @Override
            public Iterator<BitSet> iterator() {
                return new SubSetIterator(s);
            }
        };
    }

    /**
     * An iterator over subsets stored in the trie (including s).
     * The iterator traverses the trie in pre-order and outputs the bitsets for marked nodes. While doing so,
     * the iterator uses only edges set in the given set. The running time is \(O(|T|*|U|\) where \(T\) is the the trie
     * and \(U\) is the universe. For instances, if \(s\) contains only ones this methods iterates over all sets stored
     * in the trie. However, if \(s\) becomes smaller this method becomes more efficient. Note that the size of the trie
     * can be exponential in the size of the universe.
     */
    class SubSetIterator implements Iterator<BitSet> {

        private Stack<Node> stack;
        private BitSet s;
        private BitSet next;
        private boolean returnedEmptySet;

        public SubSetIterator(BitSet s) {
            this.s = s;
            this.stack = new Stack<>();
            this.stack.push(root);
            this.returnedEmptySet = false;
            this.next = successor();
        }

        private BitSet successor() {
            // handle empty set
            if (containsEmptySet && !returnedEmptySet) {
                returnedEmptySet = true;
                return new BitSet();
            }
            // traverse the trie
            while (!stack.isEmpty()) {
                Node v = stack.pop();
                for (int i = s.length(); (i = s.previousSetBit(i - 1)) > v.label; ) {
                    if (v.children.containsKey(i)) stack.push(v.children.get(i));
                }
                if (v.marked) { // construct found bitset
                    BitSet nextSet = new BitSet();
                    while (v.label >= 0) {
                        nextSet.set(v.label);
                        v = v.parent;
                    }
                    return nextSet;
                }
            }
            // trie search done
            return null;
        }

        @Override
        public boolean hasNext() {
            return next != null;
        }

        @Override
        public BitSet next() {
            BitSet tmp = next;
            next = successor();
            return tmp;
        }
    }

    //MARK: Maximal SubSet Iterator

    /**
     * Returns an iterator over the inclusion maximal subsets of the given set s that are stored in the trie.
     * If the given set is stored in the trie, the iterator will iterate over it as well.
     * @param s The bitset we query.
     * @return An iterator over bitsets that contain the given one.
     */
    public Iterable<BitSet> getMaxSubSets(BitSet s) {
        return new Iterable<BitSet>() {
            @Override
            public Iterator<BitSet> iterator() {
                return new MaxSubSetIterator(s);
            }
        };
    }

    /**
     * An iterator over inclusion maximal subsets stored in the trie (including s).
     * The iterator traverses the trie in pre-order and outputs the bitsets for marked nodes that have no more children. While doing so,
     * the iterator uses only edges set in the given set. The running time is \(O(|T|*|U|\) where \(T\) is the the trie
     * and \(U\) is the universe.
     */
    class MaxSubSetIterator implements Iterator<BitSet> {

        private Stack<Node> stack;
        private BitSet s;
        private BitSet next;
        private boolean returnedEmptySet;

        public MaxSubSetIterator(BitSet s) {
            this.s = s;
            this.stack = new Stack<>();
            this.stack.push(root);
            this.returnedEmptySet = false;
            this.next = successor();
        }

        private BitSet successor() {
            // handle empty set
            if (containsEmptySet && !returnedEmptySet) {
                returnedEmptySet = true;
                return new BitSet();
            }
            // traverse the trie
            while (!stack.isEmpty()) {
                Node v = stack.pop();
                boolean noChildren = true;
                for (int i = s.length(); (i = s.previousSetBit(i - 1)) > v.label; ) {
                    if (v.children.containsKey(i)){
                        stack.push(v.children.get(i));
                        noChildren = false;
                    }
                }
                if (v.marked && noChildren) { // construct found bitset
                    BitSet nextSet = new BitSet();
                    while (v.label >= 0) {
                        nextSet.set(v.label);
                        v = v.parent;
                    }
                    return nextSet;
                }
            }
            // trie search done
            return null;
        }

        @Override
        public boolean hasNext() {
            return next != null;
        }

        @Override
        public BitSet next() {
            BitSet tmp = next;
            next = successor();
            return tmp;
        }
    }

    //MARK: Superset Iterator

    /**
     * Returns an iterator over supersets of the given set s.
     * If the given set is stored in the trie, the iterator will iterate over it as well.
     * @param s The bitset we query.
     * @return An iterator over supersets.
     */
    public Iterable<BitSet> getSuperSets(BitSet s) {
        return new Iterable<BitSet>() {
            @Override
            public Iterator<BitSet> iterator() {
                return new SuperSetIterator(s);
            }
        };
    }

    /**
     * An iterator over supersets stored in the trie (including s).
     * The iterator traverses the trie in pre-order and outputs the bitsets of marked nodes.
     * Thereby the iterator uses all edges with labels smaller then the current smallest label in the set, and the one of
     * the smallest label in the set.
     *
     * The running time is \(O(|T|*|U|)\) where \(T\) is the trie, i.e., if \(s\) is empty this iterates over all sets stored
     * in the trie. However, if \(s\) becomes larger this method becomes more efficient. Note that |T| can be exponential
     * in the universe size.
     */
    class SuperSetIterator implements Iterator<BitSet> {

        private Stack<Node> stack;
        private BitSet s;
        private BitSet next;
        private boolean returnedEmptySet;

        public SuperSetIterator(BitSet s) {
            this.s = s;
            this.stack = new Stack<>();
            this.stack.push(root);
            this.returnedEmptySet = false;
            this.next = successor();
        }

        private BitSet successor() {
            // handle empty set
            if (containsEmptySet && !returnedEmptySet && s.cardinality() == 0) {
                returnedEmptySet = true;
                return new BitSet();
            }
            // find next marked node
            while (!stack.isEmpty()) {
                Node v = stack.pop();
                int i = s.nextSetBit(v.label +1);
                if (i < 0) i = universeSize; // no label left, we can add all
                do { // insert elements up to the first label of s
                    if (v.children.containsKey(i)) stack.push(v.children.get(i));
                    i--;
                } while (i > v.label);
                // if v is marked and we have reached every bit in s
                if (v.marked && s.nextSetBit(v.label+1) == -1) {
                    BitSet nextSet = new BitSet();
                    while (v.label >= 0) { // constructed the stored set
                        nextSet.set(v.label);
                        v = v.parent;
                    }
                    return nextSet;
                }
            }
            // trie search done
            return null;
        }

        @Override
        public boolean hasNext() {
            return next != null;
        }

        @Override
        public BitSet next() {
            BitSet tmp = next;
            next = successor();
            return tmp;
        }
    }

    //MARK: internal node class

    /**
     * A node of the trie is labeled with an label of the universe and may be marked. If the node is marked, the set
     * consisting of all labels on the unique path from the node to the root is stored in the trie.
     * The node also stores adjacency information to its children and to its unique parent.
     */
    private class Node {
        Node parent;
        Map<Integer, Node> children;
        int label; // label with wish the node is labeled
        boolean marked; // flag that indicates if the given set is in the trie
        public Node(Node parent, int element) {
            this.parent = parent;
            this.children = new HashMap<>();
            this.label = element;
            this.marked = false;
        }
    }
}
