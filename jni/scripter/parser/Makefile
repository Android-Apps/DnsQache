# source files.
SRC = format.c parse.c token.c

OBJ = $(SRC:.c=.o)

OUT = libparser.a

# include directories
INCLUDES = -I.
#INCLUDES = -I. -I../include/ -I/usr/local/include

# C++ compiler flags (-g -O2 -Wall)
CFLAGS = -g

# compiler
CC = gcc

# library paths
#LIBS = -L../ -L/usr/local/lib -lm

# compile flags
LDFLAGS = -g

.SUFFIXES: .c

default: $(OUT)

.c.o:
	$(CC) $(INCLUDES) $(CFLAGS) -c $< -o $@

$(OUT): $(OBJ)
	ar rcs $(OUT) $(OBJ)

#depend: dep
#
#dep:
#	makedepend -- $(CFLAGS) -- $(INCLUDES) $(SRC)

clean:
	rm -f $(OBJ) $(OUT) Makefile.bak 

$(OBJ): parser.h stdapi.h
