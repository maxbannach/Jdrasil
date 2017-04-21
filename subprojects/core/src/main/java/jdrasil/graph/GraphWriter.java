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

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * This class provides various methods to print graphs and tree decomposition to different streams.
 *  
 * @author Max Bannach
 */
public class GraphWriter {

	// hide the constructor
	private GraphWriter() {}
	
	/**
	 * Constitutes a string representing the graph.
	 * The string is like the PACE graph format, i.e., if the type of the vertices is Integer,
	 * this will exactly produce the PACE format.
	 * @param graph that should be serialized
	 * @return string representing the graph
	 */
	public static <T extends Comparable<T>> String graphToString(Graph<T> graph) {
		StringBuilder sb = new StringBuilder();
		sb.append("p tw " + graph.getCopyOfVertices().size() + " " + graph.getNumberOfEdges() + "\n");
		for (T v : graph.getCopyOfVertices()) {
			for (T w : graph.getNeighborhood(v)) {
				if (v.compareTo(w) > 0) continue;
				sb.append(v + " " + w + "\n");
			}
		}
		return sb.toString();
	}

	/**
	 * Constitutes a string representing the directed graph.
	 * @param graph that should be serialized
	 * @return string representing the graph
	 */
	public static <T extends Comparable<T>> String directedGraphToString(Graph<T> graph) {
		StringBuilder sb = new StringBuilder();
		sb.append("p tw " + graph.getCopyOfVertices().size() + " " + graph.getNumberOfEdges() + "\n");
		for (T v : graph.getCopyOfVertices()) {
			for (T w : graph.getNeighborhood(v)) {
				sb.append(v + " " + w + "\n");
			}
		}
		return sb.toString();
	}
	
	/**
	 * Write the string of @see graphToString to System.out.
	 * @param graph
	 * @throws IOException
	 */
	public static <T extends Comparable<T>> void writeGraph(Graph<T> graph) throws IOException {
		writeGraph(graph, System.out);
	}
	
	/**
	 * Write the string of @see graphToString to the given output stream.
	 * @param graph
	 * @throws IOException
	 */
	public static <T extends Comparable<T>> void writeGraph(Graph<T> graph, OutputStream stream) throws IOException {
		BufferedWriter out = new BufferedWriter(new OutputStreamWriter(stream));
		out.write(graphToString(graph));
		out.flush();
		out.close();
	}
	
	/**
	 * Writes down a tree decomposition in the format specified by PACE
	 * @param td the decomposition to be serialized
	 * @return the serialized string
	 */
	public static <T extends Comparable<T>> String treedecompositionToString(TreeDecomposition<T> td) {
		StringBuilder sb = new StringBuilder();	
		
		// print solution line
		sb.append("s td ");
		sb.append(td.numberOfBags + " ");
		sb.append((td.width+1) + " ");
		sb.append(td.n);
		
		// print the bags
		for (Bag<?> bag : td.tree) {
			sb.append("\nb " + bag);
		}
		
		// print the edges
		for (Bag<T> v : td.tree) {
			for (Bag<T> w : td.tree.getNeighborhood(v)) {
				if (v.id < w.id-1) {
					sb.append("\n" + v.id + " " + w.id);
				} else if (v.id == w.id-1) {
					sb.append("\n" + v.id + " " + w.id);
				}
			}	
		}
		
		// done
		return sb.toString();
	}
	
	/**
	 * Write the string of @see treedecompositionToString to System.out.
	 * @param td the tree decomposition to be written
	 * @throws IOException
	 */
	public static <T extends Comparable<T>> void writeTreeDecomposition(TreeDecomposition<T> td) throws IOException {
		writeTreeDecomposition(td, System.out);
	}
	
	/**
	 * Write the string of @see treedecompositionToString to the given output stream.
	 * @param td the tree decomposition to be written
	 * @param stream to which the graph should be written
	 * @throws IOException
	 */
	public static <T extends Comparable<T>> void writeTreeDecomposition(TreeDecomposition<T> td, OutputStream stream) throws IOException {
		BufferedWriter out = new BufferedWriter(new OutputStreamWriter(stream));
		out.write(treedecompositionToString(td));
		out.flush();
		out.close();
	}
	
