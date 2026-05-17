CC = gcc
CFLAGS = -Wall -Wextra -g -Isrc/main/c -Isrc/main/c/libs -pthread

# -lrt é necessário no Linux (shm_open, sem_open); no macOS essas funções
# estão na libc padrão, por isso não existe a biblioteca separada.
UNAME := $(shell uname)
ifeq ($(UNAME), Darwin)
    LDFLAGS = -lm -pthread
else
    LDFLAGS = -lm -lrt -pthread
endif

simulation_engine: simulation_engine.o physics.o cJSON.o json_parser.o flight_simulation.o position_tracker.o
	$(CC) $(CFLAGS) simulation_engine.o physics.o cJSON.o json_parser.o flight_simulation.o position_tracker.o -o simulation_engine $(LDFLAGS)

report_generator: report_generator.o
	$(CC) $(CFLAGS) report_generator.o -o report_generator $(LDFLAGS)

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

run: simulation_engine
	./simulation_engine > simulation_output.txt 2>&1
	@echo "Simulation complete. Output written to simulation_output.txt"

run_with_report: simulation_engine report_generator
	nohup ./simulation_engine > simulation_output.txt 2>&1 &
	sleep 1
	./report_generator
	@echo "Simulation and report generation complete"

test: test_physics test_position_tracker
	./test_physics
	./test_position_tracker

clean:
	rm -f *.o simulation_engine report_generator test_physics test_position_tracker

.PHONY: run run_with_report test clean
