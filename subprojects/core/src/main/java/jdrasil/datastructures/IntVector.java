package jdrasil.datastructures;

import java.util.Arrays;

public class IntVector {
	int[] array;
	int size;
	
	public IntVector(){
		size = 0;
		array = new int[16];
	}
	
	public IntVector(IntVector original){
		array = Arrays.copyOf(original.array, original.array.length);
		size = original.size;
	}
	
	public void push(int elem){
		if(size >= array.length)
			changeCapacity((3*size)/2);
		array[size++] = elem;
	}
	
	public void set(int index, int elem){
		array[index] = elem;
	}
	
	public int get(int index){
		return array[index];
	}
	
	void changeCapacity(int newCap){
		array = Arrays.copyOf(array, newCap);
	}
	
	public void popReplace(int index){
		array[index] = array[size-1];
		size--;
	}
	
	public void popBack(){
		if(size == 0)
			throw new RuntimeException("Cannot pop from empty IntVector");
		size--;
	}
	
	public int size(){
		return size;
	}
	
}
