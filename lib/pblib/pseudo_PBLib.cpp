/** 
 *  This is the wrapper class that allows PBLib to communicate with Java
 *  by giving an appropriate API.  
 *  author: berndt
 */
#include "pseudo_PBLib.h"
#include "pb2cnf.h"
#include "VectorClauseDatabase.h"
#include <vector>

// We will use those Java classes, their constructors
// (e.g. java_util_ArrayList_) and their methods later on.
static jclass java_util_ArrayList;
static jmethodID java_util_ArrayList_;
static jclass java_lang_Integer;
static jmethodID java_lang_Integer_;
jmethodID java_util_ArrayList_size;
jmethodID java_util_ArrayList_get;
jmethodID java_util_ArrayList_add;

// This is our JNI-environment carrying all necessary informations
static thread_local JNIEnv* env;

// The PBLib-library maintains its clauses in a
// VectorClauseDatabase-object. To produce those values, it needs a
// config object. We use this object to maintain those informations.
class myDatas{
 public:
  myDatas(PBConfig config);
  VectorClauseDatabase formula;
  PB2CNF pb2cnf;
  IncPBConstraint constraint;
};

// The constructor of the datas object
myDatas::myDatas(PBConfig config):
  formula(config),  pb2cnf(config),  constraint()
{
}

// Initialize everything by assigning the Java classes and methods to their variables. 
void init(JNIEnv *env) {
  java_util_ArrayList      = static_cast<jclass>(env->NewGlobalRef(env->FindClass("java/util/ArrayList")));
  java_util_ArrayList_     = env->GetMethodID(java_util_ArrayList, "<init>", "()V");
  java_lang_Integer      = static_cast<jclass>(env->NewGlobalRef(env->FindClass("java/lang/Integer")));
  java_lang_Integer_     = env->GetMethodID(java_lang_Integer, "<init>", "(I)V");
  java_util_ArrayList_size = env->GetMethodID (java_util_ArrayList, "size", "()I");
  java_util_ArrayList_get  = env->GetMethodID(java_util_ArrayList, "get", "(I)Ljava/lang/Object;");
  java_util_ArrayList_add  = env->GetMethodID(java_util_ArrayList, "add", "(Ljava/lang/Object;)Z");
}

/**
 * Convert a C++ std::vector of std::vectors of ints to a Java ArrayList of ArrayLists of ints
 */
jobject cpp2java(JNIEnv *env,std::vector<std::vector<int>> vectors) {
  init(env);
  jobject result = env->NewObject(java_util_ArrayList, java_util_ArrayList_);
  for (auto v: vectors) {
    jobject l = env->NewObject(java_util_ArrayList, java_util_ArrayList_);
    for(jint i: v){
      jobject io = env->NewObject(java_lang_Integer, java_lang_Integer_,i);
      env->CallBooleanMethod(l, java_util_ArrayList_add, io);
    }
    env->CallBooleanMethod(result, java_util_ArrayList_add, l);
  }
  return result;
}

/**
 * Initialize PBLib by creating a new config and a new data object
 */
jlong JNICALL Java_pseudo_PBLib_init(JNIEnv *env, jclass obj){
  PBConfig config = std::make_shared< PBConfigClass >();
  jlong j = reinterpret_cast<jlong>(new myDatas(config));
  
  return j;
}


/**
 * Initialize an incremental atMostK-constraint by giving an initial upper bound.
 *
 * @param arr: the previous existing formula
 * @param n: the number of variables in arr
 * @param k: the upper bound
 * @param m: the maximal index of a literal+1 (the next free literal)
 * @param handle: the object handle
 * @return an ArrayList<ArrayList<Int>> representing the new constraint-formula
 */
jobject JNICALL Java_pseudo_PBLib_initIterAtMostK(JNIEnv *env, jclass, jintArray arr, jint n, jint k, jint m, jlong handle){
  myDatas *d = reinterpret_cast<myDatas *>(handle);
  AuxVarManager auxvars(m);

  // Convert the Java array to a C++ array
  jint *body = env->GetIntArrayElements(arr, 0);

  // Build a vector of pairs of weights and literals. We simply set all
  // of the weights to 1
  std::vector< PBLib::WeightedLit > literals;
  for(int i = 0; i < n; i++){
    literals.push_back(PBLib::WeightedLit(body[i],1));
  }

  // Initialize the constraints.
  IncPBConstraint constraint(literals, PBLib::LEQ, k);
  d->constraint = constraint;
  d->pb2cnf.encodeIncInital(d->constraint, d->formula, auxvars);

  // return the new formula
  return cpp2java(env,d->formula.getClauses());
}


/**
 * Decrease the already initialized incremental atMostK-constraint with a new upper bound
 *
 * @param k: the new upper bound
 * @param m: the maximal index of a literal+1 (the next free literal)
 * @param handle: the object handle
 * @return an ArrayList<ArrayList<Int>> representing the new constraint-formula
 */
