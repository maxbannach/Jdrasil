javac pseudo/PBLib.java
javah -jni pseudo.PBLib

g++ -std=c++0x -c -fPIC -w  -I../ -I"/usr/lib/jvm/java-8-openjdk-amd64/include/" -I"/usr/lib/jvm/java-8-openjdk-amd64/include/linux/" -o PBLib.o pseudo_PBLib.cpp
g++ -std=c++0x  -w -shared -o libPBLib.so -Wl,-soname,-rpath PBLib.o libpblib.a

jar cf pblib.jar pseudo/PBLib.class
cp pblib.jar ../
