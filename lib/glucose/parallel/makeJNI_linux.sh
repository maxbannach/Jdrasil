javac glucp/JPGlucose.java
javah -jni glucp.JPGlucose

g++ -fPIC -c -I../ -I"/usr/lib/jvm/java-8-openjdk-amd64/include/" -I"/usr/lib/jvm/java-8-openjdk-amd64/include/linux/" -c glucp_JPGlucose.cpp
g++ glucp_JPGlucose.o -shared -o libJPGlucose.so -Wl,-soname,-rpath MultiSolvers.o ParallelSolver.o SharedCompanion.o SolverConfiguration.o ClausesBuffer.o SolverCompanion.o ../utils/System.o ../core/*.o ../simp/SimpSolver.o

jar cf glucosep.jar glucp/JPGlucose.class 
cp glucosep.jar ../../
