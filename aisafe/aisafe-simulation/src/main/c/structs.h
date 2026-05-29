#ifndef STRUCTS_H
#define STRUCTS_H

// Standard POSIX headers needed for the types used in this file

#include <sys/types.h>  // pid_t — the type for a process ID
#include <unistd.h>     // gives us getpid(), fork(), etc.
#include <stdbool.h>    // bool, true, false (not built into C by default)
#include <time.h>       // time_t, available if needed

// ── Limits ────────────────────────────────────────────────────────────────────

#define MAX_FLIGHTS   100   // maximum number of simultaneous flights the sim supports
#define MAX_WAYPOINTS  50   // maximum route points per flight plan
#define MAX_LEGS       10   // maximum number of sequential legs per flight
#define CALLSIGN_LEN   16   // characters for a flight ID, e.g. "TP1234\0"

// Jet-A fuel density used to convert litres to kg when the JSON stores volume

#define FUEL_DENSITY_KG_PER_L  0.804   // 1 litre of Jet-A ≈ 0.804 kg

// Leg scheduled_arrival_offset_s sentinel: arrival not in JSON

#define ARRIVAL_OFFSET_NOT_SET (-1.0)  // scheduled_arrival_offset_s sentinel: arrival not in JSON

// ── Basic physical types ───────────────────────────────────────────────────────

// A 3-D position in space at a specific simulation time.
// All processes read and write this struct inside shared memory.

typedef struct {
    double latitude;    // degrees, –90 (south pole) to +90 (north pole)
    double longitude;   // degrees, –180 to +180
    double altitude;    // metres above sea level (MSL)
    double timestamp;   // simulation clock value (seconds) when this was recorded
} Position;

// The aircraft's movement vector at a given instant.

typedef struct {
    double speed;           // True Air Speed (TAS) in m/s — actual speed through the air
    double heading;         // direction the nose is pointing, degrees clockwise from north
    double vertical_speed;  // m/s — positive = climbing, negative = descending
} Velocity;

// ── Aircraft physical properties (loaded from JSON, never changed during sim) ──

// Every number the physics engine needs to compute forces on the aircraft.
// These come from the "aircraft" and "engine" sections of the flight plan JSON.

typedef struct {
    double empty_weight;                // kg — aircraft with zero fuel, zero payload
    double wing_area;                   // m² — total lifting surface area
    double aspect_ratio;                // wingspan² / wing_area — describes wing shape
                                        //   high AR (glider) = efficient; low AR (fighter) = draggy
    double oswald_efficiency;           // 'e' factor: how close to an ideal elliptic wing (0–1)
    double cd0;                         // zero-lift drag coefficient: fuselage, nacelles, etc.
    int    num_engines;
    double thrust_sea_level_stopped;    // N per engine at Mach 0 (static thrust)
    double thrust_sea_level_cruise;     // N per engine at Mach 0.9 (used to derive thrust slope)
    double thrust_lapse_rate;           // exponent for altitude correction: thinner air = less thrust
    double tsfc;                        // Thrust Specific Fuel Consumption (N/N/s)
                                        //   how many kg of fuel burned per Newton of thrust per second
} AircraftProperties;

// ── Flight phase ──────────────────────────────────────────────────────────────

typedef enum {
    PHASE_GATE,     // aircraft at gate or between legs (not airborne)
    PHASE_CLIMB,    // aircraft climbing to cruise altitude
    PHASE_CRUISE,   // aircraft level at cruise altitude
    PHASE_DESCEND   // aircraft descending toward destination
} FlightPhase;

// ── Flight plan types ─────────────────────────────────────────────────────────

// A single point along the route the aircraft must pass through.

typedef struct {
    Position pos;                // lat/lon/alt of this waypoint
    double target_speed;         // desired IAS (knots) at this waypoint
    FlightPhase segment_phase;   // phase when flying toward this waypoint
} Waypoint;

// The full sequence of waypoints from departure to destination.

typedef struct {
    Waypoint waypoints[MAX_WAYPOINTS];
    int num_waypoints;              // how many entries in the array are valid
    int current_waypoint_index;     // which waypoint we are currently heading toward
} FlightPlan;

// ── Per-leg data ──────────────────────────────────────────────────────────────
// One entry per leg. Populated by the JSON parser before fork().
// Read-only during simulation — only the child reads these to reset state.

