#include <stdio.h>
#include <math.h>
#include <signal.h>
#include <unistd.h>
#include <string.h>
#include <errno.h>
#include "flight_simulation.h"
#include "physics.h"

// g_callsign is set once before any signal can fire; both handlers read it safely.
// g_terminate follows the TP4 flag pattern: set inside the handler (async-signal-safe),
// checked in the step loop — exit() must NOT be called from a signal handler.
static char                   g_callsign[CALLSIGN_LEN];
static volatile sig_atomic_t  g_terminate = 0;

/**
 * @brief Writes "[callsign] suffix" to stdout using only async-signal-safe calls.
 *
 * Uses write() directly instead of printf/sprintf, which are not async-signal-safe.
 * A pointer walk replaces strlen() for the same reason.
 *
 * @param callsign Null-terminated callsign string.
 * @param suffix   Null-terminated message suffix.
 */
static void sig_write_msg(const char *callsign, const char *suffix) {
    char buf[256];
    char *p = buf;
    char *end = buf + sizeof(buf) - 1;

    *p++ = '[';
    for (const char *s = callsign; *s && p < end; ) *p++ = *s++;
    *p++ = ']';
    *p++ = ' ';
    for (const char *s = suffix; *s && p < end; ) *p++ = *s++;

    write(STDOUT_FILENO, buf, (size_t)(p - buf));
}

/**
 * @brief SIGUSR1 handler — notifies the flight of a proximity violation.
 */
static void sigusr1_handler(int sig) {
    (void)sig;
    sig_write_msg(g_callsign, "SIGUSR1: safety violation detected.\n");
}

/**
 * @brief SIGTERM handler — requests clean shutdown of the flight process.
 *
 * Fired by the parent when total violations exceed the configured threshold.
 * Sets g_terminate flag only; exit() must NOT be called from a signal handler.
 *
 * @param sig Signal number (unused).
 */
static void sigterm_handler(int sig) {
    (void)sig;
    g_terminate = 1;
}

#define ARRIVAL_THRESHOLD_M  2000.0

/**
 * @brief Computes the great-circle distance between two lat/lon points (Haversine).
 *
 * Accurate for any distance on Earth; does not use a flat-earth approximation.
 *
 * @param lat1_deg Latitude of point 1 in degrees.
 * @param lon1_deg Longitude of point 1 in degrees.
 * @param lat2_deg Latitude of point 2 in degrees.
 * @param lon2_deg Longitude of point 2 in degrees.
 * @return Distance in metres.
 */
static double haversine_m(double lat1_deg, double lon1_deg,
                           double lat2_deg, double lon2_deg) {
    const double R = 6371000.0;
    double lat1 = lat1_deg * M_PI / 180.0;
    double lat2 = lat2_deg * M_PI / 180.0;
    double dlat = (lat2_deg - lat1_deg) * M_PI / 180.0;
    double dlon = (lon2_deg - lon1_deg) * M_PI / 180.0;
    double a = sin(dlat/2)*sin(dlat/2)
             + cos(lat1)*cos(lat2)*sin(dlon/2)*sin(dlon/2);
    return R * 2.0 * atan2(sqrt(a), sqrt(1.0 - a));
}

/**
 * @brief Computes the initial bearing from point 1 to point 2 on a sphere.
 *
 * 0 = north, pi/2 = east, pi = south, -pi/2 = west (atan2 convention).
 *
 * @param lat1_deg Latitude of origin in degrees.
 * @param lon1_deg Longitude of origin in degrees.
 * @param lat2_deg Latitude of destination in degrees.
 * @param lon2_deg Longitude of destination in degrees.
 * @return Bearing in radians.
 */
static double bearing_rad(double lat1_deg, double lon1_deg,
                           double lat2_deg, double lon2_deg) {
    double lat1    = lat1_deg * M_PI / 180.0;
    double lat2    = lat2_deg * M_PI / 180.0;
    double dlambda = (lon2_deg - lon1_deg) * M_PI / 180.0;
    return atan2(sin(dlambda) * cos(lat2),
                 cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dlambda));
}

