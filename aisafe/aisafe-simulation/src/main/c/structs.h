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
#define CALLSIGN_LEN   16   // characters for a flight ID, e.g. "TP1234\0"

// Jet-A fuel density used to convert litres to kg when the JSON stores volume

#define FUEL_DENSITY_KG_PER_L  0.804   // 1 litre of Jet-A ≈ 0.804 kg

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

    bool is_active;             // true while flying OR waiting to depart; child sets false when it lands
    bool violation_detected;    // true if this flight ever came too close to another
} Flight;

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

typedef struct {
    SimulationConfig config;        // simulation parameters — read-only after loading
    double current_time;            // global clock; advances in the parent's loop

    int num_flights;                // how many flights were loaded from JSON
    Flight flights[MAX_FLIGHTS];    // the full flight data array — shared R/W

    int  total_violations_detected; // incremented by parent's violation checker
    bool termination_requested;     // parent sets this true → children exit their loops
} SharedMemoryData;

#endif /* STRUCTS_H */
