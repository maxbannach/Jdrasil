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
package jdrasil.utilities;

import jdrasil.datastructures.BitSetTrie;

import java.util.BitSet;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Test for the BitSetTrie that performs different pseudo random sequences of adding, removing and querying bitsets.
 * Furthermore, pseudo random sets are created and there sub- and super-sets are computed manually and compared with
 * the iterator of the BitSetTrie.
 *
 * @author Max Bannach
 */
public class BitSetTrieTest {

    /* size of bitsets inserted to the trie (needed to randomly generate them) */
    private final int BITSET_SIZE = 128;

    /* how many bitsets are inserted into the trie per test? */
    private final int TEST_SIZE = 4096;

    /* Seed for the random number generator used to create bitsets */
    private final long SEED = 123456789;

    @org.junit.Test
    public void simpleInsertContains() throws Exception {
        BitSetTrie T = new BitSetTrie();
        Random rng = new Random(SEED);
        BitSet set = new BitSet();
        for (int i = 0; i < BITSET_SIZE; i++) set.set(i, rng.nextBoolean());
        assertFalse(T.contains(set));
        T.insert(set);
        assertTrue(T.contains(set));
    }

    @org.junit.Test
    public void emptyInsertContains() throws Exception {
        BitSetTrie T = new BitSetTrie();
        BitSet set = new BitSet();
        assertFalse(T.contains(set));
        T.insert(set);
        assertTrue(T.contains(set));
        T.remove(set);
        assertFalse(T.contains(set));
    }

    @org.junit.Test
    public void insertContainsSequence() throws Exception {
        BitSetTrie T = new BitSetTrie();
        Set<BitSet> sets = new HashSet<>();

        // insert and test
        Random rng = new Random(SEED);
        for (int i = 0; i < TEST_SIZE; i++) {
            BitSet set = new BitSet();
            for (int j = 0; j < BITSET_SIZE; j++) set.set(j, rng.nextBoolean());
            if (sets.contains(set)) continue;;
            assertFalse(T.contains(set));
            T.insert(set);
            assertTrue(T.contains(set));
            sets.add(set);
        }

        // test after construction
        for (BitSet set : sets) assertTrue(T.contains(set));
    }

    @org.junit.Test
    public void insertRemoveContainsSequence() throws Exception {
        BitSetTrie T = new BitSetTrie();
        Set<BitSet> sets = new HashSet<>();
        Set<BitSet> removed = new HashSet<>();

        // insert and test
        Random rng = new Random(SEED);
        for (int i = 0; i < TEST_SIZE; i++) {
            BitSet set = new BitSet();
            for (int j = 0; j < BITSET_SIZE; j++) set.set(j, rng.nextBoolean());
            if (sets.contains(set)) continue;;
            assertFalse(T.contains(set));
            T.insert(set);
            assertTrue(T.contains(set));
            sets.add(set);
        }

        // remove some
        for (BitSet set : sets) {
            if (rng.nextBoolean()) continue;
            assertTrue(T.contains(set));
            T.remove(set);
            assertFalse(T.contains(set));
            removed.add(set);
        }
        sets.removeAll(removed);

        // test after construction
        for (BitSet set : sets) assertTrue(T.contains(set));
        for (BitSet set : removed) assertFalse(T.contains(set));
    }

    @org.junit.Test
    public void iterateSubsets() throws Exception {

        // build a trie
        BitSetTrie T = new BitSetTrie();
        Set<BitSet> sets = new HashSet<>();

        // insert some sets (already tested)
        Random rng = new Random(SEED);
        for (int i = 0; i < TEST_SIZE; i++) {
            BitSet set = new BitSet();
            for (int j = 0; j < BITSET_SIZE; j++) set.set(j, rng.nextBoolean());
            if (sets.contains(set) || set.cardinality() == 0) continue;;
            assertFalse(T.contains(set));
            T.insert(set);
            assertTrue(T.contains(set));
            sets.add(set);
        }

        // compute the subsets in T of each set, and check if the iterator provide exactly these sets
        for (BitSet set : sets) {
            // compute subsets
            Set<BitSet> subsets = new HashSet<>();
            for (BitSet subset : sets) {
                boolean isSubset = true;
                for (int i = subset.nextSetBit(0); i >= 0; i = subset.nextSetBit(i+1)) if (!set.get(i)) { isSubset = false; break; };
                if (isSubset) subsets.add(subset);
            }
            // check was the trie does
            Set<BitSet> Tsubsets = new HashSet<>();
            for (BitSet subset : T.getSubSets(set)) Tsubsets.add(subset);
            assertEquals(subsets, Tsubsets);
        }
    }

