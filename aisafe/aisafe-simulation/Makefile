CC = gcc
CFLAGS = -Wall -Wextra -g -Isrc/main/c -Isrc/main/c/libs
LDFLAGS = -lm -lrt

simulation_engine: simulation_engine.o physics.o cJSON.o json_parser.o flight_simulation.o
	$(CC) $(CFLAGS) simulation_engine.o physics.o cJSON.o json_parser.o flight_simulation.o -o simulation_engine $(LDFLAGS)

simulation_engine.o: src/main/c/simulation_engine.c
	$(CC) $(CFLAGS) -c src/main/c/simulation_engine.c

physics.o: src/main/c/physics.c
	$(CC) $(CFLAGS) -c src/main/c/physics.c

cJSON.o: src/main/c/libs/cJSON.c
	$(CC) $(CFLAGS) -c src/main/c/libs/cJSON.c

json_parser.o: src/main/c/json_parser.c
	$(CC) $(CFLAGS) -c src/main/c/json_parser.c

flight_simulation.o: src/main/c/flight_simulation.c
	$(CC) $(CFLAGS) -c src/main/c/flight_simulation.c

test_physics: src/main/c/test_physics.c src/main/c/physics.c
	$(CC) $(CFLAGS) src/main/c/test_physics.c src/main/c/physics.c -o test_physics $(LDFLAGS)

run: simulation_engine
	./simulation_engine > simulation_output.txt 2>&1
	@echo "Simulation complete. Output written to simulation_output.txt"

test: test_physics
	./test_physics

clean:
	rm -f *.o simulation_engine test_physics

.PHONY: run test clean
