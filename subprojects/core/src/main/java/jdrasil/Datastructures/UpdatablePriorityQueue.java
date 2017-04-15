/**
 * 
 */
package jdrasil.Datastructures;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * @author thorsten
 *
 */
public class UpdatablePriorityQueue<T extends Comparable<T>> {
	
	private Map<Integer, T> 			values;
	private Map<Integer, Integer>		indexInQueue;
	private ArrayList<Integer> 			heap;
	
	UpdatablePriorityQueue(){
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
	public void insert(int elem, T value){
		values.put(elem,  value);
		indexInQueue.put(elem, heap.size());
		heap.add(elem);
		upheap(heap.size()-1);
	}
	
	/**
	 * 
	 * @return An element of this queue with minimum priority
	 */
	public int getMin(){
		return heap.get(0);
	}
	
	/**
	 * Return and remove an alement with minimum priority. 
	 * @return
	 */
	public int removeMin(){
		int rVal = heap.get(0);
		heap.set(0, heap.get(heap.size()-1));
		indexInQueue.put(heap.get(0), 0);
		heap.remove(heap.size()-1);
		downHeap(0);
		return rVal;
	}
	
	public void updateValue(int elem, T newValue){
		values.put(elem, newValue);
		upheap(indexInQueue.get(elem));
		downHeap(indexInQueue.get(elem));
	}
	
	public T getMinPrio(){
		if(heap.size() <= 0)
			throw new RuntimeException("Cannot remove from empty heap! ");
		return values.get(heap.get(0));
	}
	/**
	 * Move a key upwards in the heap
	 * @param index the index to start from
	 */
	private void upheap(int index){
		if(index <= 0)
			return;
		int x = heap.get(index);
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
		int x = heap.get(index);
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
}
