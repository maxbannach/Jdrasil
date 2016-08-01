#!/bin/bash

cd lib/pblib/
cmake .
make clean
make pblib
./makeJNI_linux.sh
