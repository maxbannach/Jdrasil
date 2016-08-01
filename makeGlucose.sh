#!/bin/bash

cd lib/glucose/simp/
make clean
make
./makeJNI_linux.sh
cd ../parallel
make clean
make
./makeJNI_linux.sh
