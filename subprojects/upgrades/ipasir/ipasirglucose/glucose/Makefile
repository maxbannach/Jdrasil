###################################################################################################

.PHONY:	r d p sh cr cd cp csh lr ld lp lsh config all install install-headers install-lib\
        install-bin clean distclean
all:	r lr lsh

## Load Previous Configuration ####################################################################

-include config.mk

## Configurable options ###########################################################################

# Directory to store object files, libraries, executables, and dependencies:
BUILD_DIR      ?= build

# Include debug-symbols in release builds
GLUCOSE_RELSYM ?= -g

# Sets of compile flags for different build types
GLUCOSE_REL    ?= -std=c++11 -O3 -D NDEBUG
GLUCOSE_DEB    ?= -std=c++11 -O0 -D DEBUG 
GLUCOSE_PRF    ?= -std=c++11 -O3 -D NDEBUG
GLUCOSE_FPIC   ?= -fpic

# GNU Standard Install Prefix
prefix         ?= /usr/local

## Write Configuration  ###########################################################################

config:
	@( echo 'BUILD_DIR?=$(BUILD_DIR)'           ; \
	   echo 'GLUCOSE_RELSYM?=$(GLUCOSE_RELSYM)' ; \
	   echo 'GLUCOSE_REL?=$(GLUCOSE_REL)'       ; \
	   echo 'GLUCOSE_DEB?=$(GLUCOSE_DEB)'       ; \
	   echo 'GLUCOSE_PRF?=$(GLUCOSE_PRF)'       ; \
	   echo 'GLUCOSE_FPIC?=$(GLUCOSE_FPIC)'     ; \
	   echo 'prefix?=$(prefix)'                 ) > config.mk

## Configurable options end #######################################################################

INSTALL ?= install

# GNU Standard Install Variables
exec_prefix ?= $(prefix)
includedir  ?= $(prefix)/include
bindir      ?= $(exec_prefix)/bin
libdir      ?= $(exec_prefix)/lib
datarootdir ?= $(prefix)/share
mandir      ?= $(datarootdir)/man

# Target file names
GLUCOSE      = glucose#       Name of Glucose main executable.
GLUCOSE_CORE = glucose_core#  Name of simplified Glucose executable (only core solver support).
GLUCOSE_SLIB = lib$(GLUCOSE).a#  Name of Glucose static library.
GLUCOSE_DLIB = lib$(GLUCOSE).so# Name of Glucose shared library.

# Shared Library Version
SOMAJOR=2
SOMINOR=1
SORELEASE?=.0#   Declare empty to leave out from library file name.

GLUCOSE_CXXFLAGS = -std=c++11 -I. -D __STDC_LIMIT_MACROS -D __STDC_FORMAT_MACROS -Wall -Wno-parentheses -Wextra
GLUCOSE_LDFLAGS  = -Wall -lz

ECHO=@echo
ifeq ($(VERB),)
VERB=@
else
VERB=
endif



