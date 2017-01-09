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
import java.util.Set;


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
		Graph<T> copy = GraphFactory.emptyGraph();
		
		// copy vertices
		for (T v : graph) copy.addVertex(v);
		
		// copy edges
		for (T v : graph) {
			for (T w : graph.getNeighborhood(v)) {
				if (v.compareTo(w) < 0) {
					copy.addEdge(v, w);
				}
			}
		} 
		
		return copy;
	}
	
}
