CC=gcc
CFLAGS=-Wall -O3 -DNLGLOG -DNDEBUG -DNCHKSOL -DNLGLDRUPLIG -DNLGLYALSAT -DNLGLFILES -DNLGLDEMA
LINGELING=lingeling
IPASIR=$(shell cd .. && pwd)/
OS := $(shell uname -s)

all: targets

ifeq ($(OS),Darwin)
targets: libipasirsolver.dylib
else
targets: libipasirsolver.so
endif

libipasirsolver.dylib:
	cd lingeling; ./configure.sh; make
	$(CC) $(CFLAGS) -Xlinker -dylib -Wl,-install_name,$(IPASIR)$@ -o libipasirsolver.dylib ipasirsolver.c -I$(LINGELING) -L$(LINGELING) -llgl
	cp libipasirsolver.dylib ..

libipasirsolver.so:
	cd lingeling; CC="gcc -fPIC" ./configure.sh; make;
	$(CC) $(CFLAGS) -fPIC  ipasirsolver.c -shared -o libipasirsolver.so -Wl,-rpath,. -I$(LINGELING) -L$(LINGELING) -llgl
	cp libipasirsolver.so ..

.PHONY: libipasirsolver.dylib
