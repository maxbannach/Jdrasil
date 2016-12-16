JAVAC = javac

SRC = src/
BIN = bin/
UP = upgrades/
DOC = doc/
MANUAL = manual/

files = $(shell find src -type f -name '*.java' | sed "s|^src/||")
sources = $(addprefix $(SRC),$(files))
classes = $(addprefix $(BIN), $(files:.java=.class))
#tex = $(shell find $(MANUAL) -type f -name '*.tex')

all: $(BIN) $(classes)

$(UP):
	mkdir $(upgrades)

$(BIN):
	mkdir $(BIN)

$(BIN)%.class: $(SRC)%.java
	$(JAVAC) -sourcepath $(SRC) -d $(BIN) $<

jdrasil.jar: $(BIN) $(classes)
	jar cvfe jdrasil.jar jdrasil.App -C $(BIN) .

jar: jdrasil.jar

$(UP)ipasir/jdrasil_sat_NativeSATSolver.h: $(classes)
	javah -jni -cp bin -d $(UP)ipasir jdrasil.sat.NativeSATSolver

cinterface: $(UP)ipasir/jdrasil_sat_NativeSATSolver.h

$(DOC):
	mkdir $(DOC)
	mkdir $(DOC)html
	mkdir $(DOC)manual

documentation: $(DOC)
	javadoc -header "<script type="text/javascript" src="http://cdn.mathjax.org/mathjax/latest/MathJax.js?config=TeX-AMS-MML_HTMLorMML"></script>" -d $(DOC)html -sourcepath $(SRC) -subpackages jdrasil

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

# Upgrades

upgrade-sat4j:
	wget http://download.forge.ow2.org/sat4j/sat4j-core-v20130525.zip
	unzip sat4j-core-v20130525.zip
	mv org.sat4j.core.jar $(UP)
	rm -rf sat4j-core-v20130525.zip
	rm org.sat4j.core-src.jar

.PHONY: clean jni jar
