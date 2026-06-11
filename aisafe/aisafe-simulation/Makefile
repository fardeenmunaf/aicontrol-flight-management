CC = gcc
CFLAGS = -Wall -Wextra -g -Isrc/main/c -Isrc/main/c/libs -pthread
LDFLAGS = -lm -pthread -lrt

all: simulation_engine report_generator flight_plan_validator

simulation_engine: simulation_engine.o physics.o cJSON.o json_parser.o flight_simulation.o position_tracker.o
	$(CC) $(CFLAGS) simulation_engine.o physics.o cJSON.o json_parser.o flight_simulation.o position_tracker.o -o simulation_engine $(LDFLAGS)

report_generator: report_generator.o
	$(CC) $(CFLAGS) report_generator.o -o report_generator $(LDFLAGS)

flight_plan_validator: src/main/c/flight_plan_validator.c src/main/c/libs/cJSON.c
	$(CC) $(CFLAGS) src/main/c/flight_plan_validator.c src/main/c/libs/cJSON.c -o flight_plan_validator -lm

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

position_tracker.o: src/main/c/position_tracker.c
	$(CC) $(CFLAGS) -c src/main/c/position_tracker.c

report_generator.o: src/main/c/report_generator.c
	$(CC) $(CFLAGS) -c src/main/c/report_generator.c

test_physics: src/main/c/test_physics.c src/main/c/physics.c
	$(CC) $(CFLAGS) src/main/c/test_physics.c src/main/c/physics.c -o test_physics $(LDFLAGS)

test_position_tracker: src/main/c/test_position_tracker.c src/main/c/position_tracker.c
	$(CC) $(CFLAGS) src/main/c/test_position_tracker.c src/main/c/position_tracker.c -o test_position_tracker $(LDFLAGS)

run: all
	./simulation_engine

run_log: all
	./simulation_engine > simulation_output.txt 2>&1
	@echo "Simulation complete. Output written to simulation_output.txt"

test: test_physics test_position_tracker
	./test_physics
	./test_position_tracker

clean:
	rm -f *.o simulation_engine report_generator flight_plan_validator test_physics test_position_tracker simulation_report_*.json

.PHONY: all run test clean