/**
 * @brief Moves a lat/lon point by a given distance along a bearing.
 *
 * Updates *lat_deg and *lon_deg in-place using spherical trigonometry.
 *
 * @param lat_deg Pointer to latitude in degrees (updated in-place).
 * @param lon_deg Pointer to longitude in degrees (updated in-place).
 * @param dist_m  Distance to move in metres.
 * @param bearing Direction of travel in radians.
 */
static void advance_position(double *lat_deg, double *lon_deg,
                              double dist_m, double bearing) {
    const double R = 6371000.0;
    double delta = dist_m / R;
    double lat   = *lat_deg * M_PI / 180.0;
    double lon   = *lon_deg * M_PI / 180.0;
    double lat2  = asin(sin(lat)*cos(delta) + cos(lat)*sin(delta)*cos(bearing));
    double lon2  = lon + atan2(sin(bearing)*sin(delta)*cos(lat),
                               cos(delta) - sin(lat)*sin(lat2));
    *lat_deg = lat2 * 180.0 / M_PI;
    *lon_deg = lon2 * 180.0 / M_PI;
}

/**
 * @brief Advances the flight's current waypoint index past any already-passed waypoints.
 *
 * A waypoint is considered passed when the aircraft is at least as close to the
 * destination as that waypoint is. Stops short of the final waypoint so the
 * arrival check can fire normally.
 *
 * @param f            Pointer to the Flight in local memory.
 * @param dist_to_dest Current great-circle distance to the destination in metres.
 * @param dest_lat     Destination latitude in degrees.
 * @param dest_lon     Destination longitude in degrees.
 */
static void advance_waypoints(Flight *f, double dist_to_dest, double dest_lat, double dest_lon) {
    int last = f->legs[f->current_leg].waypoint_end_index;
    while (f->flight_plan.current_waypoint_index < last) {
        int wi = f->flight_plan.current_waypoint_index;
        double dist_wp_to_dest = haversine_m(
            f->flight_plan.waypoints[wi].pos.latitude,
            f->flight_plan.waypoints[wi].pos.longitude,
            dest_lat, dest_lon);
        if (dist_to_dest <= dist_wp_to_dest)
            f->flight_plan.current_waypoint_index++;
        else
            break;
    }
}

/**
 * @brief Returns the target IAS for the given phase and altitude.
 *
 * ATC restricts all aircraft to 250 kts below 10 000 ft (3 048 m).
 *
 * @param altitude_m Altitude in metres above MSL.
 * @param phase      Current flight phase.
 * @return Target indicated air speed in knots.
 */
static double ias_schedule_knots(double altitude_m, FlightPhase phase) {
    if (phase == PHASE_DESCEND) return 280.0;
    if (altitude_m < 3048.0)    return 250.0;
    return 300.0;
}

/**
 * @brief Executes one physics time step for a single flight.
 *
 * Reads and updates the child's local Flight copy. Also checks for arrival
 * and sets f->is_active = false when the aircraft reaches the destination.
 *
 * @param shm Local simulation data block (reads config and flights[idx]).
 * @param idx Index of the flight to advance.
 * @param out Output struct filled with all 25 intermediate physics values.
 */
