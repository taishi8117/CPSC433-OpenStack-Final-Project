# Makefile

JAVAC = javac
JAVA = java
JAVA_ARGS = project.OpenStack -config ../openstack.config
FLAGS = -nowarn -g
JAVA_FILES = $(wildcard src/lib/*.java) $(wildcard src/object/*.java) $(wildcard src/project/*.java)
CLASSPATH = -cp "src/project:src/object:src/lib:.:src/lib/json-simple-1.1.1.jar:src/lib/http-20070405.jar:src/lib/commons-io-2.5/commons-io-2.5-javadoc.jar:src/lib/commons-io-2.5/commons-io-2.5.jar"
CLASSPATH2 = -cp "project:object:lib:.:lib/json-simple-1.1.1.jar:lib/http-20070405.jar:lib/commons-io-2.5/commons-io-2.5-javadoc.jar:lib/commons-io-2.5/commons-io-2.5.jar"

.PHONY = all clean

all: $(JAVA_FILES)
	@echo 'Making all...'
	@$(JAVAC) $(FLAGS) $(CLASSPATH) $?

clean:
	rm -f $(JAVA_FILES:.java=.class)

run: ./run_Openstack.sh
