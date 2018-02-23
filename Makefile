.PHONY: all parser visitors compiler clean

all: parser visitors compiler

parser:
	java -jar jar/jtb132di.jar -te grammar/minijava.jj

visitors:
	java -jar jar/javacc5.jar grammar/minijava-jtb.jj

compiler:
	javac src/*.java

clean:
	rm -f *.class *~
	rm -rf syntaxtree visitor
