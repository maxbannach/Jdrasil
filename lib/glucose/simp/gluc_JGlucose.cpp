/**
 * This is the wrapper class that allows Glucose (non-parallel) to
 * communicate with Java by giving an appropriate API
 * author: berndt
 */

#include "gluc_JGlucose.h"
#include "SimpSolver.h"
#include <stdint.h>
#include <iostream>
#include <string> 
#include <signal.h>
#include <unistd.h>

// did we terminate early?
bool terminated = false;


// initialize the solver and deactivate the output
jlong JNICALL Java_gluc_JGlucose_ginit(JNIEnv *env, jclass obj){
  jlong j = reinterpret_cast<jlong>(new Glucose::SimpSolver());
  Glucose::SimpSolver *S = reinterpret_cast<Glucose::SimpSolver *>(j);
  S->verbosity = -1;
  S->showModel = 1;
  return  j;
}

/**
 * Add a clause to the instance. 
 *
 * @param handle The pointer to the instance
 * @param xs the new clause
 * @return whether the clause was added successfully
 */
jboolean JNICALL Java_gluc_JGlucose_gadd(JNIEnv *env, jclass obj, jlong handle,  jintArray xs){
  // get the glucose object and the C++ array
  Glucose::SimpSolver *S = reinterpret_cast<Glucose::SimpSolver *>(handle);
  jsize n = env->GetArrayLength(xs);
  jint *body = env->GetIntArrayElements(xs, NULL);

  // construct the vector representing the clause
  Glucose::vec<Glucose::Lit> vs;
  for(int i = 0 ; i < n; i++){
    int a = body[i];
    int varl = abs(a)-1;
    Glucose::Lit l;

    // use the right constructor
    if(a > 0){
      l = Glucose::mkLit(varl);
    }
    else{
      l = ~Glucose::mkLit(varl);
    }

    // increase the number of variables by allocating new memory
    while(varl >= S->nVars()){
      S->newVar();
    }

    // freeze all the variables. We may need them later on
    S->setFrozen(var(l),true);
    
    vs.push(l);

  }
  
  bool res = S->addClause(vs);

  return res;
}

/**
 * Set the terminated flag to true, if a timeout occurs.
 */
static void catchalrm (int sig) {
  terminated = true;
}


/**
 * Solve the formula with a timeout
 * @deprecated
 * @param handle the glucose object
 * @param t the allowed time
 * @return true, if the formula is satisfiable within time t
 */
jboolean JNICALL Java_gluc_JGlucose_gsat_1time (JNIEnv *, jclass, jlong handle, jint t){
  Glucose::SimpSolver *S = reinterpret_cast<Glucose::SimpSolver *>(handle);

  // setup the timeout handler
  // signal (SIGALRM, catchalrm);
  // alarm (t);
  
  bool res = S->solve();

  // test whether a timeout occured
  // if(terminated){
  //   return false;
  // }
  return res;  
}

/**
 * Solve the formula without a timeout
 *
 * @param handle the glucose object
 * @return true if the formula is satisfiable
 */
jboolean JNICALL Java_gluc_JGlucose_gsat (JNIEnv *, jclass, jlong handle){
  Glucose::SimpSolver *S = reinterpret_cast<Glucose::SimpSolver *>(handle);
  bool res = S->solve();
  return res;  
}

/**
 * Returns the value of the variable. Attention: 0 means true and 1 means false
 * 
 * @param a the variable (may be negated) to get the value
 * @return the value of a in the satisfying assignment of the formula 
 */
jint JNICALL Java_gluc_JGlucose_gderef  (JNIEnv *, jclass, jlong handle, jint a){
  int varl = abs(a)-1;
    Glucose::Lit l;
    if(a > 0){
      l = Glucose::mkLit(varl);
    }
    else{

      l = ~Glucose::mkLit(varl);
    }
  Glucose::SimpSolver *S = reinterpret_cast<Glucose::SimpSolver *>(handle);
  return toInt(S->modelValue(l));
  
}
