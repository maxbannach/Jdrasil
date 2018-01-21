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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Partition Refinement is a data structure to represent a partition of a set
 * that allows to refine this set.
 * 
 * Let \(S\) be a set and \(P={S_1,...,S_n}\) be a partition of \(S\). Let, furthermore, \(X\) be a subset of \(S\).
 * The refinement of \(P\) with respect to \(X\) is defined as:
 *   refinement\((P,X) = {S_1 \cap X, S_1 \setminus X, ..., S_n \cap X, S_n \setminus X}\) where elements that do not
 *   intersect \(X\) are leaved unchanged.
 * 
 * This operation can be computed in time \(O(|X|)\) independently of \(|S|\) and \(n\).
 * See Qinna Wang – Partition Refinement: a meaningful technique for Graphs
 * 
 * @param <T>
 * @author Max Bannach
 */
public class PartitionRefinement<T> {

	/** This stores the actual partition as a mapping from elements to the sets they lie in. */
	private final Map<T, Set<T>> partition;
	
	/** During a refine step new sets may be introduced, which are stored in this map. */
	private final Map<Set<T>, Set<T>> nextSet;
	
	/**
	 * Initialize the internal datastructures and computes a trivial partition of the
	 * given universe: P = {universe},
	 * @param universe
	 */
	public PartitionRefinement(Set<T> universe) {
		
		// init internal data structures
		this.partition = new HashMap<>();
		this.nextSet = new HashMap<>();
		
		// we have to ensure that we use HashSets for O(1) operations
		HashSet<T> U;
		if (universe instanceof HashSet) {
			U = (HashSet<T>) universe;
		} else {
			U = new HashSet<>(universe);
		}
		
		// compute start partition -> all in the only set (the universe)
		for (T e : U) {
			partition.put(e, U);
		}
	}
	
	/**
	 * Refines the stored partition of the original set, i.e. it applies:
	 * refinement(P,X) = {S_1 cap X, S_1 minus X, ..., S_n cap X, S_n minus X}
	 * 
	 * This method runs in time O(|X|).
	 * 
	 * @param X
	 */
	public void refine(Set<T> X) {
		
		for (T x : X) {
			// get the set containing x in O(1)
			Set<T> Si = partition.get(x);
			
			// get the set we put x to (or create it) in O(1)
			Set<T> Sj = null;
			if (!nextSet.containsKey(Si)) {
				nextSet.put(Si, new HashSet<T>());
			}
			Sj = nextSet.get(Si);
			
			// remove x from this set, it becomes Si minus X in O(1)
			Si.remove(x);
			
			// since Si has changed we have to update the data structure
			nextSet.put(Si, Sj);
			
			// add x to this set
			Sj.add(x);
			
			// update the set x is in, O(1)
			partition.put(x, nextSet.get(Si));
		}
		
		// clear data structure for next round, O(|X|) as not more elements are created
		nextSet.clear();
		
	}

	/**
	 * Get the currently stored partition as map from elements to subsets of the universe.
	 * @return
	 */
	public Map<T, Set<T>> getPartition() {
		return partition;
	}
	
}
