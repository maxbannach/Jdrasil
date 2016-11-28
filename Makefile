JAVAC = javac

SRC = src/
BIN = bin/
DOC = doc/
MANUAL = manual/

files = $(shell find src -type f -name '*.java' | sed "s|^src/||")
sources = $(addprefix $(SRC),$(files))
classes = $(addprefix $(BIN), $(files:.java=.class))
#tex = $(shell find $(MANUAL) -type f -name '*.tex')

all: $(BIN) $(classes)

$(BIN):
	mkdir $(BIN)

$(BIN)%.class: $(SRC)%.java
	$(JAVAC) -sourcepath $(SRC) -d $(BIN) $<

App.jar: $(BIN) $(classes)
	jar cvfe App.jar jdrasil.App -C $(BIN) .

jar: App.jar

$(DOC):
	mkdir $(DOC)

documentation: $(DOC)
	javadoc -d $(DOC) -sourcepath $(SRC) -subpackages de

$(MANUAL)manual.pdf: $(tex)
	(cd $(MANUAL) && lualatex manual.tex)

manual: $(MANUAL)manual.pdf

clean:
	rm -rf $(BIN)
	rm -rf $(DOC)
	rm $(MANUAL)*.aux
	rm $(MANUAL)*.log
	rm $(MANUAL)*.pdf

.PHONY: clean
