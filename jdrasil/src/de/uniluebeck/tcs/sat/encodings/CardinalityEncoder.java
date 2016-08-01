package de.uniluebeck.tcs.sat.encodings;

/**
 * CardinalityEncoder.java
 * @author bannach
 */

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import pseudo.PBLib;
import de.uniluebeck.tcs.sat.Formula;

/**
 * This class provides methods to encode cardinality constraints into a formula of propositional logic.
 * Cardinality constraints are constraints of the form At-Most-k (AMK) and At-Least-k (ALK), which force that a certain
 * number of variables in the formula is set to true. Such encodings are commonly used and, therefore, it is important to
 * choose efficient versions of the encoding.
 * 
 * The paper "Sat Encodings of the At-Most-k Constraint" from Alan M. Frisch and Paul A. Giannaros provides a nice
 * overview of possible encodings. This class orients itself on this paper.
 * 
 */
public class CardinalityEncoder {

	/** This class is a singleton. */
	private static CardinalityEncoder instance;
	
	/** Hide the constructor. */
	private CardinalityEncoder() {
		instance = this;
	}
	
	/**
	 * This class is implemented as singleton.
	 * Use this method to get the only allocated object of this class.
	 * @return
	 */
	public static CardinalityEncoder getInstance() {
		if (instance == null) {
			instance = new CardinalityEncoder();
		}
		return instance;
	}
		
	//MARK: Binomial Encoding
	
	/** @see binomialAMK(Formula phi, List<Integer> variables, int k) */
	private void binomialAMK(Formula phi, List<Integer> variables, List<Integer> C, int k, int pos) {
		
		// construction of subset complete
		if (k == 0) {
			phi.addClause(new ArrayList<>(C));
			return;
		}
		
		// end of recursion, not enough variables to complete subset
		if (pos == variables.size()) return;
		
		// for each remaining variable, we may add it or do not add it
		for (int i = pos; i < variables.size(); i++) {
			C.add(-1 * variables.get(i));
			binomialAMK(phi, variables, C, k-1, i+1);
			C.remove(C.size()-1);
		}
	}
	
	/**
	 * Add an At-Most-k cardinality constraint for the given set of variables to the formula.
	 * This method encodes the constraint using the binomial encoding, which uses n^k new clauses, but
	 * does not introduce new variables.
	 * @param phi
	 * @param variables
	 * @param k
	 */
	public void binomialAMK(Formula phi, Set<Integer> variables, int k) {
		binomialAMK(phi, new ArrayList<>(variables), new LinkedList<>(), k+1, 0);
	}
	
	/**
	 * Add an At-Least-k cardinality constraint for the given set of variables to the formula.
	 * This method encodes the constraint using the binomial At-Most-(n-k) constraint.
	 * @param phi
	 * @param variables
	 * @param k
	 */
	public void binomialALK(Formula phi, Set<Integer> variables, int k) {
		List<Integer> neg = new ArrayList<>(variables);
		for (int i = 0; i < neg.size(); i++) neg.set(i, -1*neg.get(i));
		binomialAMK(phi, neg, new LinkedList<>(), variables.size()-k+1, 0);
	}

	//MARK: Binary Encoding
	