static void compute_physics_step(SimulationData *shm, int idx, StepData *out) {
    Flight *f  = &shm->flights[idx];
    double dt  = shm->config.time_step;

    int last_wp  = f->legs[f->current_leg].waypoint_end_index;
    double dest_lat = f->flight_plan.waypoints[last_wp].pos.latitude;
    double dest_lon = f->flight_plan.waypoints[last_wp].pos.longitude;

    double dist_to_dest = haversine_m(f->current_pos.latitude, f->current_pos.longitude,
                                       dest_lat, dest_lon);

    advance_waypoints(f, dist_to_dest, dest_lat, dest_lon);
    int wi = f->flight_plan.current_waypoint_index;
    FlightPhase phase = f->flight_plan.waypoints[wi].segment_phase;

    double cruise_alt = f->flight_plan.waypoints[f->legs[f->current_leg].waypoint_start_index].pos.altitude;

    double alt_m    = f->current_pos.altitude;
    double temp     = isa_temperature(alt_m);
    double pressure = isa_pressure(alt_m);
    double rho      = isa_air_density(alt_m);
    double a_sound  = isa_speed_of_sound(alt_m);

    double wp_speed = f->flight_plan.waypoints[wi].target_speed;
    double ias_kts, mach, tas;
    if (wp_speed > 0.0 && phase == PHASE_CRUISE) {
        /* Cruise profile speed is TAS (kts); derive Mach directly. */
        tas    = wp_speed * 0.514444;
        mach   = tas / a_sound;
        ias_kts = wp_speed;
    } else {
        ias_kts = (wp_speed > 0.0) ? wp_speed : ias_schedule_knots(alt_m, phase);
        mach   = calculate_mach(ias_kts, rho);
        tas    = calculate_tas(mach, alt_m);
    }

    double cl = calculate_lift_coefficient(f->current_mass, f->properties.wing_area, rho, tas);
    double cd = calculate_drag_coefficient(f->properties.cd0, cl, f->properties.aspect_ratio, f->properties.oswald_efficiency);
    double lift = calculate_lift_force(cl, f->properties.wing_area, rho, tas);
    double drag = calculate_drag_force(cd, f->properties.wing_area, rho, tas);

    double thrust_per_engine = calculate_thrust(
        f->properties.thrust_sea_level_stopped,
        f->properties.thrust_sea_level_cruise,
        f->properties.thrust_lapse_rate, mach, rho);

    double total_thrust;
    switch (phase) {
        case PHASE_CRUISE:  total_thrust = drag; break;
        case PHASE_DESCEND: total_thrust = thrust_per_engine * f->properties.num_engines * 0.1; break;
        default:            total_thrust = thrust_per_engine * f->properties.num_engines; break;
    }

    double climb_rate  = calculate_climb_rate(total_thrust, drag, tas, f->current_mass);
    double fuel_burned = calculate_fuel_burn(total_thrust, f->properties.tsfc, dt);
    double horiz_dist  = calculate_step_distance(tas, climb_rate, dt);

    // Intent: bearing straight to destination (before any reroute deviation).
    // Stored so the conflict check can look ahead along the un-deviated path.
    double intent_angle = bearing_rad(f->current_pos.latitude, f->current_pos.longitude, dest_lat, dest_lon);
    f->intent_heading   = intent_angle * 180.0 / M_PI;

    // Actual flown heading = intent plus the reroute deviation set by check_violations.
    double heading_angle = intent_angle + shm->deviation_rad[idx];

    // Snap to destination to avoid oscillating past it every step.
    if (dist_to_dest <= horiz_dist) {
        fuel_burned *= dist_to_dest / horiz_dist;
        horiz_dist = dist_to_dest;
        f->current_pos.latitude  = dest_lat;
        f->current_pos.longitude = dest_lon;
        phase = PHASE_DESCEND;
        f->current_phase = phase;
    } else {
        advance_position(&f->current_pos.latitude, &f->current_pos.longitude, horiz_dist, heading_angle);
    }

    f->current_pos.altitude += climb_rate * dt;
    if (f->current_pos.altitude < 0.0)
        f->current_pos.altitude = 0.0;

    double target_alt = f->flight_plan.waypoints[wi].pos.altitude;
    if (phase == PHASE_CLIMB && f->current_pos.altitude > target_alt)
        f->current_pos.altitude = target_alt;
    else if (phase == PHASE_DESCEND && f->current_pos.altitude < target_alt)
        f->current_pos.altitude = target_alt;

    f->current_mass -= fuel_burned;
    double min_mass = f->properties.empty_weight + f->payload;
    if (f->current_mass < min_mass)
        f->current_mass = min_mass;

    f->velocity.speed          = tas;
    f->velocity.vertical_speed = climb_rate;
    f->velocity.heading        = heading_angle * 180.0 / M_PI;
    f->current_phase           = phase;

    double new_dist = haversine_m(f->current_pos.latitude, f->current_pos.longitude,
                                   dest_lat, dest_lon);
    if (new_dist < ARRIVAL_THRESHOLD_M)
        f->is_active = false;

    out->dist_to_dest      = dist_to_dest;
    out->cruise_alt        = cruise_alt;
    out->temperature       = temp;
    out->pressure          = pressure;
    out->rho               = rho;
    out->speed_of_sound    = a_sound;
    out->ias_kts           = ias_kts;
    out->mach              = mach;
    out->tas               = tas;
    out->cl                = cl;
    out->cd                = cd;
    out->lift              = lift;
    out->drag              = drag;
    out->thrust_per_engine = thrust_per_engine;
    out->total_thrust      = total_thrust;
    out->climb_rate        = climb_rate;
    out->fuel_burned       = fuel_burned;
    out->horiz_dist        = horiz_dist;
    out->phase             = phase;
}

