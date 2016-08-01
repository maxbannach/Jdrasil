javac gluc/JGlucose.java
javah -jni gluc.JGlucose

g++ -fPIC -c -I../ -I"/usr/lib/jvm/java-8-openjdk-amd64/include/" -I"/usr/lib/jvm/java-8-openjdk-amd64/include/linux/" -c gluc_JGlucose.cpp
g++ gluc_JGlucose.o -shared -o libJGlucose.so -Wl,-soname,-rpath SimpSolver.o ../core/*.o

jar cf glucose.jar gluc/JGlucose.class
cp glucose.jar ../../
