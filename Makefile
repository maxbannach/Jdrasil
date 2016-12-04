JAVAC = javac

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

$(LIB)ipasir/jdrasil_sat_NativeSATSolver.h: $(classes)
	javah -jni -cp bin -d $(LIB)ipasir jdrasil.sat.NativeSATSolver

cinterface: $(LIB)ipasir/jdrasil_sat_NativeSATSolver.h

$(DOC):
	mkdir $(DOC)
	mkdir $(DOC)html
	mkdir $(DOC)manual

documentation: $(DOC)
	javadoc -d $(DOC)html -sourcepath $(SRC) -subpackages jdrasil

$(MANUAL)manual.pdf: $(DOC) $(tex)
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
