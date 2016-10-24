#!/bin/bash
sed -i -e 's@EXEC@'"$JAVA_EXECUTABLE"'@g' tw-exact
sed -i -e 's@EXEC@'"$JAVA_EXECUTABLE"'@g' tw-exact-parallel
sed -i -e 's@EXEC@'"$JAVA_EXECUTABLE"'@g'  tw-heuristic
sed -i -e 's@EXEC@'"$JAVA_EXECUTABLE"'@g'  tw-heuristic-parallel
