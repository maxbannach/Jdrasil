#!/bin/bash

cd lib/pblib/
cp CMakeListsMAC.txt CMakeLists.txt
cmake .
make clean
make pblib
./makeJNI.sh
