SCRIPTPATH=$(dirname "$0")
cd $SCRIPTPATH
sber=kermit
host=$(hostname)
javac gluc/JGlucose.java
javah -jni gluc.JGlucose
make clean
make


$CXX -c -w -I../ -I$JAVA_INCLUDE_DIR  gluc_JGlucose.cpp -o JGlucose.o
$CXX -w -shared -o libJGlucose.jnilib JGlucose.o SimpSolver.o ../core/*.o

#MAC
#g++ -c -w  -I../ -I/Applications/Xcode.app/Contents/Developer/Platforms/MacOSX.platform/Developer/SDKs/MacOSX10.12.sdk/System/Library/Frameworks/JavaVM.framework/Headers/
# g++ -w -shared -o libJGlucose.jnilib JGlucose.o SimpSolver.o ../core/*.o

# For Linux
#g++ -fPIC -c -I../ -I"/home/sebastianberndt/jdk/include/" -I"/home/sebastianberndt/jdk/include/linux/" -c gluc_JGlucose.cpp
#g++ gluc_JGlucose.o -shared -o libJGlucose.so -Wl,-soname,-rpath SimpSolver.o ../core/*.o

jar cf glucose.jar gluc/JGlucose.class
cp glucose.jar ../../
