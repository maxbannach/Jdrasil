SCRIPTPATH=$(dirname "$0")
cd $SCRIPTPATH
sber=kermit
host=$(hostname)
javac pseudo/PBLib.java
javah -jni pseudo.PBLib

$CXX -std=c++0x -c -w -I../ -I$JAVA_INCLUDE_DIR -o PBLib.o pseudo_PBLib.cpp
$CXX -std=c++0x  -w -shared -o libPBLib.jnilib PBLib.o libpblib.a 
#/usr/local/bin/g++-6 -std=c++0x -c -w  -I../ -I/Applications/Xcode.app/Contents/Developer/Platforms/MacOSX.platform/Developer/SDKs/MacOSX10.12.sdk/System/Library/Frameworks/JavaVM.framework/Headers/ -o PBLib.o pseudo_PBLib.cpp
#/usr/local/bin/g++-6 -std=c++0x  -w -shared -o libPBLib.jnilib PBLib.o libpblib.a 
jar cf pblib.jar pseudo/PBLib.class
cp pblib.jar ../
