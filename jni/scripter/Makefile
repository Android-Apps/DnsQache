# subdirectories
DIRS=parser

# source files.
SRC = main.c

OBJ = $(SRC:.c=.o)

OUT = scripter

# include directories
INCLUDES = -I.
#INCLUDES = -I. -I../include/ -I/usr/local/include

# C++ compiler flags (-g -O2 -Wall)
CFLAGS = -g

# compiler
CC = gcc

# library paths
LIBS = parser/libparser.a
#LIBS = -L../ -L/usr/local/lib -lm

# compile flags
LDFLAGS = -g

.SUFFIXES: .c

default: subdirs $(OUT)

subdirs: $(DIRS)
	make -C $<

.c.o:
	$(CC) $(INCLUDES) $(CFLAGS) -c $< -o $@

$(OUT): $(OBJ)
	gcc -o $@ $< $(LIBS)

clean:
	rm -f $(OBJ) $(OUT)

main.o: parser/stdapi.h parser/parser.h
scripter: parser/libparser.a