void run_flight_process(SharedBlock *shm, int flight_index) {
    Flight *f = &shm->sim.flights[flight_index];

    strncpy(g_callsign, f->callsign, CALLSIGN_LEN - 1);
    g_callsign[CALLSIGN_LEN - 1] = '\0';

    struct sigaction sa_usr1;
    sa_usr1.sa_handler = sigusr1_handler;
    sigemptyset(&sa_usr1.sa_mask);
    sa_usr1.sa_flags = SA_RESTART;
    sigaction(SIGUSR1, &sa_usr1, NULL);

    struct sigaction sa_term;
    sa_term.sa_handler = sigterm_handler;
    sigemptyset(&sa_term.sa_mask);
    sa_term.sa_flags = 0;
    sigaction(SIGTERM, &sa_term, NULL);

    f->current_phase = f->flight_plan.waypoints[f->flight_plan.current_waypoint_index].segment_phase;

    printf("[%s] Process started (PID %d). "
           "Initial mass: %.0f kg | Fuel: %.0f kg | Payload: %.0f kg\n",
           f->callsign, (int)getpid(),
           f->initial_mass, f->initial_fuel_mass, f->payload);
    fflush(stdout);

    /* Signal parent that this child is ready (reuses step_done — no separate semaphore needed). */
    sem_post(&shm->step_done[flight_index]);

    while (1) {
        int r;
        do { r = sem_wait(&shm->step_go[flight_index]); } while (r < 0 && errno == EINTR && !g_terminate);

        if (r < 0 || g_terminate || shm->terminate || !f->is_active)
            break;

        StepData step;
        compute_physics_step(&shm->sim, flight_index, &step);

        /* Store step data so the parent Violations thread can read it for display. */
        shm->sim.last_step[flight_index] = step;

        /* Handle multi-leg flights: if this leg just ended and another follows, reset state. */
        if (!f->is_active && f->current_leg < f->num_legs - 1) {
            f->current_leg++;
            Leg *next = &f->legs[f->current_leg];

            f->departure_offset_s                 = next->departure_offset_s;
            f->payload                            = next->payload;
            f->initial_fuel_mass                  = next->fuel_mass;
            f->current_mass                       = f->properties.empty_weight + next->payload + next->fuel_mass;
            f->flight_plan.current_waypoint_index = next->waypoint_start_index;
            f->leg_just_completed                 = true;
            f->is_active                          = true;
            f->initial_mass                       = f->properties.empty_weight + next->payload + next->fuel_mass;
            f->fuel_exhaustion_reported           = false;
        }

        sem_post(&shm->step_done[flight_index]);

        if (!f->is_active)
            break;
    }
}
