#include "JGlucose.h"
#include "SimpSolver.h"
#include <stdint.h>
#include <iostream>
#include <string> 





jlong JNICALL Java_JGlucose_ginit(JNIEnv *env, jclass obj){
  jlong j = reinterpret_cast<jlong>(new Glucose::SimpSolver());
  return  j;
}

jboolean JNICALL Java_JGlucose_gadd(JNIEnv *env, jclass obj, jlong handle,  jintArray xs){
  Glucose::SimpSolver *S = reinterpret_cast<Glucose::SimpSolver *>(handle);
  jsize n = env->GetArrayLength(xs);
  jint *body = env->GetIntArrayElements(xs, NULL);
  Glucose::vec<Glucose::Lit> vs;
  for(int i = 0 ; i < n; i++){
    int a = body[i];

    Glucose::Lit l;
    if(a > 0){
      l = Glucose::mkLit(a-1);
    }
    else{
      l = ~Glucose::mkLit(-a-1);
    }
    vs.push(l);
    S->newVar();
  }
  
  bool res = S->addClause(vs);
  return res;
}


jboolean JNICALL Java_JGlucose_gsat (JNIEnv *, jclass, jlong handle){
  Glucose::SimpSolver *S = reinterpret_cast<Glucose::SimpSolver *>(handle);
  bool res = S->solve();
  return res;  
}

jint JNICALL Java_JGlucose_gderef  (JNIEnv *, jclass, jlong handle, jint l){
  Glucose::Lit lit;
    if(l > 0){
      lit = Glucose::mkLit(l-1);
    }
    else{
      lit = ~Glucose::mkLit(-l-1);
    }
  Glucose::SimpSolver *S = reinterpret_cast<Glucose::SimpSolver *>(handle);
  return toInt(S->modelValue(lit));
  
}
