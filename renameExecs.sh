#!/bin/bash
sed -i 's@EXEC@'"$JAVA_EXECUTABLE"'@g' tw-exact
sed -i 's@EXEC@'"$JAVA_EXECUTABLE"'@g' tw-exact-parallel
sed -i 's@EXEC@'"$JAVA_EXECUTABLE"'@g'  tw-heuristic
sed -i 's@EXEC@'"$JAVA_EXECUTABLE"'@g'  tw-heuristic-parallel
