export JAVA_COMPILER=/usr/bin/JAVAC
export CXX=/usr/local/bin/g++-6
export JAVA_INCLUDE_DIR=/Applications/Xcode.app/Contents/Developer/Platforms/MacOSX.platform/Developer/SDKs/MacOSX10.12.sdk/System/Library/Frameworks/JavaVM.framework/Headers/
export JAVA_EXECUTABLE=/usr/bin/java

all:	glucose pblib java execs

execs:
	sed -i -e 's@EXEC@'"$JAVA_EXECUTABLE"'@g' tw-exact
	sed -i -e 's@EXEC@'"$JAVA_EXECUTABLE"'@g' tw-exact-parallel
	sed -i -e 's@EXEC@'"$JAVA_EXECUTABLE"'@g' tw-heuristic
	sed -i -e 's@EXEC@'"$JAVA_EXECUTABLE"'@g' tw-heuristic-parallel

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

