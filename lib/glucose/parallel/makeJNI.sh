SCRIPTPATH=$(dirname "$0")
cd $SCRIPTPATH
javac glucp/JPGlucose.java
javah -jni glucp.JPGlucose

make clean
make

# MAC
 g++ -c -w  -I../ -I/Applications/Xcode.app/Contents/Developer/Platforms/MacOSX.platform/Developer/SDKs/MacOSX10.11.sdk/System/Library/Frameworks/JavaVM.framework/Headers/ glucp_JPGlucose.cpp -o JPGlucose.o
 g++ -w -shared -o libJPGlucose.jnilib JPGlucose.o MultiSolvers.o ParallelSolver.o SharedCompanion.o SolverConfiguration.o ClausesBuffer.o SolverCompanion.o ../utils/System.o ../core/*.o ../simp/SimpSolver.o

# For Linux
#g++ -fPIC -c -I../ -I"/home/sebastianberndt/jdk/include/" -I"/home/sebastianberndt/jdk/include/linux/" -c glucp_JPGlucose.cpp
#g++ glucp_JPGlucose.o -shared -o libJPGlucose.so -Wl,-soname,-rpath MultiSolvers.o ParallelSolver.o SharedCompanion.o SolverConfiguration.o ClausesBuffer.o SolverCompanion.o ../utils/System.o ../core/*.o ../simp/SimpSolver.o

jar cf glucosep.jar glucp/JPGlucose.class 
cp glucosep.jar ../../
