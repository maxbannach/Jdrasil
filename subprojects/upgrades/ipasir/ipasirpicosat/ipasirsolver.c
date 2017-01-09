#include "picosat.h"
#include "ipasir.h"

const char* ipasir_signature () {
  return "PicoSAT";
}

void * ipasir_init() {
  PicoSAT* instance = picosat_init();
  return instance;
}

void ipasir_release(void* instance) {
  picosat_reset(instance);
}

void ipasir_add(void* instance, int literal) {
  picosat_add(instance, literal);
}

void ipasir_assume(void* instance, int literal) {
  picosat_assume(instance, literal);
}

int ipasir_solve(void* instance) {
  return picosat_sat(instance, -1);
}

int ipasir_val(void* instance, int literal) {
  int val = picosat_deref(instance, literal);
  if (!val) return 0;
  return val < 0 ? -1*literal : literal;
}

int ipasir_failed(void* instance, int literal) {
  return picosat_failed_assumption(instance, literal) ? 1 : 0;
}

void ipasir_set_terminate(void* instance, void* state, int (*terminate)(void * state)) {
  picosat_set_interrupt(instance, state, terminate);
}
