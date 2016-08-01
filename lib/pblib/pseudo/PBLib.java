package pseudo;
/**
 * This is our wrapper class to communicate with the PBLib-Library of Peter Steinke.
 * @author Sebastian Berndt <berndt@tcs.uni-luebeck.de>
 */

import java.util.List;
import java.util.ArrayList;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.util.Arrays;


/** 
 * This class gives Java access to the PBLib functions
 * @author berndt
 *
 */
public class PBLib{
	
    // load the library
    static {
        System.loadLibrary("PBLib");
    }

    // a pointer to the corresponding C++ object
    public long handle;

    public static void main(String[] args) {
    }
    
    /**
     * Initialize the object by calling the C++ init method
     */
    public PBLib(){
        handle = init();
    }

    /**
     * Convert a List of integers into an array
     * @param variables the list to be converted
     * @return an array representation of the list
     */
    public int[] convert(List<Integer> variables){
        int n = variables.size();
        int[] arr = new int [n];
        int i = 0;
        for(int x: variables){
        	arr[i] = x;
        	i++;
        }
       
        return arr;
    }

    /**
     * Initialize an incremental atMostK constraint
     * @param variables the list of variables
     * @param k the upper bound
     * @param m the maximal index of an used variable
     * @return a formula containing the atMostK constraint
     */
    public  ArrayList<ArrayList<Integer>> initIterAtMostK(List<Integer> variables, int k, int m){
        int n = variables.size();
        return initIterAtMostK(convert(variables),n,k,m+1, handle);
        
    }

    /**
     * Decrease the upper bound in an initialized atMostK constraint
     * @param k the new upper bound
     * @param m the maximal index of an used variable
     * @return a formula containing the new clauses
     */
    public  ArrayList<ArrayList<Integer>> iterAtMostK(int k, int m){
        return citerAtMostK(k,m+1,handle);
    }
    
    /**
     * Initialize an incremental atLeastK constraint
     * @param variables the list of variables
     * @param k the lower bound
     * @param m the maximal index of an used variable
     * @return a formula containing the atMostK constraint
     */
    public  ArrayList<ArrayList<Integer>> initIterAtLeastK(List<Integer> variables, int k, int m){
        int n = variables.size();
        return initIterAtLeastK(convert(variables),n,k,m+1,handle);
    }


    /**
     * Increase the lower bound in an initialized atLeastK constraint
     * @param k the new lower bound
     * @param m the maximal index of an used variable
     * @return a formula containing the new clauses
     */
    public  ArrayList<ArrayList<Integer>> iterAtLeastK(int k, int m){
        return citerAtLeastK(k,m+1,handle);
    }

    /**
     * Compute an non-incremental atMostK constraint
     * @param variables the list of variables
     * @param k the upper bound
     * @param m the maximal index of an used variable
     * @return a formula containing the atMostK constraint
     */
    public ArrayList<ArrayList<Integer>> atMostK(List<Integer> variables, int k, int m){
        int n = variables.size();
        return generateAtMostK(convert(variables),n,k,m+1);
    }

    /**
     * Compute an non-incremental atLeastK constraint
     * @param variables the list of variables
     * @param k the lower bound
     * @param m the maximal index of an used variable
     * @return a formula containing the atLeastK constraint
     */
    public ArrayList<ArrayList<Integer>> atLeastK(List<Integer> variables, int k, int m){
        int n = variables.size();
        return generateAtLeastK(convert(variables),n,k,m+1);
    }

// The interfaces to the native pseudo_PBLib.cpp
    protected static native ArrayList<ArrayList<Integer>> generateAtMostK(int[] arr, int n, int k, int m);
    protected static native  ArrayList<ArrayList<Integer>> generateAtLeastK(int[] arr, int n, int k, int m);

    protected static native  ArrayList<ArrayList<Integer>> initIterAtMostK(int[] arr, int n, int k, int m, long h);
    protected static native  ArrayList<ArrayList<Integer>> initIterAtLeastK(int[] arr, int n, int k, int m, long h);

    protected static native  ArrayList<ArrayList<Integer>> citerAtMostK( int k, int m, long h);
    protected static native  ArrayList<ArrayList<Integer>> citerAtLeastK(int k, int m, long h);


    protected static native long init();


}
