#ifndef STRUCTS_H
#define STRUCTS_H

#include <sys/types.h>
#include <sys/mman.h>
#include <fcntl.h>
#include <semaphore.h>
#include <unistd.h>
#include <stdbool.h>
#include <time.h>
#include <pthread.h>

#define MAX_FLIGHTS   100
#define MAX_WAYPOINTS  50
#define MAX_LEGS       10
#define CALLSIGN_LEN   16

#define FUEL_DENSITY_KG_PER_L  0.804

#define ARRIVAL_OFFSET_NOT_SET (-1.0)

typedef struct {
    double latitude;
    double longitude;
    double altitude;
    double timestamp;
} Position;

typedef struct {
    double speed;
    double heading;
    double vertical_speed;
} Velocity;

typedef struct {
    double empty_weight;
    double wing_area;
    double aspect_ratio;
    double oswald_efficiency;
    double cd0;
    int    num_engines;
    double thrust_sea_level_stopped;
    double thrust_sea_level_cruise;
    double thrust_lapse_rate;
    double tsfc;
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
    double wind_direction_deg;
    double wind_speed_ms;
    FlightPhase segment_phase;
} Waypoint;

typedef struct {
    Waypoint waypoints[MAX_WAYPOINTS];
    int num_waypoints;
    int current_waypoint_index;
} FlightPlan;

typedef struct {
    double departure_offset_s;
    double scheduled_arrival_offset_s;
    double fuel_mass;
    double payload;
    int    waypoint_start_index;
    int    waypoint_end_index;
} Leg;

typedef struct {
    double dist_to_dest;
    double cruise_alt;
    double temperature;
    double pressure;
    double rho;
    double speed_of_sound;
    double ias_kts;
    double mach;
    double tas;
    double cl;
    double cd;
    double lift;
    double drag;
    double thrust_per_engine;
    double total_thrust;
    double climb_rate;
    double fuel_burned;
    double horiz_dist;
    FlightPhase phase;
} StepData;

typedef struct {
    int  id;
    char callsign[CALLSIGN_LEN];
    pid_t process_id;

    Position current_pos;
    Velocity velocity;
    double   intent_heading;   /* bearing to destination (deg), before any reroute deviation */
    AircraftProperties properties;
    FlightPlan flight_plan;

    double payload;
    double initial_fuel_mass;
    double initial_mass;
    double current_mass;
    double max_climb_rate_mps;    /* from flightProfile rateClimb; 0 = uncapped  */
    double max_descent_rate_mps;  /* from flightProfile rateDescent; 0 = uncapped */

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

typedef struct {
    double start_time;
    double end_time;
    double time_step;
    double safety_distance_threshold;
    double collision_distance_threshold;
    int    max_violations_threshold;
    double sim_start_seconds;
    bool   reroute;
    int    sim_start_year;
    int    sim_start_month;
    int    sim_start_day;
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

    StepData last_step[MAX_FLIGHTS];
    double   deviation_rad[MAX_FLIGHTS];   /* reroute turn applied per flight this step */
    Position prev_pos[MAX_FLIGHTS];
} SimulationData;

/* Per-flight wind data written by the environment thread each step. */
typedef struct {
    double wind_speed_ms;
    double wind_direction_deg;
} WindData;

#define MAX_WEATHER_DB 200

typedef struct {
    double lat1, lon1, lat2, lon2;
    double alt_inf, alt_sup;
    double wind_direction_deg;
    double wind_speed_ms;
} WeatherEntry;

/* Inter-process shared memory block.
   Placed in POSIX shared memory so parent and child processes share it.
   We use named semaphores (sem_open) on all platforms: the returned pointers
   are inherited across fork, and unlike sem_init(pshared=1) this works on
   macOS too (where process-shared sem_init is unsupported). */
typedef struct {
    SimulationData sim;
    sem_t         *step_go[MAX_FLIGHTS];    /* parent posts → child wakes  */
    sem_t         *step_done[MAX_FLIGHTS];  /* child posts  → Results wakes */
    bool           terminate;               /* set by cleanup to unblock children */
    WindData       env_wind[MAX_FLIGHTS];   /* written by env thread, read by flight processes */
    sem_t         *env_sem;                 /* named binary semaphore protecting env_wind */
} SharedBlock;

/* Semaphore accessor macros — resolve to sem_t*. */
#define SEM_GO(shm, i)   ((shm)->step_go[i])
#define SEM_DONE(shm, i) ((shm)->step_done[i])
#define SEM_ENV(shm)     ((shm)->env_sem)

/* Parent thread phase — guards condition variable waits against spurious wakeups. */
typedef enum {
    THREAD_IDLE,
    THREAD_ENVIRONMENT,  /* first: env thread writes wind before children start */
    THREAD_DISPATCHER,
    THREAD_RESULTS,
    THREAD_VIOLATIONS,
    THREAD_REPORT
} SimThreadPhase;

/* Parent-process thread coordination context.
   Holds the shared memory pointer, mutex, condition variables, and
   per-step state shared between the parent-process worker threads. */
typedef struct {
    SharedBlock      *shm;
    pthread_mutex_t   mutex;
    pthread_cond_t    cond_environment;
    pthread_cond_t    cond_dispatch;
    pthread_cond_t    cond_results;
    pthread_cond_t    cond_violations;
    pthread_cond_t    cond_step_done;
    pthread_cond_t    cond_report;
    SimThreadPhase    current_thread;
    bool              was_active[MAX_FLIGHTS];
    int               num_flights;
    pid_t             pids[MAX_FLIGHTS];
    PositionHistory  *histories;
    FlightPhase       prev_phase[MAX_FLIGHTS];
    double            step_time;
    int               step_num;
    int               total_steps;
    bool              sim_done;
    int               pending_violations;
    int               logged_violation_idx; /* next violations[] index to log in report_thread */
    bool              report_finalize;
} SimContext;

#endif /* STRUCTS_H */
