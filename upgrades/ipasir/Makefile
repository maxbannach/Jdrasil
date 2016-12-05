JNI = /Library/Java/JavaVirtualMachines/jdk1.8.0_45.jdk/Contents/Home/include
JNIMD = /Library/Java/JavaVirtualMachines/jdk1.8.0_45.jdk/Contents/Home/include/darwin

all: targets

targets: jni

jni:
	g++ -Xlinker -dylib -L. -lipasirsolver -I$(JNI) -I$(JNIMD)  -o libjdrasil_sat_NativeSATSolver.dylib jdrasil_sat_NativeSATSolver.cpp

.PHONY: jni
