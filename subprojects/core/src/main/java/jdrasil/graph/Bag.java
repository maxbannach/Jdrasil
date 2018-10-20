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

import java.io.Serializable;
import java.util.Collection;
import java.util.Set;

/**
 * This class models a bag of the tree-decomposition. It holds a set of vertices of the original graph of type T.
 * 
 * @author Max Bannach
 */
public class Bag<T extends Comparable<T>> implements Comparable<Bag<T>>, Serializable {
	
	private static final long serialVersionUID = -2010473680565903696L;

	/** The vertices stored in this bag. */
	public Set<T> vertices;
	
	/** Each bag has an id in its tree-decomposition. */
	public int id;
	
	/**
	 * A bag is just constructed with corresponding vertices.
	 * @param vertices Vertices added to the bag.
	 */
	Bag(Set<T> vertices, int id) {
		this.vertices = vertices;
		this.id = id;
	}
	
	/**
	 * Checks if a given vertex is part of this bag.
	 * @param v the vertex to be checked
	 * @return true if the vertex is in the bag
	 */
	public boolean contains(T v) {
		return this.vertices.contains(v);
	}
	
	/**
	 * Check if the given vertices are all in this bag.
	 * @param vertices that may be in the bag
	 * @return true if all vertices are in the bag
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