	/**
	 * Add an At-Most-k cardinality constraint to the given formula using the binary encoding.
	 * This encoding introduce O(kn log n) clauses and O(kn) new variables.
	 * @param phi
	 * @param variables
	 * @param k
	 */
	public void binaryAMK(Formula phi, Set<Integer> variables, int k) {
		List<Integer> vars = new ArrayList<>(variables);
		int n = variables.size();
		int logn = (int) Math.ceil(Math.log(n)/Math.log(2));
		int currentVar = phi.getHighestVariable()+1;
		
		// variables for the binary strings
		int[][] B = new int[k+1][logn+1];
		for (int i = 1; i <= k; i++) {
			for (int j = 1; j <= logn; j++) {
				B[i][j] = currentVar;
				currentVar = currentVar + 1;
			}
		}
		
		// variables to encode the formula as CNF
		int[][] T = new int[k+1][n+1];
		for (int g = 1; g <= k; g++) {
			for (int i = 1; i <= n; i++) {
				T[g][i] = currentVar;
				currentVar = currentVar + 1;
			}
		}
		
		// actually add the encoding
		for (int i = 1; i <= n; i++) {
			
			// left clause
			List<Integer> C = new ArrayList<>();
			C.add(-1 * vars.get(i-1));
			for (int g = Math.max(1, k-n+i); g <= Math.min(i,k); g++) {
				C.add(T[g][i]);
			}
			phi.addClause(C);
			
			// right clause
			for (int g = Math.max(1, k-n+i); g <= Math.min(i,k); g++) {
				for (int j = 1; j <= logn; j++) {
					if (((i >> (j-1)) & 1) == 1) {
						phi.addClause(-1*T[g][i], B[g][j]);
					} else {
						phi.addClause(-1*T[g][i], -1*B[g][j]);
					}
				}
			}
		}
		
		// finally, mark auxiliary variables as such
		for (int i = 1; i <= k; i++) {
			for (int j = 1; j <= logn; j++) {
				phi.markAuxiliary(B[i][j]);
			}
		}
		for (int g = 1; g <= k; g++) {
			for (int i = 1; i <= n; i++) {
				phi.markAuxiliary(T[g][i]);
			}
		}
	}
	
	/**
	 * Add an At-Least-k cardinality constraint for the given set of variables to the formula.
	 * This method encodes the constraint using the binary At-Most-(n-k) constraint.
	 * @param phi
	 * @param variables
	 * @param k
	 */
	public void binaryALK(Formula phi, Set<Integer> variables, int k) {
		Set<Integer> neg = new HashSet<>();
		for (Integer v : variables) neg.add(-1*v); 
		binaryAMK(phi, neg, variables.size()-k);
	}
	
	//MARK: Sequential Counter Encoding
	
	/**
	 * Add an At-Most-k cardinality constraint to the given formula using the sequential counter encoding.
	 * This encoding introduce O(kn) clauses and O(kn) new variables.
	 * @param phi
	 * @param variables
	 * @param k
	 */
	public void sequentialAMK(Formula phi, Set<Integer> variables, int k) {
		
		// small instances are better handled by binomial
		if (variables.size() < 7 || k <= 1) {
			binomialAMK(phi, variables, k);
			return;
		}
		
		List<Integer> X = new ArrayList<>(variables);
		X.add(0,0);				
		int n = variables.size();
		int currentVar = phi.getHighestVariable() + 1;
		
		// new variables encoding the register (i.e., the counting circuit)
		int[][] R = new int[n+1][k+1];
		for (int i = 1; i <= n; i++) {
			for (int j = 1; j <= k; j++) {
				R[i][j] = currentVar;
				currentVar = currentVar + 1;
			}
		}
		
		// equation 1
		for (int i = 1; i <= n-1; i++) {
			phi.addClause(-1*X.get(i), R[i][1]);
		}
		
		// equation 2
		for (int j = 2; j <= k; j++) {
			phi.addClause(-1*R[1][j]);
		}
		
		// equation 3
		for (int i = 2; i <= n-1; i++) {
			for (int j = 1; j <= k; j++) {
				phi.addClause(-1*R[i-1][j], R[i][j]);
			}
		}
		
		// equation 4
		for (int i = 2; i <= n-1; i++) {
			for (int j = 2; j <= k; j++) {
				phi.addClause(-1*X.get(i), -1*R[i-1][j-1], R[i][j]);
			}
		}
		
		// equation 5
		for (int i = 2; i <= n; i++) {
			phi.addClause(-1*X.get(i), -1*R[i-1][k]);
		}
		
		// finally, mark auxiliary variables as such
		for (int i = 1; i <= n; i++) {
			for (int j = 1; j <= k; j++) {
				phi.markAuxiliary(R[i][j]);
			}
		}
	}
	
