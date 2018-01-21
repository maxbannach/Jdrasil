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

import jdrasil.datastructures.PartitionRefinement;

import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Test for the PartitionRefinement class that generates pseudo random sets and subsets and partition them manually.
 *
 * @author Max Bannach
 */
public class PartitionRefinmentTest {

    /* size of the used pseudo random set */
    private final int SET_SIZE = 1000;

    /* number of iterative refinement */
    private final int ROUNDS = 250;

    /** Generate a pseudo random set of integers */
    private Set<Integer> pseudoRandomSet() {
        Random rng = new Random(42);
        Set<Integer> S = new HashSet<>();
        for (int i = 0; i < SET_SIZE; i++) S.add(rng.nextInt());
        return S;
    }

    /** Generate a pseudo random set of integers */
    private Set<Integer> pseudoRandomSubSet(Set<Integer> S, int i) {
        Random rng = new Random(i);
        Set<Integer> sub = new HashSet<>();
        for (int e : S) if (rng.nextBoolean()) sub.add(e);
        return sub;
    }

    @org.junit.Test
    public void empty() throws Exception {
        Set<Integer> S = new HashSet<>();
        PartitionRefinement<Integer> PR = new PartitionRefinement(S);
        assertEquals(PR.getPartition().keySet().size(), 0);
    }

    @org.junit.Test
    public void noRefinement() throws Exception {
        Set<Integer> S = pseudoRandomSet();
        PartitionRefinement<Integer> PR = new PartitionRefinement(S);
        for (Integer e : S) assertEquals(PR.getPartition().get(e), S);
    }

    @org.junit.Test
    public void refinement() throws Exception {
        Set<Integer> S = pseudoRandomSet();
        PartitionRefinement<Integer> PR = new PartitionRefinement(S);

        // set to refine with
        Set<Integer> X = pseudoRandomSubSet(S, 0);

        // manual refine
        Set<Integer> S1 = new HashSet<>(S);
        Set<Integer> S2 = new HashSet<>(S);
        S1.removeAll(X);
        S2.retainAll(X);

        // partition refinement
        PR.refine(X);
        Map<Integer, Set<Integer>> partition = PR.getPartition();
        for (int e : S1) assertEquals(partition.get(e), S1);
        for (int e : S2) assertEquals(partition.get(e), S2);
        for (int e : S)  assertEquals(partition.get(e), X.contains(e) ? S2 : S1);
    }

    @org.junit.Test
    public void iterativeRefinement() throws Exception {
        Set<Integer> S = pseudoRandomSet();
        PartitionRefinement<Integer> PR = new PartitionRefinement(S);
        Set<Set<Integer>> myPartition = new HashSet<>();
        myPartition.add(S);

        // iteratively refine the partition
        for (int i = 0; i < ROUNDS; i++) {

            // get next set to refine with
            Set<Integer> X = pseudoRandomSubSet(S, i);

            // refine manually
            Set<Set<Integer>> newParition = new HashSet<>();
            for (Set<Integer> Y : myPartition) {
                Set<Integer> A = new HashSet<>(Y);
                Set<Integer> B = new HashSet<>(Y);
                A.removeAll(X);
                B.retainAll(X);
                newParition.add(A); // may be Y
                if (B.size() > 0) newParition.add(B);
            }
            myPartition = newParition;

            // partition with PR
            PR.refine(X);
            Map<Integer, Set<Integer>> partition = PR.getPartition();

            // test equality (forward direction)
            for (Set<Integer> Y : myPartition) {
                for (int e : Y) assertEquals(partition.get(e), Y);
            }

            // test equality (backward direction)
            for (int e : S) {
                assertTrue(partition.get(e).contains(e));
                assertTrue(myPartition.contains(partition.get(e)));
            }
        }
    }

}
