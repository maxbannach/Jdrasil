package jdrasil.algorithms.upperbounds;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import jdrasil.graph.Bag;
import jdrasil.graph.Graph;
import jdrasil.graph.TreeDecomposer;
import jdrasil.graph.TreeDecomposition;
import jdrasil.graph.TreeDecomposition.TreeDecompositionQuality;

public class GreedyPathDecomposer <T extends Comparable<T>> implements TreeDecomposer<T>{

	private Graph<T> graph;
	private int tries;
	
	public GreedyPathDecomposer(Graph<T> graph) {
		this.graph=graph;
		this.tries=0;
	}
	
	public GreedyPathDecomposer(Graph<T> graph,int tries) {
		this.graph=graph;
		this.tries=tries;
	}
	
	@Override
	public TreeDecomposition<T> call() throws Exception {
		TreeDecomposition<T> minTd=singleCall(null);
		int minTw=minTd.getWidth();
		
		for (int i=1;i<tries;i++) {
			TreeDecomposition<T> td=singleCall(null);
			if (td.getWidth()<minTw) {
				minTw=td.getWidth();
				minTd=td;
			}
			
		}
		// If tries==0, then start once at every vertex that is not isolated.
		if (tries==0) {
			for (T v : graph) {
			   	if (!graph.getNeighborhood(v).isEmpty()) {
					TreeDecomposition<T> td=singleCall(v);
					if (td.getWidth()<minTw) {
						minTw=td.getWidth();
						minTd=td;
					}
			   	}
			}
		}
		return minTd;
	}
	
	private TreeDecomposition<T> singleCall(T firstChoice) throws Exception {
		TreeDecomposition<T> td=new TreeDecomposition<T>(graph);
		Set<T> currentSet=new HashSet<T>();
		Bag<T> bPrev=null;

		// Vertices which are not yet chosen to a bag of the path decomposition
	    Set<T> unchosenV=graph.getVertices();

	    // deg is the number of neighbors which are not yet chosen to a bag of the path decomposition
	    Map<T,Integer> deg=new HashMap<T,Integer>();
	    for (T v : graph) {
	    	deg.put(v,graph.getNeighborhood(v).size());
	    }
	    
	    // Find vertices with degree 0.
	    Set<T> degZero=new HashSet<T>();
	    for (T v : graph) {
	    	if (deg.get(v)==0) {
	    		degZero.add(v);
	    	}
	    }
	    
	    while (!unchosenV.isEmpty()) {
	        // If currentSet is empty, choose an arbitrary vertex.
	        if (currentSet.isEmpty() && degZero.isEmpty()) {
	        	T v = firstChoice;
	        	if (firstChoice==null) {
	        		v = choice(unchosenV);
	        	}
	            addVertex(v, graph, unchosenV, currentSet, deg, degZero);
	        }
	        
	        T mindegv,mindegu;
	        if (!degZero.isEmpty()) {
	            mindegv = choice(degZero);
	        } else {
	            // Find a vertex with minimal degree in the current bag.
	            mindegu=currentSet.iterator().next();
	            for (T u : currentSet) {
	                if (deg.get(u)<=deg.get(mindegu)) {
	                    mindegu=u;
	                }
	            }
	            ArrayList<T> minDegUList=new ArrayList<T>();
	            for (T u : currentSet) {
	            	if (deg.get(u)==deg.get(mindegu)) {
	            		minDegUList.add(u);
	            	}
	            }
	            mindegu=choice(minDegUList);
	            
	            // Find a neighbor of mindegu of minimal degree.
	            Set<T> neighbors=new HashSet<T>();
	            for (T v : graph.getNeighborhood(mindegu)) {
	            	if (unchosenV.contains(v)) {
	            		neighbors.add(v);
	            	}
	            }
	            mindegv=neighbors.iterator().next();
	            for (T v : neighbors) {
	                if (deg.get(v)<deg.get(mindegv)) {
	                    mindegv=v;
	                }
	            }
	            ArrayList<T> minDegVList=new ArrayList<T>();
	            for (T v : neighbors) {
	            	if (deg.get(v)==deg.get(mindegv)) {
	            		minDegVList.add(v);
	            	}
	            }
	            mindegv=choice(minDegVList);
	        }
	        
	        // Add this neighbor and update degrees.
	        addVertex(mindegv, graph, unchosenV, currentSet, deg, degZero);
	        
	        // Find deg-0 vertices in current bag
	        ArrayList<T> degZeroInBag=new ArrayList<T>();
	        for (T v : degZero) {
	        	if (currentSet.contains(v)) {
	        		degZeroInBag.add(v);
	        	}
	        }
	        if (!degZeroInBag.isEmpty()) {
	        	// Create bag before removing vertices from it.
	        	Bag<T> b=td.createBag(new HashSet<T>(currentSet));
	      		if (bPrev!=null) {
	      			td.addTreeEdge(bPrev, b);
	      		}
	      		bPrev=b;
		        // Remove deg-0 vertices from current bag
	            for (T v : degZeroInBag) {
	            	currentSet.remove(v);
	                degZero.remove(v);
	    	    }
	        }
    		
	    }

		return td;
	}

	private T choice(Set<T> mySet) {
		int item=new Random().nextInt(mySet.size());
		int i=0;
		for (T elem : mySet) {
			if (i==item) {
				return elem;
			}
			i++;
		}
		return null;
	}

	private T choice(ArrayList<T> myList) {
		int item=new Random().nextInt(myList.size());
		return myList.get(item);
	}
	    
	private void addVertex(T v,Graph<T> graph,Set<T> unchosenV,Set<T> currentSet,Map<T,Integer> deg,Set<T> degZero) {
		currentSet.add(v);
		unchosenV.remove(v);
		for (T w : graph.getNeighborhood(v)) {
			int neigh=deg.get(w)-1;
			deg.replace(w,neigh);
			if (neigh==0) {
				degZero.add(w);
			}
		}
	}
	    
	@Override
	public TreeDecomposition<T> getCurrentSolution() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public TreeDecompositionQuality decompositionQuality() {
		// TODO Auto-generated method stub
		return null;
	}

}