typedef struct {
    double departure_offset_s;       // seconds from sim t=0 when this leg departs
    double scheduled_arrival_offset_s; // seconds from sim t=0; ARRIVAL_OFFSET_NOT_SET if arrival not in JSON (departure always required)
    double fuel_mass;                // kg of fuel loaded for this leg
    double payload;                  // kg of passengers + cargo for this leg
    int    waypoint_start_index;     // index into flight_plan.waypoints[] of this leg's first navigation target
    int    waypoint_end_index;       // index of this leg's destination waypoint (inclusive)
} Leg;

// ── Physics step data — all 25 intermediate values for one time-step ──────────
//
// compute_physics_step() fills one of these per step.
// It is then sent to the parent via the pipe so the parent can print the
// full per-step table without duplicating any formula logic.

typedef struct {
    double dist_to_dest;        // m — great-circle distance to destination at step start
    double cruise_alt;          // m — target cruise altitude for this route length
    double temperature;         // K — ISA temperature at current altitude
    double pressure;            // Pa — ISA pressure at current altitude
    double rho;                 // kg/m³ — ISA air density at current altitude
    double speed_of_sound;      // m/s — ISA speed of sound at current altitude
    double ias_kts;             // kt — indicated airspeed target for this phase
    double mach;                // — Mach number derived from IAS + density
    double tas;                 // m/s — True Air Speed = Mach × speed_of_sound
    double cl;                  // — lift coefficient
    double cd;                  // — drag coefficient
    double lift;                // N — total lift force
    double drag;                // N — total drag force
    double thrust_per_engine;   // N — thrust from one engine (altitude+Mach corrected)
    double total_thrust;        // N — thrust from all engines (adjusted for flight phase)
    double climb_rate;          // m/s — vertical speed (positive = climb, negative = descent)
    double fuel_burned;         // kg — fuel consumed this step
    double horiz_dist;          // m — horizontal ground distance covered this step
    FlightPhase phase;          // which phase the aircraft is in this step
} StepData;

// ── Core flight entity ────────────────────────────────────────────────────────

// One flight = one child process + one slot in shared memory.
// The child process writes into this struct every simulation step;
// the parent process reads it to detect violations and log status.

typedef struct {
    int  id;                          // sequential index (1-based)
    char callsign[CALLSIGN_LEN];      // human-readable ID, e.g. "TP1234"
    pid_t process_id;                 // PID of the child process simulating this flight

    Position current_pos;             // updated by child every step — lives in shared memory
    Velocity velocity;                // updated by child every step
    AircraftProperties properties;    // constant after loading
    FlightPlan flight_plan;           // constant after loading

    // Mass budget (all in kg)
    double payload;             // passengers + cargo for this leg
    double initial_fuel_mass;   // fuel loaded at the departure airport
    double initial_mass;        // = empty_weight + payload + initial_fuel_mass
    double current_mass;        // decreases every step as fuel burns

    FlightPhase current_phase;  // updated each step — parent reads to detect transitions

    double departure_offset_s;  // seconds from sim t=0 when this flight departs; child waits until current_time >= this

    Leg  legs[MAX_LEGS];     // per-leg data array; legs[0] = first leg
    int  num_legs;           // total legs; 1 for a simple A→B flight
    int  current_leg;        // index into legs[], starts at 0, incremented on leg completion
    bool leg_just_completed; // child sets true when a leg ends; parent reads and clears it

    bool is_active;                  // true while flying OR waiting to depart; child sets false when it lands
    bool violation_detected;         // true if this flight ever had any violation
    bool fuel_exhaustion_reported;   // true once the parent has counted a fuel-exhaustion violation
} Flight;

// ── Position history  ──────────────────────────────────────────────
//
// The parent process accumulates every validated position update it receives
// from each flight process through the pipe.  When the buffer is full, the
// oldest entry is overwritten (ring/circular buffer).

#define MAX_HISTORY 200

typedef struct {
    Position entries[MAX_HISTORY]; // ring buffer of past positions
    int      count;                // number of valid entries (capped at MAX_HISTORY)
    int      next;                 // index where the next write will go
} PositionHistory;

// ── Pipe (IPC) message types ──────────────────────────────────────────────────

