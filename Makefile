# define a makefile variable for the java compiler
CC = javac

SRC = Crawler.java

# this target entry builds the Client.class
all:
	$(CC) $(SRC)

# To start over from scratch, type 'make clean'.
# Removes all .class files, so that the next make rebuilds them
clean:
	$(RM) *.class
