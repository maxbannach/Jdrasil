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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream.GetField;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import jdrasil.utilities.JdrasilProperties;
import jdrasil.utilities.logging.JdrasilLogger;


/**
 * The GraphFactory provides methods to construct new graph objects:
 *  - the empty graph
 *  - a graph stored in a .gr file
 *  - or from other data (as logical formulas) 
 *  
 *  All methods of this class should be invoked in a static way.
 *  
 *  All methods of this class that produce graphs from .gr files are compatible to .dgf files as well.
 *  
 *  @author Max Bannach
 */
public class GraphFactory {

	/** Jdrasils Logger */
    private final static Logger LOG = Logger.getLogger(JdrasilLogger.getName());
	// hide the constructor
	private GraphFactory() {}
	
	/**
	 * Constructs an empty graph with vertices of type T.
	 * 
	 * @param <T> the type of the vertices
	 * @return the empty graph
	 */
	public static <T extends Comparable<T>> Graph<T> emptyGraph() {
		return new Graph<T>();
	}
	
	/**
	 * Construct a graph from the content of a .gr file given 
	 * as BufferedReader.
	 * 
	 * This method can also be used to parse .dgf files.
	 * 
	 * @param in - the BufferedReader storing the graph
	 * @return A graph object with the graph (vertices are integer)
	 * @throws IOException if the file was not found or is not correct encoded
	 */
	public static Graph<Integer> graphFromBufferedReaderGR(BufferedReader in) throws IOException {
		
		// create a "fresh" graph
		Graph<Integer> G = new Graph<>();
		G.setLogEdgesInNeighbourhood(false);
		// read the graph
		String line = "";
		String[] ll;
		while ( (line = in.readLine()) != null) {
			if (line.length() == 0) { continue; } // just catch empty lines
			ll = line.split(" ");
			if (ll[0].equals("c")) { continue; } // we do not handle comments
			if (ll[0].equals("p")) { 
				int n = Integer.parseInt(ll[2]);
				
				// create vertices {1,...n}, this is required as .dimacs can contain isolated vertices
				for (int v = 1; v <= n; v++) {
					G.addVertex(v);					
				}		
				
				continue;
			} 
			
			// we skip information from .dgf files to be compatible to them
			if (ll[0].equals("n")) { continue; } 
			if (ll[0].equals("d")) { continue; }
			if (ll[0].equals("v")) { continue; }
			if (ll[0].equals("x")) { continue; }
			if (ll[0].equals("b")) { continue; }
			if (ll[0].equals("l")) { continue; }
			
			// parse an edge
			int u = 0;
			int v = 0;
			if (ll[0].equals("e")) { // .dgf edge
				u = Integer.parseInt(ll[1]);
				v = Integer.parseInt(ll[2]);				
			} else { // .gr edge
				u = Integer.parseInt(ll[0]);
				v = Integer.parseInt(ll[1]);	
			}
			G.addEdge(u, v);
		}
		
		// done
		getFillValues(G);
		G.setLogEdgesInNeighbourhood(true);		// Set it to true - if it's not needed, it can be disabled lateron
		return G;
	}

	/**
	 * Construct a graph from a .gr file (as defined by PACE).
	 * 
	 * This method can also be used to parse .dgf files.
	 * 
	 * @param grFile - the file storing the graph
	 * @return A graph object with the graph (vertices are integer)
	 * @throws IOException if the file was not found or is not correct encoded
	 */
	public static Graph<Integer> graphFromGr(File grFile) throws IOException {
		
		// read the graph
		BufferedReader in = new BufferedReader(new FileReader(grFile));
		Graph<Integer> G = graphFromBufferedReaderGR(in);
		in.close();
		
		// done
		return G;
	}
	
	/**
	 * Construct a graph from the content of a .gr file read from stdin
	 * 
	 * This method can also be used to parse .dgf files.
	 * 
	 * @return A graph object with the graph (vertices are integer)
	 * @throws IOException if the file was not found or is not correct encoded
	 */
	public static Graph<Integer> graphFromStdin() throws IOException {
		
		// read the graph
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		Graph<Integer> G = graphFromBufferedReaderGR(in);
		in.close();
		
		// done
		return G;
	}
	
	/**
	 * Given a graph and a subgraph of it (in form of a set of vertices), this method computes a new graph object
	 * representing the subgraph. The vertices in this graph are the same objects as in the original graph.
	 * @param graph
	 * @param subgraph
	 * @return
	 */
	public static <T extends Comparable<T>> Graph<T> graphFromSubgraph(Graph<T> graph, Set<T> subgraph) {
		
		// create a fresh graph
		Graph<T> G = new Graph<>();
		
		// add vertices of the component to G
		for (T v : subgraph) {
			G.addVertex(v);
		}
		
		// add edges of the component to G
		for (T v : subgraph) {
			for (T w: subgraph) {
				if (v.compareTo(w) < 0 && graph.isAdjacent(v, w)) {
					G.addEdge(v, w);
				}
			}
		}
		
		// done
		return G;
	}
	
