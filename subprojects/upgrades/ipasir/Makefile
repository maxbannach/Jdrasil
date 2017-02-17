JNI = $(shell jrunscript -e 'java.lang.System.out.println(java.lang.System.getProperty("java.home"));')/../include
JNIMDMAC = $(JNI)/darwin
JNIMDLINUX = $(JNI)/linux
JNIMDWIN32 = $(JNI)/win32
OS := $(shell uname -s)

all: targets

ifeq ($(OS),Darwin)
targets: jniMac
else
targets: jniLinux
endif

jniMac:
	g++ -Xlinker -dylib -L. -lipasirsolver -I$(JNI) -I$(JNIMDMAC) -I$(JNIMDLINUX) -I$(JNIMDWIN32)  -o libjdrasil_sat_NativeSATSolver.dylib jdrasil_sat_NativeSATSolver.cpp

jniLinux:
	g++  -Wl,-rpath=$(shell pwd) -fPIC -shared --std=gnu++11 -L. -lipasirsolver -I$(JNI) -I$(JNIMDMAC) -I$(JNIMDLINUX) -I$(JNIMDWIN32)  -o libjdrasil_sat_NativeSATSolver.so jdrasil_sat_NativeSATSolver.cpp

.PHONY: jniMac jniLinux
