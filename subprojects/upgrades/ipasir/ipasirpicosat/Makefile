CC=gcc
CFLAGS=-Wall -O3 -DNLGLOG -DNDEBUG -DNCHKSOL -DNLGLDRUPLIG -DNLGLYALSAT -DNLGLFILES -DNLGLDEMA
PICOSAT=picosat
IPASIR=$(shell cd .. && pwd)/


all: targets

targets: libipasirsolver.dylib

libipasirsolver.dylib:
		$(CC) $(CFLAGS) -Xlinker -dylib -Wl,-install_name,$(IPASIR)$@ -o libipasirsolver.dylib ipasirsolver.c -I$(PICOSAT) -L$(PICOSAT) -lpicosat
	cp libipasirsolver.dylib ..
.PHONY: libipasirsolver.dylib
