CC=g++
CFLAGS=-Wall -O3 -DNLGLOG -DNDEBUG -DNCHKSOL -DNLGLDRUPLIG -DNLGLYALSAT -DNLGLFILES -DNLGLDEMA
GLUCOSE=glucose
IPASIR=$(shell cd .. && pwd)/
OS := $(shell uname -s)

all: targets

ifeq ($(OS),Darwin)
targets: libipasirsolver.dylib
else
targets: libipasirsolver.so
endif

libipasirsolver.dylib:
	make -C glucose lr
	cp glucose/build/release/lib/libglucose.a glucose/
	$(CC) $(CFLAGS) -Xlinker -dylib -Wl,-install_name,$(IPASIR)$@ -o libipasirsolver.dylib ipasirsolver.cc -I$(GLUCOSE) -L$(GLUCOSE) -lglucose
	cp libipasirsolver.dylib ..

libipasirsolver.so:
	CXX="g++ -fPIC" make -C glucose lr
	cp glucose/build/release/lib/libglucose.a glucose/
	$(CC) $(CFLAGS) -std=c++11 -fPIC  ipasirsolver.cc -shared -o libipasirsolver.so -Wl,-rpath,. -I$(GLUCOSE) -L$(GLUCOSE) -lglucose	
	cp libipasirsolver.so ..

.PHONY: libipasirsolver.dylib libipasirsolver.so