// Every message that a child sends to the parent through the pipe has this type tag.
// Using an enum (not a raw int) makes the code self-documenting and prevents
// accidentally mixing up message types.

typedef enum {
    MSG_POSITION_UPDATE,    // child finished a physics step — new position follows
    MSG_VIOLATION_NOTIFY,   // (reserved for future use)
    MSG_TERMINATE,          // (reserved for future use)
    MSG_SYNC_STEP           // (reserved — alternative step sync via pipe)
} MessageType;

// The actual struct written into the pipe.
// Fixed size is important: write() and read() work on exact byte counts,
// so the struct must not contain pointers or variable-length data.
// step carries all 25 intermediate physics values so the parent can print
// the full per-step table without recomputing anything.

typedef struct {
    MessageType type;    // what kind of message this is
    int flight_id;       // which flight sent this message
    Position new_pos;    // the position after the completed step
    StepData step;       // all 25 intermediate physics values for the UI table
} PipeMessage;

// ── Simulation configuration ──────────────────────────────────────────────────

// Loaded from simulation_settings.json before any process is forked.
// All fields are read-only after loading.

typedef struct {
    double start_time;                  // simulation clock start in seconds
    double end_time;                    // simulation clock end in seconds
    double time_step;                   // seconds per step, e.g. 1800 = 30 simulated minutes
    double safety_distance_threshold;   // metres — two aircraft closer than this = violation
    int    max_violations_threshold;    // stop the sim if total violations exceeds this
    double sim_start_seconds;           // wall-clock anchor: seconds since midnight (e.g. 21600 for 06:00)
} SimulationConfig;

// ── Shared memory block ───────────────────────────────────────────────────────

// This is the struct that lives in POSIX shared memory (shm_open + mmap).
// After fork(), both parent and all children map to the EXACT SAME physical RAM.
// A write by any process is immediately visible to all others — no copies, no messages.
//
// Layout: config and num_flights are set before fork and are read-only during the sim.
//         flights[] is read/written by children (each child owns its own slot).
//         current_time, total_violations_detected, termination_requested are
//         written by the parent and read by children.

// ── US109 — Simulation Report Data ───────────────────────────────────────
//
// Records a single safety violation event with full context:
// timestamp, both aircraft positions and velocities, separation distance.

typedef struct {
    double timestamp;           // simulation clock when violation occurred (seconds)
    int flight_a_id;           // index of first aircraft
    int flight_b_id;           // index of second aircraft
    Position pos_a;            // position of flight_a at violation time
    Position pos_b;            // position of flight_b at violation time
    Velocity vel_a;            // velocity vector of flight_a
    Velocity vel_b;            // velocity vector of flight_b
    double distance;           // actual separation in metres (below threshold)
} ViolationEvent;

#define MAX_RECORDED_VIOLATIONS 1000

// Per-flight execution status in the report
typedef struct {
    char callsign[CALLSIGN_LEN];
    bool completed_successfully;
    bool had_violation;
    double fuel_remaining;      // kg at end of flight
    double total_distance;      // metres flown
    int num_legs_completed;     // how many legs finished
} FlightStatus;

// Complete simulation report (lives in shared memory for persistence)
typedef struct {
    // Simulation timing
    double sim_start_time;      // simulation clock start (seconds)
    double sim_end_time;        // simulation clock end (seconds)
    double wall_clock_start;    // wall-clock seconds when sim began

    // Totals
    int total_flights;
    int total_violations_recorded;

    // Violation log
    ViolationEvent violations[MAX_RECORDED_VIOLATIONS];

    // Per-flight statuses
    FlightStatus flight_statuses[MAX_FLIGHTS];

    // Report generation flag (set by parent when sim completes)
    bool report_ready;
} SimulationReport;

typedef struct {
    SimulationConfig config;        // simulation parameters — read-only after loading
    double current_time;            // global clock; advances in the parent's loop

    int num_flights;                // how many flights were loaded from JSON
    Flight flights[MAX_FLIGHTS];    // the full flight data array — shared R/W

    int  total_violations_detected; // incremented by parent's violation checker
    bool termination_requested;     // parent sets this true → children exit their loops

    SimulationReport report;        // US109 — report data accumulated during sim
} SharedMemoryData;

#endif /* STRUCTS_H */