	/**
	 * Represents the graph as string the the .gr file format of PACE.
	 * In order to do so, a bijection from V to {1,...,|V|} will be computed and vertices
	 * will be represented as integers in the output.
	 * @param graph that should be serialized 
	 * @return string representing the graph
	 */
	public static <T extends Comparable<T>> String graphToValidString(Graph<T> graph) {
		// compute the bijection
		int index = 1;
		Map<T, Integer> phi = new HashMap<T, Integer>();
		for (T v : graph.getCopyOfVertices()) {
			phi.put(v, index);
			index = index + 1;
		}

		// compute the string using a string builder
		StringBuilder sb = new StringBuilder();
		sb.append("p tw " + graph.getCopyOfVertices().size() + " "
				+ graph.getNumberOfEdges() + "\n");
		for (T v : graph.getCopyOfVertices()) {
			for (T w : graph.getNeighborhood(v)) {
				if (v.compareTo(w) > 0)
					continue;
				sb.append(phi.get(v) + " " + phi.get(w) + "\n");
			}
		}

		// done
		return sb.toString();
	}
	
	/**
	 * Write the string of @see graphToValidString to System.out.
	 * @param graph
	 * @throws IOException
	 */
	public static <T extends Comparable<T>> void writeValidGraph(Graph<T> graph) throws IOException {
		writeValidGraph(graph, System.out);
	}
	
	/**
	 * Write the string of @see graphToValidString to the given output stream.
	 * @param graph
	 * @throws IOException
	 */
	public static <T extends Comparable<T>> void writeValidGraph(Graph<T> graph, OutputStream stream) throws IOException {
		BufferedWriter out = new BufferedWriter(new OutputStreamWriter(stream));
		out.write(graphToValidString(graph));
		out.flush();
		out.close();
	}
	
	/**
	 * Output the graph in TikZ syntax to embed it into LaTeX documents.
	 * Compiling this code needs an actual TikZ version, LuaLaTeX, and the TikZ graph drawing force library.
	 * @param graph that should be serialized 
	 * @return string representing the graph
	 */
	public static <T extends Comparable<T>> String graphToTikZ(Graph<T> graph) {
		StringBuilder sb = new StringBuilder();
		sb.append("\\tikz\\graph[spring electrical layout] {\n");
		for (T v : graph.getCopyOfVertices()) sb.append(v+";");
		sb.append("\n");
		for (T v : graph.getCopyOfVertices()) {
			for (T w : graph.getNeighborhood(v)) {
				if (v.compareTo(w) < 0) {
					sb.append(v + " -- " + w + ";\n");
				}
			}
		}
		sb.append("};\n");
		return sb.toString();
	}
	
	/**
	 * Write the string of @see graphToTikZ to System.out.
	 * @param graph
	 * @throws IOException
	 */
	public static <T extends Comparable<T>> void writeTikZ(Graph<T> graph) throws IOException {
		writeTikZ(graph, System.out);
	}
	
	/**
	 * Write the string of @see graphToTikZ to the given output stream.
	 * @param graph
	 * @throws IOException
	 */
	public static <T extends Comparable<T>> void writeTikZ(Graph<T> graph, OutputStream stream) throws IOException {
		BufferedWriter out = new BufferedWriter(new OutputStreamWriter(stream));
		out.write(graphToTikZ(graph));
		out.flush();
		out.close();
	}
	
	/**
	 * Output the tree-decomposition in TikZ syntax to embed it into LaTeX documents.
	 * Compiling this code needs an actual TikZ version, LuaLaTeX, and the TikZ graphdrawing binary tree library.
	 * @return
	 */
	public static <T extends Comparable<T>> String treedecompositionToTikZ(TreeDecomposition<T> td) {
		StringBuilder sb = new StringBuilder();		
		sb.append("\\tikz\\graph[binary tree layout] {\n");
		
		// print the bags
		for (Bag<T> bag : td.tree) {
			sb.append(bag.id + "/ $\\{~");
			for (T v : bag.vertices) sb.append(v + "~");
			sb.append("\\}$;\n");
		}
		
		// print the edges
		for (Bag<T> v : td.tree) {
			for (Bag<T> w : td.tree.getNeighborhood(v)) {
				if (v.id < w.id) {
					sb.append(v.id + " -- " + w.id + ";\n");
				}
			}	
		}
		
		sb.append("};\n");
		return sb.toString();
	}
	
	/**
	 * Write the string of @see treedecompositionToTikZ to System.out.
	 * @param td the tree decomposition to be written
	 * @throws IOException
	 */
	public static <T extends Comparable<T>> void writeTreeDecompositionTikZ(TreeDecomposition<T> td) throws IOException {
		writeTreeDecompositionTikZ(td, System.out);
	}
	
	/**
	 * Write the string of @see treedecompositionToTikZ to the given output stream.
	 * @param td the tree decomposition to be written
	 * @param stream the stream to which should be written
	 * @throws IOException
	 */
	public static <T extends Comparable<T>> void writeTreeDecompositionTikZ(TreeDecomposition<T> td, OutputStream stream) throws IOException {
		BufferedWriter out = new BufferedWriter(new OutputStreamWriter(stream));
		out.write(treedecompositionToTikZ(td));
		out.flush();
		out.close();
	}
	
}