	/**
	 * Construct a copy of the given graph.
	 * @param graph that should be copied
	 * @return graph that has exactly the same edge relation as the given graph
	 */
	public static <T extends Comparable<T>> Graph<T> copy(Graph<T> graph) {
		return new Graph<T>(graph);
//		Graph<T> copy = GraphFactory.emptyGraph();
//
//		// copy vertices
//		for (T v : graph) copy.addVertex(v);
//
//		// copy edges
//		for (T v : graph) {
//			for (T w : graph.getNeighborhood(v)) {
//				if (v.compareTo(w) < 0) {
//					copy.addEdge(v, w);
//				}
//			}
//		}
//
//		return copy;
	}
	
	/**
	 * Compute the size of the intersection of two arraylists of Integers. This assumes that they are sorted.
	 */
	private static int getSizeOfIntersection(ArrayList<Integer> a1, ArrayList<Integer> a2, int[] countAt){
		int ret = 0;
		int i1=0;
		int i2=0;
		while(i1 < a1.size() && i2 < a2.size()){
			if(a1.get(i1).equals(a2.get(i2))){
				countAt[a1.get(i1)]++;
				i1++;
				i2++;
			}
			else if(a1.get(i1).compareTo(a2.get(i2)) < 0)
				i1++;
			else
				i2++;
		}
		return ret;
	}
	/**
	 * Compute the intersection of two arrays of ints. Used for updating the number of edges in the neighbourhood of the graph. 
	 * @param a1
	 * @param a2
	 * @param countAt
	 * @return
	 */
	private static int getSizeOfIntersection(int[] a1, int[] a2, int[] countAt){
		int ret = 0;
		int i1=0;
		int i2=0;
		while(i1 < a1.length && i2 < a2.length){
			if(a1[i1] == a2[i2]){
				countAt[a1[i1]]++;
				i1++;
				i2++;
			}
			else if(a1[i1] < a2[i2]) 
				i1++;
			else
				i2++;
		}
		return ret;
	}
	
	private static void getFillValues(Graph<Integer> graph){
		// Get the adjacency list as integers
		ArrayList<int[]> myAdArrays = new ArrayList<>();
//		ArrayList<ArrayList<Integer>> myAdLists = new ArrayList<>();
		for(int i = 0 ; i <= graph.getNumVertices()+1 ; i++){
			myAdArrays.add(null);
//			myAdLists.add(new ArrayList<>());
		}
		long t = System.currentTimeMillis();
		for(int node : graph.getCopyOfVertices()){
			Set<Integer> out_edges = graph.getNeighborhood(node);
			myAdArrays.set(node, new int[out_edges.size()]);
			int index = 0;
			for(int out : out_edges)
				myAdArrays.get(node)[index++]=out;
//			myAdLists.get(node).addAll(out_edges);  
			
		}
//		int[] count1 = new int[graph.getNumVertices()+1];
		int[] count2 = new int[graph.getNumVertices()+1];
		
		
		LOG.info("Time for creating the arrays: " + (System.currentTimeMillis()-t));
		t = System.currentTimeMillis();
		for(int[] e : myAdArrays)
			if(e != null)
				Arrays.sort(e);
//		for(ArrayList<Integer> l : myAdLists)
//			Collections.sort(l);
		LOG.info("Time for sorting: " + (System.currentTimeMillis()-t));
		t = System.currentTimeMillis();
		Set<Integer> nodes = graph.getCopyOfVertices();
//		for(Integer i : nodes){
//			for(Integer v : graph.getNeighborhood(i)){
//				if(v.compareTo(i) < 0){
//					getSizeOfIntersection(myAdLists.get(i), myAdLists.get(v), count1);
//				}
//			}
//		}
//		LOG.info("Time for the array-list-version: " + (System.currentTimeMillis()-t));
		t = System.currentTimeMillis();
		for(Integer i : nodes){
			for(Integer v : graph.getNeighborhood(i)){
				if(v.compareTo(i) < 0){
					getSizeOfIntersection(myAdArrays.get(i), myAdArrays.get(v), count2);
				}
			}
		}
		LOG.info("Time for the version on arrays: " + (System.currentTimeMillis()-t));
		
//		for(int i = 0 ; i < count1.length ; i++){
//			if(count1[i] != count2[i]){
//				throw new RuntimeException("i=" + i +  ",count-values were different???"  + count1[i] + " - " + count2[i]);
//			}
//		}
//		LOG.info("Results were equal! ");
		for(Integer v : nodes)
			graph.setNumEdgesInNeighbourhood(v, count2[v]);
		if(JdrasilProperties.containsKey("debug")){
			t = System.currentTimeMillis();
			Integer toCheck =  graph.checkFillValues();
			if(toCheck != null){
				LOG.info("Checking " + toCheck + " with fill-value " + count2[toCheck]);
				for(Integer u : graph.getNeighborhood(toCheck)){
					StringBuilder sb = new StringBuilder("Neighbour " + u + " with adjacencies ");
					int[] n = myAdArrays.get(u);
					for(int i : n)
						sb.append(i + " ");
					LOG.info(sb.toString());
				}
			}
			LOG.info("Checking the fill-values took time " + (System.currentTimeMillis()-t));
		}
	}
	
}
