#include "lglib.h"
#include "ipasir.h"

const char* ipasir_signature () {
  return "Lingeling";
}

void * ipasir_init() {
  LGL* instance = lglinit();
  return instance;
}

void ipasir_release(void* instance) {
  lglrelease(instance);
}

void ipasir_add(void* instance, int literal) {
  if (literal != 0) {
    lglfreeze(instance, literal);
    lglfreeze(instance, -1*literal);
  }
  lgladd(instance, literal);
}

void ipasir_assume(void* instance, int literal) {
  lglassume(instance, literal);
}

int ipasir_solve(void* instance) {
  return lglsat(instance);
}

int ipasir_val(void* instance, int literal) {
  int val = lglderef(instance, literal);  
  return val < 0 ? -1*literal : literal;  
}

int ipasir_failed(void* instance, int literal) {
  int val = lglfailed(instance, literal);
  return val < 0 ? 0 : 1;
}

void ipasir_set_terminate(void* instance, void* state, int (*terminate)(void * state)) {
  lglseterm(instance, terminate, state);
}
