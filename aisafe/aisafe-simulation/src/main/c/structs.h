#ifndef STRUCTS_H
#define STRUCTS_H

#include <sys/types.h>
#include <unistd.h>
#include <stdbool.h>
#include <time.h>
#include <pthread.h>
#include <semaphore.h>

#define MAX_FLIGHTS 100
#define MAX_WAYPOINTS 50
#define CALLSIGN_LEN 16

// Represents a 3D position at a specific timestamp
typedef struct {
    double latitude;
    double longitude;
    double altitude;
    double timestamp;
} Position;

// Represents velocity and heading
typedef struct {
    double speed;           // True Air Speed
    double heading;         // Direction in degrees
    double vertical_speed;  // Rate of climb/descent
} Velocity;

// Represents an aircraft's physical properties for the physics engine
typedef struct {
    double mass;
    double wing_area;
    double max_thrust;
    double cd0;             // Zero-lift drag coefficient
    double aspect_ratio;
    double oswald_efficiency;
} AircraftProperties;

// Represents a flight plan waypoint
typedef struct {
    Position pos;
    double target_speed;
} Waypoint;

// Represents a flight plan
typedef struct {
    Waypoint waypoints[MAX_WAYPOINTS];
    int num_waypoints;
    int current_waypoint_index;
} FlightPlan;

// Represents a flight entity
typedef struct {
    int id;
    char callsign[CALLSIGN_LEN];
    pid_t process_id;       // PID of the child process simulating this flight
    
    Position current_pos;
    Velocity velocity;
    AircraftProperties properties;
    FlightPlan flight_plan;
    
    bool is_active;
    bool violation_detected;
} Flight;

// Inter-process communication structures (Pipes)
typedef enum {
    MSG_POSITION_UPDATE,
    MSG_VIOLATION_NOTIFY,
    MSG_TERMINATE,
    MSG_SYNC_STEP // Used for step-by-step pipe synchronization if not using semaphores
} MessageType;

typedef struct {
    MessageType type;
    int flight_id;
    Position new_pos;
} PipeMessage;

// Configuration for the simulation
typedef struct {
    double start_time;
    double end_time;
    double time_step;
    double safety_distance_threshold;
    int max_violations_threshold;
} SimulationConfig;

// Structure for the Shared Memory block (US105, US106, US107, US108)
typedef struct {
    SimulationConfig config;
    double current_time;
    
    int num_flights;
    Flight flights[MAX_FLIGHTS];
    
    int total_violations_detected;
    bool termination_requested;
    
    // POSIX Synchronization primitives for shared memory
    pthread_mutex_t data_mutex;
    pthread_cond_t violation_cond; // For notifying report thread (US107)
    
    // Semaphores for step-by-step synchronization (US108)
    sem_t step_semaphores[MAX_FLIGHTS];
    sem_t main_semaphore;
} SharedMemoryData;

#endif /* STRUCTS_H */