SRCS = $(wildcard core/*.cc) $(wildcard simp/*.cc) $(wildcard utils/*.cc)
HDRS = $(wildcard mtl/*.h) $(wildcard core/*.h) $(wildcard simp/*.h) $(wildcard utils/*.h)
OBJS = $(filter-out %Main.o, $(SRCS:.cc=.o))

r:	$(BUILD_DIR)/release/bin/$(GLUCOSE)
d:	$(BUILD_DIR)/debug/bin/$(GLUCOSE)
p:	$(BUILD_DIR)/profile/bin/$(GLUCOSE)
sh:	$(BUILD_DIR)/dynamic/bin/$(GLUCOSE)

cr:	$(BUILD_DIR)/release/bin/$(GLUCOSE_CORE)
cd:	$(BUILD_DIR)/debug/bin/$(GLUCOSE_CORE)
cp:	$(BUILD_DIR)/profile/bin/$(GLUCOSE_CORE)
csh:	$(BUILD_DIR)/dynamic/bin/$(GLUCOSE_CORE)

lr:	$(BUILD_DIR)/release/lib/$(GLUCOSE_SLIB)
ld:	$(BUILD_DIR)/debug/lib/$(GLUCOSE_SLIB)
lp:	$(BUILD_DIR)/profile/lib/$(GLUCOSE_SLIB)
lsh:	$(BUILD_DIR)/dynamic/lib/$(GLUCOSE_DLIB).$(SOMAJOR).$(SOMINOR)$(SORELEASE)

## Build-type Compile-flags:
$(BUILD_DIR)/release/%.o:			GLUCOSE_CXXFLAGS +=$(GLUCOSE_REL) $(GLUCOSE_RELSYM)
$(BUILD_DIR)/debug/%.o:				GLUCOSE_CXXFLAGS +=$(GLUCOSE_DEB) -g
$(BUILD_DIR)/profile/%.o:			GLUCOSE_CXXFLAGS +=$(GLUCOSE_PRF) -pg
$(BUILD_DIR)/dynamic/%.o:			GLUCOSE_CXXFLAGS +=$(GLUCOSE_REL) $(GLUCOSE_FPIC)

## Build-type Link-flags:
$(BUILD_DIR)/profile/bin/$(GLUCOSE):		GLUCOSE_LDFLAGS += -pg
$(BUILD_DIR)/release/bin/$(GLUCOSE):		GLUCOSE_LDFLAGS += --static $(GLUCOSE_RELSYM)
$(BUILD_DIR)/profile/bin/$(GLUCOSE_CORE):	GLUCOSE_LDFLAGS += -pg
$(BUILD_DIR)/release/bin/$(GLUCOSE_CORE):	GLUCOSE_LDFLAGS += --static $(GLUCOSE_RELSYM)

## Executable dependencies
$(BUILD_DIR)/release/bin/$(GLUCOSE):	 	$(BUILD_DIR)/release/simp/Main.o $(BUILD_DIR)/release/lib/$(GLUCOSE_SLIB)
$(BUILD_DIR)/debug/bin/$(GLUCOSE):	 	$(BUILD_DIR)/debug/simp/Main.o $(BUILD_DIR)/debug/lib/$(GLUCOSE_SLIB)
$(BUILD_DIR)/profile/bin/$(GLUCOSE):	 	$(BUILD_DIR)/profile/simp/Main.o $(BUILD_DIR)/profile/lib/$(GLUCOSE_SLIB)
# need the main-file be compiled with fpic?
$(BUILD_DIR)/dynamic/bin/$(GLUCOSE):	 	$(BUILD_DIR)/dynamic/simp/Main.o $(BUILD_DIR)/dynamic/lib/$(GLUCOSE_DLIB)

## Executable dependencies (core-version)
$(BUILD_DIR)/release/bin/$(GLUCOSE_CORE):	$(BUILD_DIR)/release/core/Main.o $(BUILD_DIR)/release/lib/$(GLUCOSE_SLIB)
$(BUILD_DIR)/debug/bin/$(GLUCOSE_CORE):	 	$(BUILD_DIR)/debug/core/Main.o $(BUILD_DIR)/debug/lib/$(GLUCOSE_SLIB)
$(BUILD_DIR)/profile/bin/$(GLUCOSE_CORE):	$(BUILD_DIR)/profile/core/Main.o $(BUILD_DIR)/profile/lib/$(GLUCOSE_SLIB)
# need the main-file be compiled with fpic?
$(BUILD_DIR)/dynamic/bin/$(GLUCOSE_CORE): 	$(BUILD_DIR)/dynamic/core/Main.o $(BUILD_DIR)/dynamic/lib/$(GLUCOSE_DLIB)

## Library dependencies
$(BUILD_DIR)/release/lib/$(GLUCOSE_SLIB):	$(foreach o,$(OBJS),$(BUILD_DIR)/release/$(o))
$(BUILD_DIR)/debug/lib/$(GLUCOSE_SLIB):		$(foreach o,$(OBJS),$(BUILD_DIR)/debug/$(o))
$(BUILD_DIR)/profile/lib/$(GLUCOSE_SLIB):	$(foreach o,$(OBJS),$(BUILD_DIR)/profile/$(o))
$(BUILD_DIR)/dynamic/lib/$(GLUCOSE_DLIB).$(SOMAJOR).$(SOMINOR)$(SORELEASE)\
 $(BUILD_DIR)/dynamic/lib/$(GLUCOSE_DLIB).$(SOMAJOR)\
 $(BUILD_DIR)/dynamic/lib/$(GLUCOSE_DLIB):	$(foreach o,$(OBJS),$(BUILD_DIR)/dynamic/$(o))

## Compile rules (these should be unified, buit I have not yet found a way which works in GNU Make)
$(BUILD_DIR)/release/%.o:	%.cc
	$(ECHO) Compiling: $@
	$(VERB) mkdir -p $(dir $@)
	$(VERB) $(CXX) $(GLUCOSE_CXXFLAGS) $(CXXFLAGS) -c -o $@ $< -MMD -MF $(BUILD_DIR)/release/$*.d

$(BUILD_DIR)/profile/%.o:	%.cc
	$(ECHO) Compiling: $@
	$(VERB) mkdir -p $(dir $@)
	$(VERB) $(CXX) $(GLUCOSE_CXXFLAGS) $(CXXFLAGS) -c -o $@ $< -MMD -MF $(BUILD_DIR)/profile/$*.d

$(BUILD_DIR)/debug/%.o:	%.cc
	$(ECHO) Compiling: $@
	$(VERB) mkdir -p $(dir $@)
	$(VERB) $(CXX) $(GLUCOSE_CXXFLAGS) $(CXXFLAGS) -c -o $@ $< -MMD -MF $(BUILD_DIR)/debug/$*.d

$(BUILD_DIR)/dynamic/%.o:	%.cc
	$(ECHO) Compiling: $@
	$(VERB) mkdir -p $(dir $@)
	$(VERB) $(CXX) $(GLUCOSE_CXXFLAGS) $(CXXFLAGS) -c -o $@ $< -MMD -MF $(BUILD_DIR)/dynamic/$*.d

## Linking rule
$(BUILD_DIR)/release/bin/$(GLUCOSE) $(BUILD_DIR)/debug/bin/$(GLUCOSE) $(BUILD_DIR)/profile/bin/$(GLUCOSE) $(BUILD_DIR)/dynamic/bin/$(GLUCOSE)\
$(BUILD_DIR)/release/bin/$(GLUCOSE_CORE) $(BUILD_DIR)/debug/bin/$(GLUCOSE_CORE) $(BUILD_DIR)/profile/bin/$(GLUCOSE_CORE) $(BUILD_DIR)/dynamic/bin/$(GLUCOSE_CORE):
	$(ECHO) Linking Binary: $@
	$(VERB) mkdir -p $(dir $@)
	$(VERB) $(CXX) $^ $(GLUCOSE_LDFLAGS) $(LDFLAGS) -o $@

## Static Library rule
%/lib/$(GLUCOSE_SLIB):
	$(ECHO) Linking Static Library: $@
	$(VERB) mkdir -p $(dir $@)
	$(VERB) $(AR) -rcs $@ $^

## Shared Library rule
$(BUILD_DIR)/dynamic/lib/$(GLUCOSE_DLIB).$(SOMAJOR).$(SOMINOR)$(SORELEASE)\
 $(BUILD_DIR)/dynamic/lib/$(GLUCOSE_DLIB).$(SOMAJOR)\
 $(BUILD_DIR)/dynamic/lib/$(GLUCOSE_DLIB):
	$(ECHO) Linking Shared Library: $@
	$(VERB) mkdir -p $(dir $@)
	$(VERB) $(CXX) $(GLUCOSE_LDFLAGS) $(LDFLAGS) -o $@ -shared -Wl,-soname,$(GLUCOSE_DLIB).$(SOMAJOR) $^
	$(VERB) ln -sf $(GLUCOSE_DLIB).$(SOMAJOR).$(SOMINOR)$(SORELEASE) $(BUILD_DIR)/dynamic/lib/$(GLUCOSE_DLIB).$(SOMAJOR)
	$(VERB) ln -sf $(GLUCOSE_DLIB).$(SOMAJOR) $(BUILD_DIR)/dynamic/lib/$(GLUCOSE_DLIB)

install:	install-headers install-lib install-bin
install-debug:	install-headers install-lib-debug

install-headers:
#       Create directories
	$(INSTALL) -d $(DESTDIR)$(includedir)/glucose
	for dir in mtl utils core simp; do \
	  $(INSTALL) -d $(DESTDIR)$(includedir)/$$dir ; \
	done
#       Install headers
	for h in $(HDRS) ; do \
	  $(INSTALL) -m 644 $$h $(DESTDIR)$(includedir)/$$h ; \
	done

install-lib-debug: $(BUILD_DIR)/debug/lib/$(GLUCOSE_SLIB)
	$(INSTALL) -d $(DESTDIR)$(libdir)
	$(INSTALL) -m 644 $(BUILD_DIR)/debug/lib/$(GLUCOSE_SLIB) $(DESTDIR)$(libdir)

install-lib: $(BUILD_DIR)/release/lib/$(GLUCOSE_SLIB) $(BUILD_DIR)/dynamic/lib/$(GLUCOSE_DLIB).$(SOMAJOR).$(SOMINOR)$(SORELEASE)
	$(INSTALL) -d $(DESTDIR)$(libdir)
	$(INSTALL) -m 644 $(BUILD_DIR)/dynamic/lib/$(GLUCOSE_DLIB).$(SOMAJOR).$(SOMINOR)$(SORELEASE) $(DESTDIR)$(libdir)
	ln -sf $(GLUCOSE_DLIB).$(SOMAJOR).$(SOMINOR)$(SORELEASE) $(DESTDIR)$(libdir)/$(GLUCOSE_DLIB).$(SOMAJOR)
	ln -sf $(GLUCOSE_DLIB).$(SOMAJOR) $(DESTDIR)$(libdir)/$(GLUCOSE_DLIB)
	$(INSTALL) -m 644 $(BUILD_DIR)/release/lib/$(GLUCOSE_SLIB) $(DESTDIR)$(libdir)

install-bin: $(BUILD_DIR)/dynamic/bin/$(GLUCOSE)
	$(INSTALL) -d $(DESTDIR)$(bindir)
	$(INSTALL) -m 755 $(BUILD_DIR)/dynamic/bin/$(GLUCOSE) $(DESTDIR)$(bindir)

clean:
	rm -f $(foreach t, release debug profile dynamic, $(foreach o, $(SRCS:.cc=.o), $(BUILD_DIR)/$t/$o)) \
          $(foreach t, release debug profile dynamic, $(foreach d, $(SRCS:.cc=.d), $(BUILD_DIR)/$t/$d)) \
	  $(foreach t, release debug profile dynamic, $(BUILD_DIR)/$t/bin/$(GLUCOSE_CORE) $(BUILD_DIR)/$t/bin/$(GLUCOSE)) \
	  $(foreach t, release debug profile, $(BUILD_DIR)/$t/lib/$(GLUCOSE_SLIB)) \
	  $(BUILD_DIR)/dynamic/lib/$(GLUCOSE_DLIB).$(SOMAJOR).$(SOMINOR)$(SORELEASE)\
	  $(BUILD_DIR)/dynamic/lib/$(GLUCOSE_DLIB).$(SOMAJOR)\
	  $(BUILD_DIR)/dynamic/lib/$(GLUCOSE_DLIB)

distclean:	clean
	rm -f config.mk

## Include generated dependencies
-include $(foreach s, $(SRCS:.cc=.d), $(BUILD_DIR)/release/$s)
-include $(foreach s, $(SRCS:.cc=.d), $(BUILD_DIR)/debug/$s)
-include $(foreach s, $(SRCS:.cc=.d), $(BUILD_DIR)/profile/$s)
-include $(foreach s, $(SRCS:.cc=.d), $(BUILD_DIR)/dynamic/$s)
