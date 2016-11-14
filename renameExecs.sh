#!/bin/bash

unamestr=`uname`
if [[ "$unamestr" == "Darwin" ]]; then
    sed -i '' -e's@EXEC@'"$JAVA_EXECUTABLE"'@g' tw-exact
    sed -i '' -e's@EXEC@'"$JAVA_EXECUTABLE"'@g' tw-exact-parallel
    sed -i '' -e's@EXEC@'"$JAVA_EXECUTABLE"'@g'  tw-heuristic
    sed -i ''  -e's@EXEC@'"$JAVA_EXECUTABLE"'@g'  tw-heuristic-parallel
else
    sed -i  -e's@EXEC@'"$JAVA_EXECUTABLE"'@g' tw-exact
    sed -i  -e's@EXEC@'"$JAVA_EXECUTABLE"'@g' tw-exact-parallel
    sed -i  -e's@EXEC@'"$JAVA_EXECUTABLE"'@g'  tw-heuristic
    sed -i   -e's@EXEC@'"$JAVA_EXECUTABLE"'@g'  tw-heuristic-parallel
fi
