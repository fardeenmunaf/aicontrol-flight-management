CC = gcc
CFLAGS = -Wall -Wextra -g -Isrc/main/c -Isrc/main/c/libs
LDFLAGS = -lm -lpthread

UNAME_S := $(shell uname -s)
ifeq ($(UNAME_S),Linux)
    LDFLAGS += -lrt
endif

SRCS = src/main/c/simulation_engine.c \
       src/main/c/physics.c \
       src/main/c/libs/cJSON.c \
       src/main/c/json_parser.c \
       src/main/c/flight_simulation.c

OBJS = $(SRCS:.c=.o)

TARGET = simulation_engine

all: $(TARGET)

$(TARGET): $(OBJS)
	$(CC) $(CFLAGS) -o $@ $^ $(LDFLAGS)

%.o: %.c
	$(CC) $(CFLAGS) -c -o $@ $<

clean:
	rm -f $(OBJS) $(TARGET)

.PHONY: all clean
