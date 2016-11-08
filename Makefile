# Linux Version
export JAVA_COMPILER=/usr/lib/jvm/java-8-openjdk-amd64/bin/javac
export CXX=/usr/bin/g++
export JAVA_INCLUDE_DIR=/usr/lib/jvm/java-8-openjdk-amd64/include/
export JAVA_INCLUDE_DIR_LOCAL=/usr/lib/jvm/java-8-openjdk-amd64/include/linux/
export JAVA_EXECUTABLE=/usr/lib/jvm/java-8-openjdk-amd64/bin/javac

# Uncomment for MAC
#export JAVA_COMPILER=/usr/bin/javac
#export CXX=/usr/local/bin/g++-6
#export JAVA_INCLUDE_DIR=/Applications/Xcode.app/Contents/Developer/Platforms/MacOSX.platform/Developer/SDKs/MacOSX10.12.sdk/System/Library/Frameworks/JavaVM.framework/Headers/
#export JAVA_INCLUDE_DIR_LOCAL=/Applications/Xcode.app/Contents/Developer/Platforms/MacOSX.platform/Developer/SDKs/MacOSX10.12.sdk/System/Library/Frameworks/JavaVM.framework/Headers/
#export JAVA_EXECUTABLE=/usr/bin/java

all:	glucose pblib java execs

execs:
	./renameExecs.sh

glucose:
	./makeGlucose.sh

pblib:
	./makePBLib.sh

java:
	./makeJava.sh

test:
	./tw-exact -s 1234 < instances/ClebschGraph.gr
	./tw-exact-parallel -s 1234 < instances/McGeeGraph.gr
	./tw-heuristic -s 1234 < instances/NauruGraph.gr
	./tw-heuristic-parallel -s 1234 < instances/DoubleStarSnark.gr

