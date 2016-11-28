JAVAC = javac

JNI = /Library/Java/JavaVirtualMachines/jdk1.8.0_45.jdk/Contents/Home/include
JNI2 = /Library/Java/JavaVirtualMachines/jdk1.8.0_45.jdk/Contents/Home/include/darwin

SRC = src/
BIN = bin/
LIB = lib/
DOC = doc/
MANUAL = manual/

files = $(shell find src -type f -name '*.java' | sed "s|^src/||")
sources = $(addprefix $(SRC),$(files))
classes = $(addprefix $(BIN), $(files:.java=.class))
#tex = $(shell find $(MANUAL) -type f -name '*.tex')

all: $(BIN) $(classes)

$(LIB):
	mkdir $(LIB)

$(BIN):
	mkdir $(BIN)

$(BIN)%.class: $(SRC)%.java
	$(JAVAC) -sourcepath $(SRC) -d $(BIN) $<

jdrasil.jar: $(BIN) $(classes)
	jar cvfe jdrasil.jar jdrasil.App -C $(BIN) .

jar: jdrasil.jar

$(LIB)jdrasil_sat_NativeSATSolver.h: $(classes)
	javah -jni -cp bin -d lib jdrasil.sat.NativeSATSolver

jni: $(LIB)jdrasil_sat_NativeSATSolver.h

$(LIB)libsatsolver.dylib:
	(cd $(LIB) && gcc -bundle -I${JNI} -I${JNI2} -o libsatsolver.dylib test.c)

nativesolver: $(LIB)libsatsolver.dylib

$(DOC):
	mkdir $(DOC)

documentation: $(DOC)
	javadoc -d $(DOC) -sourcepath $(SRC) -subpackages de

$(MANUAL)manual.pdf: $(tex)
	(cd $(MANUAL) && lualatex manual.tex)

manual: $(MANUAL)manual.pdf

clean:
	rm jdrasil.jar
	rm -rf $(BIN)
	rm -rf $(DOC)
	rm $(MANUAL)*.aux
	rm $(MANUAL)*.log
	rm $(MANUAL)*.pdf

.PHONY: clean jni jar
