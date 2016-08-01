/**
 * This is the wrapper class that allows Glucose (parallel) to
 * communicate with Java by giving an appropriate API
 * author: berndt
 */

#include "glucp_JPGlucose.h"
#include "ParallelSolver.h"
#include "MultiSolvers.h"
#include <stdint.h>
#include <iostream>
#include <string> 
#include <signal.h>
#include <unistd.h>

// initialize the solver and deactivate the output
jlong JNICALL Java_glucp_JPGlucose_ginit(JNIEnv *env, jclass obj){
  jlong j = reinterpret_cast<jlong>(new Glucose::MultiSolvers());
  Glucose::MultiSolvers *S = reinterpret_cast<Glucose::MultiSolvers *>(j);
  S->setVerbosity(-1);
  return  j;
}

/**
 * Add a clause to the instance. 
 *
 * @param handle The pointer to the instance
 * @param xs the new clause
 * @return whether the clause was added successfully
 */
jboolean JNICALL Java_glucp_JPGlucose_gadd(JNIEnv *env, jclass obj, jlong handle,  jintArray xs){
  // get the glucose object and the C++ array
  Glucose::MultiSolvers *S = reinterpret_cast<Glucose::MultiSolvers *>(handle);
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
    
    vs.push(l);
  }
  
  bool res = S->addClause_(vs);
  
  return res;
}





/**
 * Solve the formula with a timeout
 * 
 * @param handle the glucose object
 * @param t the allowed time
 * @return true, if the formula is satisfiable within time t
 */
jboolean JNICALL Java_glucp_JPGlucose_gsat_1time (JNIEnv *, jclass, jlong handle, jint t){
  Glucose::MultiSolvers *S = reinterpret_cast<Glucose::MultiSolvers *>(handle);

  bool res = toInt(S->solve()) == 0;
  return res;  
}

/**
 * Solve the formula without a timeout
 *
 * @param handle the glucose object
 * @return true if the formula is satisfiable
 */
jboolean JNICALL Java_glucp_JPGlucose_gsat (JNIEnv *, jclass, jlong handle){
  Glucose::MultiSolvers *S = reinterpret_cast<Glucose::MultiSolvers *>(handle);
  int ret2 = S->simplify();
  if(ret2){
    S->eliminate();
  }
  if(!ret2 || !S->okay()){
    return false;
  }
  int r = toInt(S->solve());
  bool res = r == 0;
  return res;  
}

/**
 * Returns the value of the variable. Attention: 0 means true and 1 means false
 * 
 * @param a the variable (may be negated) to get the value
 * @return the value of a in the satisfying assignment of the formula 
 */
jint JNICALL Java_glucp_JPGlucose_gderef  (JNIEnv *, jclass, jlong handle, jint a){
  int varl = abs(a)-1;
    Glucose::Lit l;
    if(a > 0){
      l = Glucose::mkLit(varl);
    }
    else{

      l = ~Glucose::mkLit(varl);
    }
  Glucose::MultiSolvers *S = reinterpret_cast<Glucose::MultiSolvers *>(handle);
  return toInt(S->model[varl]);
}


