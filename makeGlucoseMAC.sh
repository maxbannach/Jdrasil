#!/bin/bash

cd lib/glucose/simp/
make clean
make
./makeJNI.sh
cd ../parallel
make clean
make
./makeJNI.sh