    @org.junit.Test
    public void iterateSubsetsWithEmptySet() throws Exception {

        // build a trie
        BitSetTrie T = new BitSetTrie();
        Set<BitSet> sets = new HashSet<>();

        // insert some sets (already tested)
        Random rng = new Random(SEED);
        for (int i = 0; i < TEST_SIZE; i++) {
            BitSet set = new BitSet();
            for (int j = 0; j < BITSET_SIZE; j++) set.set(j, rng.nextBoolean());
            if (sets.contains(set)) continue;;
            assertFalse(T.contains(set));
            T.insert(set);
            assertTrue(T.contains(set));
            sets.add(set);
        }
        BitSet empty = new BitSet();
        if (!sets.contains(empty)) {
            T.insert(empty);
            sets.add(empty);
            assertTrue(T.contains(empty));
        }

        // compute the subsets in T of each set, and check if the iterator provide exactly these sets
        for (BitSet set : sets) {
            // compute subsets
            Set<BitSet> subsets = new HashSet<>();
            for (BitSet subset : sets) {
                boolean isSubset = true;
                for (int i = subset.nextSetBit(0); i >= 0; i = subset.nextSetBit(i+1)) if (!set.get(i)) { isSubset = false; break; };
                if (isSubset) subsets.add(subset);
            }
            // check was the trie does
            Set<BitSet> Tsubsets = new HashSet<>();
            for (BitSet subset : T.getSubSets(set)) Tsubsets.add(subset);
            assertEquals(subsets, Tsubsets);
        }
    }

    @org.junit.Test
    public void iterateSupersets() throws Exception {

        // build a trie
        BitSetTrie T = new BitSetTrie();
        Set<BitSet> sets = new HashSet<>();

        // insert some sets (already tested)
        Random rng = new Random(SEED);
        for (int i = 0; i < TEST_SIZE; i++) {
            BitSet set = new BitSet();
            for (int j = 0; j < BITSET_SIZE; j++) set.set(j, rng.nextBoolean());
            if (sets.contains(set) || set.cardinality() == 0) continue;;
            assertFalse(T.contains(set));
            T.insert(set);
            assertTrue(T.contains(set));
            sets.add(set);
        }

        // compute the supersets in T of each set, and check if the iterator provide exactly these sets
        for (BitSet set : sets) {
            // compute supersets
            Set<BitSet> supersets = new HashSet<>();
            for (BitSet superset : sets) {
                boolean isSuperset = true;
                for (int i = set.nextSetBit(0); i >= 0; i = set.nextSetBit(i+1)) if (!superset.get(i)) { isSuperset = false; break; };
                if (isSuperset) supersets.add(superset);
            }
            // check was the trie does
            Set<BitSet> Tsupersets = new HashSet<>();
            for (BitSet superset : T.getSuperSets(set)) Tsupersets.add(superset);
            assertEquals(supersets, Tsupersets);
        }
    }

    @org.junit.Test
    public void iterateSupersetsWithEmpty() throws Exception {

        // build a trie
        BitSetTrie T = new BitSetTrie();
        Set<BitSet> sets = new HashSet<>();

        // insert some sets (already tested)
        Random rng = new Random(SEED);
        for (int i = 0; i < TEST_SIZE; i++) {
            BitSet set = new BitSet();
            for (int j = 0; j < BITSET_SIZE; j++) set.set(j, rng.nextBoolean());
            if (sets.contains(set)) continue;;
            assertFalse(T.contains(set));
            T.insert(set);
            assertTrue(T.contains(set));
            sets.add(set);
        }
        BitSet empty = new BitSet();
        if (!sets.contains(empty)) {
            T.insert(empty);
            sets.add(empty);
            assertTrue(T.contains(empty));
        }

        // compute the supersets in T of each set, and check if the iterator provide exactly these sets
        for (BitSet set : sets) {
            // compute supersets
            Set<BitSet> supersets = new HashSet<>();
            for (BitSet superset : sets) {
                boolean isSuperset = true;
                for (int i = set.nextSetBit(0); i >= 0; i = set.nextSetBit(i+1)) if (!superset.get(i)) { isSuperset = false; break; };
                if (isSuperset) supersets.add(superset);
            }
            // check was the trie does
            Set<BitSet> Tsupersets = new HashSet<>();
            for (BitSet superset : T.getSuperSets(set)) Tsupersets.add(superset);
            assertEquals(supersets, Tsupersets);
        }
    }

}
