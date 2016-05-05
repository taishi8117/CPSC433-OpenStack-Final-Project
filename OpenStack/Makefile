# Makefile for Fishnet

JAVAC = javac
FLAGS = -nowarn -g
JAVA_FILES = $(wildcard src/lib/*.java) $(wildcard src/object/*.java) $(wildcard src/project/*.java)

.PHONY = all clean

all: $(JAVA_FILES)
	@echo 'Making all...'
	@$(JAVAC) $(FLAGS) $?

clean:
	rm -f $(JAVA_FILES:.java=.class)

run:
	java src/project/OpenStack