	/**
	 * Add an At-Least-k cardinality constraint for the given set of variables to the formula.
	 * This method encodes the constraint using the sequential counter At-Most-(n-k) constraint.
	 * @param phi
	 * @param variables
	 * @param k
	 */
	public void sequentialALK(Formula phi, Set<Integer> variables, int k) {
		Set<Integer> neg = new HashSet<>();
		for (Integer v : variables) neg.add(-1*v); 
		sequentialAMK(phi, neg, variables.size()-k);
	}
	
	//MARK: Commander Encoding
	
	/**
	 * Add an At-Most-k cardinality constraint to the given formula using the commander encoding.
	 * This encoding introduce O((binom{2k+2}{k+1}+binom{2k+2}{k-1})*n/2) clauses and O(kn/2) new variables.
	 * @param phi
	 * @param variables
	 * @param k
	 */
	public void commanderAMK(Formula phi, Set<Integer> variables, int k) {
		int n = variables.size();
		int s = k + 2; // group size
		int g = (int) Math.ceil(n/s); // number of groups
		int currentVar = phi.getHighestVariable() + 1;
		
		// for very small instances, binomial is simply the best
		if (n < 7) {
			binomialAMK(phi, variables, k);
			return;
		}
		
		// end of recursion, use other encoding
		if (n <= k+s) {
			sequentialAMK(phi, variables, k);
			return;
		}
		
		// the groups
		List<List<Integer>> G = new ArrayList<>(g);
		
		// the commander variables
		int[][] C = new int[g][k];
		Set<Integer> commander = new HashSet<>();
		for (int i = 0; i < g; i++) {
			for (int j = 0; j < k; j++) {
				C[i][j] = currentVar;
				phi.markAuxiliary(C[i][j]);
				commander.add(currentVar);
				currentVar = currentVar + 1;
			}
		}
		phi.setHighestVariable(currentVar-1);
		
		// partition variables into the groups
		int i = 0;
		for (Integer v : new ArrayList<>(variables)) {			
			if (G.size() < i+1) G.add(new ArrayList<>(s+k));
			G.get(i).add(v);
			if (G.get(i).size() == s) {
				// group full -> add commander variables
				for (int j = 0; j < k; j++) {
					G.get(i).add(-1*C[i][j]);
				}
				
				// AMK and ALK 
				if ( (s+k) < 7) { // small instances are directly handled by binomial
					binomialAMK(phi, new HashSet<>(G.get(i)), k);
					binomialALK(phi, new HashSet<>(G.get(i)), k);
				} else { // otherwise we need a stronger encoding
					sequentialAMK(phi, new HashSet<>(G.get(i)), k);
					sequentialALK(phi, new HashSet<>(G.get(i)), k);
				}
				
				// remove symmetrical solutions when less than k variables are true
				for (int j = 0; j < k-1; j++) {
					phi.addClause(-1*C[i][j], C[i][j+1]);
				}
				
				// start new group
				i = i + 1;
			}
		}
		
		// recursive call on commander variables
		commanderAMK(phi, commander, k);
	}
	
	//MARK: PBlib
	
    /**
	 * Add an At-Most-k cardinality constraint for the given set of
	 * variables to the formula.  This method encodes the constraint
	 * using the C++ library PBLib of Peter Steinke.
	 * @param phi
	 * @param variables
	 * @param k
	 */
    public void pbAMK(Formula phi, Set<Integer> variables, int k){
        PBLib p = new PBLib();
        ArrayList<ArrayList<Integer>> clauses = p.atMostK(new ArrayList<>(variables), k, phi.getHighestVariable());
        for(ArrayList<Integer> c: clauses){
            phi.addClause(c);
        }
    }
    
    /**
	 * Add an At-Least-k cardinality constraint for the given set of
	 * variables to the formula.  This method encodes the constraint
	 * using the C++ library PBLib of Peter Steinke.
	 * @param phi
	 * @param variables
	 * @param k
	 */
    public void pbALK(Formula phi, Set<Integer> variables, int k){
        PBLib p = new PBLib();
        ArrayList<ArrayList<Integer>> clauses = p.atLeastK(new ArrayList<>(variables), k, phi.getHighestVariable());
        for(ArrayList<Integer> c: clauses) {
            phi.addClause(c);
        }
    }
    
    //MARK: BoolVar/PB
    
    
}
