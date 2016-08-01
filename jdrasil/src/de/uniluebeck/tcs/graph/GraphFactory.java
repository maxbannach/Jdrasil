package de.uniluebeck.tcs.graph;

/**
 * GraphFactory.java
 * @author bannach
 */

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
 *  @param <T> The type of vertices of graphs is generic.
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
	
}
