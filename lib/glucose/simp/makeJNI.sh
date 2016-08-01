SCRIPTPATH=$(dirname "$0")
cd $SCRIPTPATH
sber=kermit
host=$(hostname)
javac gluc/JGlucose.java
javah -jni gluc.JGlucose
make clean
make

# MAC
 g++ -c -w  -I../ -I/Applications/Xcode.app/Contents/Developer/Platforms/MacOSX.platform/Developer/SDKs/MacOSX10.11.sdk/System/Library/Frameworks/JavaVM.framework/Headers/ gluc_JGlucose.cpp -o JGlucose.o
 g++ -w -shared -o libJGlucose.jnilib JGlucose.o SimpSolver.o ../core/*.o

# For Linux
#g++ -fPIC -c -I../ -I"/home/sebastianberndt/jdk/include/" -I"/home/sebastianberndt/jdk/include/linux/" -c gluc_JGlucose.cpp
#g++ gluc_JGlucose.o -shared -o libJGlucose.so -Wl,-soname,-rpath SimpSolver.o ../core/*.o

jar cf glucose.jar gluc/JGlucose.class
cp glucose.jar ../../