jobject JNICALL Java_pseudo_PBLib_citerAtMostK(JNIEnv *env, jclass, jint k, jint m, jlong handle){
  myDatas *d = reinterpret_cast<myDatas *>(handle);
  AuxVarManager auxvars(m);

  // get the number of current clauses
  int oldc = d->formula.getClauses().size();

  // encode the new constraint
  d->constraint.encodeNewLeq(k, d->formula, auxvars);
  std::vector<std::vector<int>> clauses = d->formula.getClauses();

  // find the new constraints
  int newc = clauses.size();
  std::vector<std::vector<int>> res;

  for(int i = oldc; i < newc; i++){
    res.push_back(clauses[i]);
  }
  return cpp2java(env,res);
  
}

/**
 * Initialize an incremental atLeastK-constraint by giving an initial lower bound.
 *
 * @param arr: the previous existing formula
 * @param n: the number of variables in arr
 * @param k: the lower bound
 * @param m: the maximal index of a literal+1 (the next free literal)
 * @param handle: the object handle
 * @return an ArrayList<ArrayList<Int>> representing the new constraint-formula
 */
jobject JNICALL Java_pseudo_PBLib_initIterAtLeastK(JNIEnv *env, jclass, jintArray arr, jint n, jint k, jint m, jlong handle){
  myDatas *d = reinterpret_cast<myDatas *>(handle);
  AuxVarManager auxvars(m);

  // Convert the Java array to a C++ array
  jint *body = env->GetIntArrayElements(arr, 0);

  
  // Build a vector of pairs of weights and literals. We simply set all
  // of the weights to 1
  std::vector< PBLib::WeightedLit > literals;
  for(int i = 0; i < n; i++){
    literals.push_back(PBLib::WeightedLit(body[i],1));
  }

  // Initialize the constraints.
  IncPBConstraint constraint(literals, PBLib::GEQ, k);
  d->constraint = constraint;
  d->pb2cnf.encodeIncInital(d->constraint, d->formula, auxvars);

  // return the new formula
  return cpp2java(env,d->formula.getClauses());
}


/**
 * Increase the already initialized incremental atLeastK-constraint with a new lower bound
 *
 * @param k: the new lower bound
 * @param m: the maximal index of a literal+1 (the next free literal)
 * @param handle: the object handle
 * @return an ArrayList<ArrayList<Int>> representing the new constraint-formula
 */
jobject JNICALL Java_pseudo_PBLib_citerAtLeastK(JNIEnv *env, jclass, jint k, jint m, jlong handle){
  myDatas *d = reinterpret_cast<myDatas *>(handle);
  AuxVarManager auxvars(m);

  // get the number of current clauses
  int oldc = d->formula.getClauses().size();

    // encode the new constraint
  d->constraint.encodeNewGeq(k, d->formula, auxvars);
  std::vector<std::vector<int>> clauses = d->formula.getClauses();

    // find the new constraints
  int newc = clauses.size();
  std::vector<std::vector<int>> res;
  
  for(int i = oldc; i < newc; i++){
    res.push_back(clauses[i]);
  }
  return cpp2java(env,res);
  
}

/**
 * Generate an atMostK-constraint by giving an upper bound
 *
 * @param arr: the previous existing formula
 * @param n: the number of variables in arr
 * @param k: the upper bound
 * @param m: the maximal index of a literal+1 (the next free literal)
 * @return an ArrayList<ArrayList<Int>> representing the new constraint-formula
 */
jobject JNICALL Java_pseudo_PBLib_generateAtMostK
(JNIEnv *env, jclass, jintArray arr, jint n, jint k, jint m){
  PBConfig config = std::make_shared<PBConfigClass>();
  PB2CNF pb(config);
  std::vector<int32_t> ls;

  // Convert the Java array to a C++ array
  jint *body = env->GetIntArrayElements(arr, 0);

  // add the literals
  for(int i = 0; i < n; i++){
    ls.push_back(body[i]);
  }
    
  std::vector< std::vector< int32_t > > formula;
  pb.encodeAtMostK(ls,k,formula,m);

  return cpp2java(env,formula);

}

/**
 * Generate an atLeastK-constraint by giving an lower bound
 *
 * @param arr: the previous existing formula
 * @param n: the number of variables in arr
 * @param k: the lower bound
 * @param m: the maximal index of a literal+1 (the next free literal)
 * @return an ArrayList<ArrayList<Int>> representing the new constraint-formula
 */
jobject JNICALL Java_pseudo_PBLib_generateAtLeastK
(JNIEnv *env, jclass, jintArray arr, jint n, jint k, jint m){
  PBConfig config = std::make_shared<PBConfigClass>();
  PB2CNF pb(config);
  std::vector<int32_t> ls;

  // Convert the Java array to a C++ array
  jint *body = env->GetIntArrayElements(arr, 0);

  // add the literals
  for(int i = 0; i < n; i++){
    ls.push_back(body[i]);
  }
    
  std::vector< std::vector< int32_t > > formula;
  
  pb.encodeAtLeastK(ls,k,formula,m);

  return cpp2java(env,formula);
}
