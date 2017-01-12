JNI = $(shell jrunscript -e 'java.lang.System.out.println(java.lang.System.getProperty("java.home"));')/../include
JNIMDMAC = $(JNI)/darwin
JNIMDLINUX = $(JNI)/linux
JNIMDWIN32 = $(JNI)/win32

all: targets

targets: jni

jni:
	g++ -Xlinker -dylib -L. -lipasirsolver -I$(JNI) -I$(JNIMDMAC) -I$(JNIMDLINUX) -I$(JNIMDWIN32)  -o libjdrasil_sat_NativeSATSolver.dylib jdrasil_sat_NativeSATSolver.cpp

.PHONY: jni
