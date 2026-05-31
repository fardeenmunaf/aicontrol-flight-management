#ifndef STRUCTS_H
#define STRUCTS_H

#include <sys/types.h>
#include <unistd.h>
#include <stdbool.h>
#include <time.h>

#define MAX_FLIGHTS   100
#define MAX_WAYPOINTS  50
#define MAX_LEGS       10
#define CALLSIGN_LEN   16

#define FUEL_DENSITY_KG_PER_L  0.804

// sentinel: arrival time not present in JSON
#define ARRIVAL_OFFSET_NOT_SET (-1.0)

typedef struct {
    double latitude;    // degrees, –90 to +90
    double longitude;   // degrees, –180 to +180
    double altitude;    // metres above MSL
    double timestamp;   // simulation clock (seconds)
} Position;

typedef struct {
    double speed;           // TAS in m/s
    double heading;         // degrees clockwise from north
    double vertical_speed;  // m/s, positive = climbing
} Velocity;

typedef struct {
    double empty_weight;
    double wing_area;                   // m²
    double aspect_ratio;
    double oswald_efficiency;           // 0–1
    double cd0;
    int    num_engines;
    double thrust_sea_level_stopped;    // N per engine at Mach 0
    double thrust_sea_level_cruise;     // N per engine at Mach 0.9
    double thrust_lapse_rate;
    double tsfc;                        // N/N/s
} AircraftProperties;

typedef enum {
    PHASE_GATE,
    PHASE_CLIMB,
    PHASE_CRUISE,
    PHASE_DESCEND
} FlightPhase;

typedef struct {
    Position pos;
    double target_speed;
    FlightPhase segment_phase;
} Waypoint;

typedef struct {
    Waypoint waypoints[MAX_WAYPOINTS];
    int num_waypoints;
    int current_waypoint_index;
} FlightPlan;

typedef struct {
    double departure_offset_s;
    double scheduled_arrival_offset_s;  // ARRIVAL_OFFSET_NOT_SET if not in JSON
    double fuel_mass;                   // kg
    double payload;                     // kg
    int    waypoint_start_index;
    int    waypoint_end_index;
} Leg;

typedef struct {
    double dist_to_dest;
    double cruise_alt;
    double temperature;     // K
    double pressure;        // Pa
    double rho;             // kg/m³
    double speed_of_sound;  // m/s
    double ias_kts;
    double mach;
    double tas;             // m/s
    double cl;
    double cd;
    double lift;            // N
    double drag;            // N
    double thrust_per_engine;
    double total_thrust;
    double climb_rate;      // m/s
    double fuel_burned;     // kg
    double horiz_dist;      // m
    FlightPhase phase;
} StepData;

typedef struct {
    int  id;
    char callsign[CALLSIGN_LEN];
    pid_t process_id;

    Position current_pos;
    Velocity velocity;
    AircraftProperties properties;
    FlightPlan flight_plan;

    double payload;
    double initial_fuel_mass;
    double initial_mass;
    double current_mass;

    FlightPhase current_phase;

    double departure_offset_s;

    Leg  legs[MAX_LEGS];
    int  num_legs;
    int  current_leg;
    bool leg_just_completed;

    bool is_active;
    bool violation_detected;
    bool fuel_exhaustion_reported;
} Flight;

#define MAX_HISTORY 200

typedef struct {
    Position entries[MAX_HISTORY];
    int      count;
    int      next;
} PositionHistory;

// Command sent from parent to child at the start of each step.
typedef struct {
    double current_time;
    bool   terminate;
} StepCommand;

// Full dynamic flight state sent from child to parent after each step.
// Carries everything the parent needs to update its local Flight copy.
typedef struct {
    int         flight_id;
    Position    current_pos;
    Velocity    velocity;
    FlightPhase current_phase;
    double      current_mass;
    double      initial_mass;
    double      initial_fuel_mass;
    double      payload;
    double      departure_offset_s;
    int         current_leg;
    int         current_waypoint_index;
    bool        is_active;
    bool        leg_just_completed;
    bool        fuel_exhaustion_reported;
    StepData    step;
} PipeMessage;

typedef struct {
    double start_time;
    double end_time;
    double time_step;
    double safety_distance_threshold;   // metres
    int    max_violations_threshold;
    double sim_start_seconds;           // seconds since midnight
} SimulationConfig;

typedef struct {
    double timestamp;
    int flight_a_id;
    int flight_b_id;
    Position pos_a;
    Position pos_b;
    Velocity vel_a;
    Velocity vel_b;
    double distance;
} ViolationEvent;

#define MAX_RECORDED_VIOLATIONS 1000

typedef struct {
    char callsign[CALLSIGN_LEN];
    bool completed_successfully;
    bool had_violation;
    double fuel_remaining;
    double total_distance;
    int num_legs_completed;
} FlightStatus;

typedef struct {
    double sim_start_time;
    double sim_end_time;
    double wall_clock_start;

    int total_flights;
    int total_violations_recorded;

    ViolationEvent violations[MAX_RECORDED_VIOLATIONS];
    FlightStatus   flight_statuses[MAX_FLIGHTS];

    bool report_ready;
} SimulationReport;

typedef struct {
    SimulationConfig config;
    double current_time;

    int    num_flights;
    Flight flights[MAX_FLIGHTS];

    int  total_violations_detected;
    bool termination_requested;

    SimulationReport report;
} SimulationData;

#endif /* STRUCTS_H */
