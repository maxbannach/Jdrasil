/* default includes */
#include <stdio.h>
#include <unordered_map>

/* JNI includes */
#include <jni.h>
#include "jdrasil_sat_NativeSATSolver.h"

/* IPASIR includes */
extern "C" {
  #include "ipasir.h"
}
  
//MARK: helper functions

/**
 * Hashmap to store the termination state of a solver.
 */
typedef std::unordered_map< void*, int > hashmap;
hashmap isTerminated;

/**
 * The three possible states a IPASIR solver can be in.
 */
enum State {
  SAT   = 0,
  UNSAT = 1,
  INPUT = 2
};

// name of the corresponding Sates in Java
const char *jStates[] = { "SAT", "UNSAT", "INPUT" };

/**
 * The calling object will hold a pointer to an "instance" of the solver,
 * this method will set it.
 */
static void setInstance(JNIEnv* env, jobject callingObject, void* instance) {
  jclass clazz = env->GetObjectClass(callingObject);
  jmethodID method = env->GetMethodID(clazz, "setPointer", "(J)V");
  env->CallVoidMethod(callingObject, method, (jlong) (intptr_t) instance);
}

/**
 * The calling object will hold a pointer to an "instance" of the solver,
 * this method will extract it.
 */
static void* getInstance(JNIEnv* env, jobject callingObject) {
  jclass clazz = env->GetObjectClass(callingObject);
  jmethodID method = env->GetMethodID(clazz, "getPointer", "()J");
  long instance = env->CallLongMethod(callingObject, method);
  
  return (void*) instance;
}

/**
 * During the solving proccess, the sat solver will call this method to check if it has to stop.
 */
static int terminationCallback(void* instance) {
  return isTerminated[instance];
}

/**
 * An IPASIR solver is in one of the states INPUT, SAT, UNSAT. This behavior is also represented
 * on the Java side. This method is used to update the state on the Java side.
 */
static void setSolverState(JNIEnv* env, jobject callingObject, State state) {
  jclass clazz = env->GetObjectClass(callingObject);
  jmethodID method = env->GetMethodID(clazz, "setCurrentState", "(Ljdrasil/sat/ISATSolver$State;)V");
  
  jclass jstate = env->FindClass("jdrasil/sat/ISATSolver$State");
  jfieldID satField  = env->GetStaticFieldID(jstate, jStates[state], "Ljdrasil/sat/ISATSolver$State;");
  jobject value = env->GetStaticObjectField(jstate, satField);
  
  env->CallVoidMethod(callingObject, method, value);  
}

//MARK: JNI implementations

JNIEXPORT jstring JNICALL Java_jdrasil_sat_NativeSATSolver_signature(JNIEnv* env, jobject callingObject) {
  return env->NewStringUTF(ipasir_signature());
}

JNIEXPORT void JNICALL Java_jdrasil_sat_NativeSATSolver_init(JNIEnv* env, jobject callingObject) {
  void* instance = ipasir_init();
  isTerminated[instance] = 0;  
  setInstance(env, callingObject, instance);
  setSolverState(env, callingObject, INPUT);  
}

JNIEXPORT void JNICALL Java_jdrasil_sat_NativeSATSolver_release(JNIEnv* env, jobject callingObject) {
  void* instance = getInstance(env, callingObject);
  ipasir_release(instance);
  fflush(stdout);
}

JNIEXPORT void JNICALL Java_jdrasil_sat_NativeSATSolver_add(JNIEnv* env, jobject callingObject, jint literal) {
  void* instance = getInstance(env, callingObject);  
  ipasir_add(instance, literal);
  setSolverState(env, callingObject, INPUT);
}

JNIEXPORT void JNICALL Java_jdrasil_sat_NativeSATSolver_assume(JNIEnv* env, jobject callingObject, jint literal) {
  void* instance = getInstance(env, callingObject);
  ipasir_assume(instance, literal);
  setSolverState(env, callingObject, INPUT);
}

JNIEXPORT jint JNICALL Java_jdrasil_sat_NativeSATSolver_solve(JNIEnv* env, jobject callingObject) {
  void* instance = getInstance(env, callingObject);
  isTerminated[instance] = 0;
  ipasir_set_terminate(instance, instance, terminationCallback);

  int result = ipasir_solve(instance);
  switch (result) {
  case 10:
      setSolverState(env, callingObject, SAT);
      break;
  case 20:
      setSolverState(env, callingObject, UNSAT);
      break;
  default:
      setSolverState(env, callingObject, INPUT);
  }
  return result;
}

JNIEXPORT jint JNICALL Java_jdrasil_sat_NativeSATSolver_val(JNIEnv* env, jobject callingObject, jint literal) {
  void* instance = getInstance(env, callingObject);
  return ipasir_val(instance, literal);
}

JNIEXPORT jboolean JNICALL Java_jdrasil_sat_NativeSATSolver_failed(JNIEnv* env, jobject callingObject, jint literal) {
  void* instance = getInstance(env, callingObject);
  return (jboolean) ipasir_failed(instance, literal);
}

JNIEXPORT void JNICALL Java_jdrasil_sat_NativeSATSolver_terminate(JNIEnv* env, jobject callingObject) {
  void* instance = getInstance(env, callingObject);
  isTerminated[instance] = 1;
}
