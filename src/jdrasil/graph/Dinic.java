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
import java.util.LinkedList;

/**
 * A simple Implementation of the Dinic algorithm to compute a max flow.
 * @author Sebatian Berndt
 *
 */
public class Dinic {
	
		/**
		 * The graph as adjacency matrix
		 */
	   int[][] g;
	   
	   /**
	    * The current flow
	    */
	   Flow f;	


	   /**
	    * Create a new Dinic instance
	    * @param g the graph
	    * @param s the source node
	    * @param t the terminal node
	    */
	    public Dinic(int[][] g, int s, int t){
	        int[][] act = new int[g.length][g.length];
	        // create the zero flow
	        f = new Flow(act,s,t);
	        this.g = g;
	    }
	    
	    /**
	     * improve the flow as long as possible
	     */
	    public boolean[][] start(){
	        int[][] ncf = improve();
	        while (ncf != null){
	            augment(f,ncf);
	            ncf=improve();
	        }
	        return toSet(f);
	    }

	    /**
	     * Converts a Flow into a boolean matrix that represents which edges have flow
	     * @param f the flow
	     * @return the boolean matrix representation of f
	     */
	    public boolean[][] toSet(Flow f){
	        boolean[][] s = new boolean[g.length][g.length];
	        for(int i = 0; i < g.length; i++){
	            for(int j = 0; j < g.length; j++){
	                s[i][j]= (f.act[i][j] == g[i][j] && g[i][j] > 0 && g[i][j] < Integer.MAX_VALUE);
	            }
	        }
	    return s;
	    }
	    
	  
	    /**
	     * set the flow to the new values given by the new residual capacities
	     * @param f the current flow
	     * @param cf the new capacities
	     */
	    public void augment(Flow f, int[][] cf){
	        for (int i= 0; i < g.length; i++){
	            for (int j = 0; j < g.length; j++){
	                f.act[i][j]=g[i][j]-cf[i][j];
	            }
	        }
	    }

	    /**
	     * improve the flow via dinic
	     * 
	     * @return the new capacities of the residual graph
	     */
	    public int[][] improve(){
	        //compute residual capacity
	        int[][] cf = new int[g.length][g.length];
	        for (int i = 0; i < g.length; i++){
	            for (int j = 0; j < g.length; j++){
	                cf[i][j]=g[i][j]-f.act[i][j];
	            }
	        }
	        
	        // compute layers
	        int[]distances = revDijkstra(f.t,cf);

	        // if s ist not reachable, we got a maximum flow
	        if (distances[f.s] == Integer.MAX_VALUE){
	            return null;
	        }
	        else{
	            // improve via dfs
	            DFS dfs = new DFS(g,cf,distances);
	            dfs.start(f.s,f.t);
	            return cf;
	        }

	    }
	    
	    /**
	     * Reversed Dijkstra Algorithm
	     * 
	     * @param t the terminal node
	     * @param cf the capacities
	     * @return an array of minimum distances to t 
	     */
	    public int[] revDijkstra(int t, int[][] cf){
	        // initialize distances
	        int[] distances = new int[g.length];
	        for (int i = 0; i < g.length; i++){
	            distances[i]=Integer.MAX_VALUE;
	        }
	        distances[t]=0;

	        // initialize queue
	        LinkedList<Integer> queue =  new LinkedList<Integer>();
	        queue.push(t);
	        while(!queue.isEmpty()){
	            int u = queue.pop();
	            for (int i = 0; i < g.length; i++){
	                // use only edges which are useful
	                if (g[i][u] > 0 && cf[i][u] > 0){
	                    // update distances
	                    int d = distances[u]+1;
	                    if (d < distances[i]){
	                        distances[i]=d;
	                        queue.push(i);
	                    }
	                }
	            }
	        }
	        return distances;
	    }




	    public static void main(String[] args){
	        
	    }

	}

/**
 * A flow representation
 * @author berndt
 *
 */
	 class Flow{

		/**
		 * The current flow 
		 */
	     int[][] act;
	    
	    /**
	     * The source node
	     */
	    public int s;
	    
	    /**
	     * The terminal node
	     */
	    public int t;

	    /**
	     * Initialize the flow
	     * @param act the current flow
	     * @param s the source node
	     * @param t the terminal node
	     */
	    public Flow(int[][] act,int s, int t){
	        this.act = act;
	        this.s = s;
	        this.t = t;
	    }
	    

	    /**
	     * Compute the value of the flow
	     * @return the value of the flow
	     */
	    public int val(){
	        int s = 0 ;
	        for (int i = 0; i < act.length; i++){
	            s = s + act[i][t];
	        }
	        return s;
	    }
	}

	 /**
	  * A representation of a dfs run
	  * @author sber
	  *
	  */
	class DFS{
		/*
		 * The graph to work on
		 */
	    int[][] g;
	    
	    // The residual capacity
	    int[][] cf;

	    // the distances / layers
	    int[] distances;

	    // the actual path
	    int[] prev;

	    // the end node    
	    int end;

	    

	   /**
	    * Initialize the DFS object
	    * @param g the graph
	    * @param cf the current capacities
	    * @param distances the minimum distances
	    */
	    public DFS(int[][] g,int[][] cf, int[] distances){
	        this.g = g;
	        this.cf = cf;
	        this.distances = distances;
	        prev = new int[g.length];
	        for (int i = 0; i< g.length; i++){
	            prev[i] = -1;
	        }
	    }
	   
	    /**
	     * Perform a DFS starting with s and ending with t
	     * 
	     * @param s the start node
	     * @param t the end node
	     * @return the dfs computation
	     */
	    public int[][] start(int s, int t){
	        this.end = t;
	        return dfs(s);
	    }

	    /**
	     * Perform an DFS-Step
	     * 
	     * @param x the current node
	     * @return the dfs computation
	     */
	    public int[][] dfs(int x){
	        // we found the end node. Lets improve the path!
	        if (x == end){
	            int delta = delta(prev);
	            int act = x;
	            int pre = prev[x];
	            // follow the path and improve it
	            while(pre > -1){
	                cf[pre][act] = cf[pre][act] - delta;
	                cf[act][pre] = cf[act][pre] + delta;

	                // we saturated an edge
	                if(cf[pre][act] == 0){
	                    x = pre;
	                }
	                act = pre;
	                pre = prev[pre];
	            }
	        }
	        // perform the dfs step
	        for (int i = 0; i < g.length; i++){
	            if (g[x][i] > 0 && cf[x][i] != 0 && distances[x]==distances[i]+1){
	                prev[i]=x;
	                dfs(i);
	            }
	        }
	        return cf;
	    }
	    
	    /**
	     * Compute delta of the path
	     * 
	     * @param path the path
	     * @return the delta of the path
	     */
	    public int delta(int[] path){
	        int act = end;
	        int prev = path[act];

	        int delta = Integer.MAX_VALUE;
	        // find the edge with the smalles capacity
	        while(prev > -1){
	            int upd = cf[prev][act];
	            if (delta > upd){
	                delta = upd;
	            }
	            act = prev;
	            prev = path[prev];
	        }
	        return delta;
	    }
	        
	}
	




	

