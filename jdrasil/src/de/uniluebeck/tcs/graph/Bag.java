package de.uniluebeck.tcs.graph;

/**
 * Bag.java
 * @author bannach
 */

import java.io.Serializable;
import java.util.Collection;
import java.util.Set;

/**
 * This class models a bag of the tree-decomposition. It holds a set of vertices of the original graph of type T.
 */
public class Bag<T extends Comparable<T>> implements Comparable<Bag<T>>, Serializable {
	
	private static final long serialVersionUID = -2010473680565903696L;

	/** The vertices stored in this bag. */
	public Set<T> vertices;
	
	/** Each bag has an id in its tree-decomposition. */
	public final int id;
	
	/**
	 * A bag is just constructed with corresponding vertices.
	 * @param vertices
	 */
	Bag(Set<T> vertices, int id) {
		this.vertices = vertices;
		this.id = id;
	}
	
	/**
	 * Checks if a given vertex is part of this bag.
	 * @param v
	 * @return
	 */
	public boolean contains(T v) {
		return this.vertices.contains(v);
	}
	
	/**
	 * Check if the given vertices are all in this bag.
	 * @param vertices
	 * @return
	 */
	public boolean containsAll(Collection<T> vertices) {
		return this.vertices.containsAll(vertices);
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(id);
		for (T v : vertices) {
			sb.append(" " + v);
		}
		return sb.toString();
	}

	@Override
	public int compareTo(Bag<T> o) {
		return this.id - o.id;
	}
}
