package glucp;

/**
 * This is our wrapper class to communicate with the parallel Glucose SAT-solver of Laurent Simon and  Gilles Audemard.
 * @author Sebastian Berndt <berndt@tcs.uni-luebeck.de>
 */
import java.util.List;
import java.util.ArrayList;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Callable;


import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.Arrays;

/** 
 * This class gives Java access to the Glucose functions
 * @author berndt
 *
 */
public class JPGlucose {
	
	// load the library
    static {
        System.loadLibrary("JPGlucose");

    }


    public static void main(String[] args) {   
    }

    /**
     * The pointer to the glucose object
     */
    protected long handle;
    
    /**
     * An indicator whether the formula was solvable
     */
    protected boolean solvable;
    
    /**
     * The possible timeout
     */
    protected int t;
    
    /**
     * Initialize a new JPGlucose object and initialize the C++ parallel glucose object
     */
    public JPGlucose() {
  
    	handle = ginit();
    	t = -1;
    	solvable = true;

        if (handle == 0)
            throw new OutOfMemoryError();

    }

    /**
     * Set the timeout
 	* @param t the new timeout
 	*/	
    public void setTimeOut(int t){
        this.t = t;
    }


    /**
     * Add a new clause to the formula
     * @param xs a list representing the clause
     * @return true if the clause could be added to the formula
     */
    public boolean addClause(List<Integer> xs){
    	int n = xs.size();
    	int[] arr = new int [n];
    	int i = 0;
    	for(int x: xs){
    		arr[i] = x;
    		i++;
    	}
        
        boolean b =gadd(handle,arr);
        if(!b) {
            solvable = false;
        }
        return b;

    }
                                         
	
    /**
	 * Solve the formula
	 * @return true if the formula is satisfiable (and if it was solved within the given time)
	 */
    public boolean solve() {

        if(solvable){
            if(t > 0){
                solvable = gsat_time(handle, t);

            }
            else{
                solvable = gsat(handle);
           }
        }
       return solvable;
    }

    /**
	 * Return the value of the literal in the satisfying assignment
	 * @param literal the literal to check
	 * @return 1 if the literal is true and 0 else (the right way)
	 */
    public int getValue(int literal) {
        if(solvable){
            return 1-gderef(handle, literal);
        }
        else{
            return -1;
        }
    }


    // The interfaces to the native glucp_JPGlucose.cpp
    protected static native long ginit();
    protected static native boolean gadd(long handle, int[] lit);
    protected static native boolean gsat(long handle);
    protected static native boolean gsat_time(long handle, int t);
    protected static native int gderef(long handle, int lit);
    
}
