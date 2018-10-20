package jdrasil.datastructures;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import jdrasil.utilities.RandomNumberGenerator;

/**
 * @author thorsten
 *
 */
public class UpdatablePriorityQueue<E, V extends Comparable<V>> {
	
	private Map<E, V> 			values;
	private Map<E, Integer>		indexInQueue;
	private ArrayList<E> 			heap;
	
	public UpdatablePriorityQueue(){
		values 			= new HashMap<>();
		indexInQueue 	= new HashMap<>();
		heap 			= new ArrayList<>();
	}
	
	
	/**
	 * Add a new element to this priority queue. 
	 * Following the "standard" implementation: Add it to the end of the array, and move 
	 * it upwards as much as possible.
	 * @param elem
	 * @param value
	 */
	public void insert(E elem, V value){
		values.put(elem,  value);
		indexInQueue.put(elem, heap.size());
		heap.add(elem);
		upheap(heap.size()-1);
	}
	
	/**
	 * 
	 * @return An element of this queue with minimum priority
	 */
	public E getMin(){
		return heap.get(0);
	}
	
	/**
	 * Return and remove an alement with minimum priority. 
	 * @return
	 */
	public E removeMin(){
		E rVal = heap.get(0);
		heap.set(0, heap.get(heap.size()-1));
		indexInQueue.put(heap.get(0), 0);
		heap.remove(heap.size()-1);
		if(!heap.isEmpty())
			downHeap(0);
		return rVal;
	}
	
	/**
	 * Find one element with minimum priority, and swap it to the root.
	 * This implementation chooses a path to one node of the induced subtree containing elements with minimum priority 
	 * randomly, thus, this choice is not uniform. 
	 * Other suggestions are highly welcome :) 
	 * 
	 * @return
	 */
	public E removeMinRandom(){
		if(heap.size() <= 0)
			throw new RuntimeException("Cannt remove from empty heap! ");
		int index = 0;
		V lowestPrio = values.get(heap.get(0));
		while(rightSon(index) < heap.size()){
			boolean goLeft = RandomNumberGenerator.nextBoolean();
			if(goLeft && values.get(heap.get(leftSon(index))).compareTo(lowestPrio) == 0){
				index = leftSon(index);
			}
			else if(!goLeft && values.get(heap.get(rightSon(index))).compareTo(lowestPrio) == 0){
				index = rightSon(index);
			}
			else
				break;
			
		}
		// Now swap the element at position 0 with the one at position "index"
		if(index != 0){
			E s = heap.get(index);
			heap.set(index,  heap.get(0));
			heap.set(0,  s);
			indexInQueue.put(heap.get(index), index);
			indexInQueue.put(heap.get(0), 0);
		}
		
		return removeMin();
	}
	
	public void updateValue(E elem, V newValue){
		if(!indexInQueue.containsKey(elem)){
			throw new RuntimeException("Could not find " + elem.toString());
		}
		values.put(elem, newValue);
		upheap(indexInQueue.get(elem));
		downHeap(indexInQueue.get(elem));
	}
	
	public V getMinPrio(){
		if(heap.size() <= 0)
			throw new RuntimeException("Cannot remove from empty heap! ");
		return values.get(heap.get(0));
	}
	
	public int size(){
		return heap.size();
	}
	/**
	 * Move a key upwards in the heap
	 * @param index the index to start from
	 */
	private void upheap(int index){
		if(index <= 0)
			return;
		E x = heap.get(index);
        int f = father(index);
        
        while(index > 0 && values.get(x).compareTo(values.get(heap.get(f))) < 0){
            heap.set(index, heap.get(f));
            indexInQueue.put(heap.get(f), index);
            index=f;
            f=father(f);
        }
        heap.set(index,  x);
        indexInQueue.put(x, index);
	}
	/**
	 * Move a value down the heap
	 * @param index
	 */
	private void downHeap(int index){
		E x = heap.get(index);
		while(leftSon(index) < heap.size()){
			int minSon = leftSon(index);
			if(rightSon(index) < heap.size() && values.get(heap.get(rightSon(index))).compareTo(values.get(heap.get(minSon))) < 0){
				minSon = rightSon(index);
			}
			if(values.get(heap.get(minSon)).compareTo(values.get(x)) < 0){
				heap.set(index,  heap.get(minSon));
				indexInQueue.put(heap.get(index), index);
				index = minSon;
				
			}
			else
				break;
		}
		heap.set(index, x);
		indexInQueue.put(x,  index);
		
	}
	private int leftSon(int index){
		return 2*index+1;
	}
	private int rightSon(int index){
		return 2*index+2;
	}
	private int father(int index){
		return (index-1)/2;
	}
	
	public void checkIsHeap(){
		if(heap.size() == 0)
			return;
		checkIsHeap(0);
		
	}
	private void checkIsHeap(int index){
		if(leftSon(index) < heap.size()){
			if(values.get(heap.get(index)).compareTo(values.get(heap.get(leftSon(index)))) > 0)
				throw new RuntimeException("not heap, index=" + index);
			checkIsHeap(leftSon(index));
		}
		if(rightSon(index) < heap.size()){
			if(values.get(heap.get(index)).compareTo(values.get(heap.get(rightSon(index)))) > 0)
				throw new RuntimeException("not heap, index=" + index);
			checkIsHeap(rightSon(index));
		}
	}
}
