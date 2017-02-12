CC=g++
CFLAGS=-Wall -O3 -DNLGLOG -DNDEBUG -DNCHKSOL -DNLGLDRUPLIG -DNLGLYALSAT -DNLGLFILES -DNLGLDEMA
GLUCOSE=glucose
IPASIR=$(shell cd .. && pwd)/


all: targets

targets: libipasirsolver.dylib

libipasirsolver.dylib:
		make -C glucose lr
		cp glucose/build/release/lib/libglucose.a glucose/
		$(CC) $(CFLAGS) -Xlinker -dylib -Wl,-install_name,$(IPASIR)$@ -o libipasirsolver.dylib ipasirsolver.cc -I$(GLUCOSE) -L$(GLUCOSE) -lglucose
	cp libipasirsolver.dylib ..
.PHONY: libipasirsolver.dylib
