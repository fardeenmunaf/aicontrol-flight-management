#include <stdio.h>
#include <math.h>
#include <unistd.h>         // write()
#include "flight_simulation.h"
#include "physics.h"

// Arrival threshold: once the aircraft is within this horizontal distance
// of the destination waypoint, it is considered landed.

#define ARRIVAL_THRESHOLD_M  2000.0

// ── SECTION 1 — Geodesic helpers ──────────────────────────────────────────────
// These work on a sphere, not a flat map.
// Necessary because lat/lon are angular coordinates — simple subtraction gives
// degrees, not metres. We need actual distances to navigate correctly.

// Great-circle distance between two lat/lon points using the Haversine formula.
// Accurate for any distance on Earth — no flat-earth approximation here.

static double haversine_m(double lat1_deg, double lon1_deg,
                           double lat2_deg, double lon2_deg) {
    const double R = 6371000.0; // Earth's mean radius in metres
    double lat1 = lat1_deg * M_PI / 180.0; // convert degrees to radians
    double lat2 = lat2_deg * M_PI / 180.0;
    double dlat = (lat2_deg - lat1_deg) * M_PI / 180.0; // Δlatitude in radians
    double dlon = (lon2_deg - lon1_deg) * M_PI / 180.0; // Δlongitude in radians

    // a = sin²(Δlat/2) + cos(lat1)·cos(lat2)·sin²(Δlon/2)
    double a = sin(dlat/2)*sin(dlat/2)
             + cos(lat1)*cos(lat2)*sin(dlon/2)*sin(dlon/2);

    // c = 2 * atan2(√a, √(1-a))   — the central angle
    return R * 2.0 * atan2(sqrt(a), sqrt(1.0 - a)); // arc length = R * c
}

// Bearing (compass direction) from point 1 to point 2, in radians.
// 0 = north, π/2 = east, π = south, -π/2 = west (atan2 convention).

static double bearing_rad(double lat1_deg, double lon1_deg,
                           double lat2_deg, double lon2_deg) {
    double lat1    = lat1_deg * M_PI / 180.0;
    double lat2    = lat2_deg * M_PI / 180.0;
    double dlambda = (lon2_deg - lon1_deg) * M_PI / 180.0; // Δlongitude

    // Standard spherical bearing formula
    return atan2(sin(dlambda) * cos(lat2),
                 cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dlambda));
}

// Move a lat/lon point 'dist_m' metres along 'bearing' (radians) on the sphere.
// Updates *lat_deg and *lon_deg in-place.

static void advance_position(double *lat_deg, double *lon_deg,
                              double dist_m, double bearing) {
    const double R = 6371000.0;
    double delta = dist_m / R;                     // angular distance (radians)
    double lat   = *lat_deg * M_PI / 180.0;
    double lon   = *lon_deg * M_PI / 180.0;

    // Spherical trigonometry: compute new lat/lon after moving delta radians
    double lat2 = asin(sin(lat)*cos(delta) + cos(lat)*sin(delta)*cos(bearing));
    double lon2 = lon + atan2(sin(bearing)*sin(delta)*cos(lat),
                               cos(delta) - sin(lat)*sin(lat2));
    *lat_deg = lat2 * 180.0 / M_PI; // convert back to degrees
    *lon_deg = lon2 * 180.0 / M_PI;
}

// ── SECTION 2 — Flight phase logic ────────────────────────────────────────────

// Advance current_waypoint_index past any intermediate waypoints that the
// aircraft has already flown by.  A waypoint is considered "passed" when the
// aircraft's distance to the destination is less than or equal to that
// waypoint's distance to the destination (i.e. we are at least as close to the
// destination as the waypoint is).  We stop short of the last waypoint so the
// arrival check can fire normally.

static void advance_waypoints(Flight *f, double dist_to_dest, double dest_lat, double dest_lon) {
    int last = f->legs[f->current_leg].waypoint_end_index;
    while (f->flight_plan.current_waypoint_index < last) {
        int wi = f->flight_plan.current_waypoint_index;
        double dist_wp_to_dest = haversine_m(f->flight_plan.waypoints[wi].pos.latitude, f->flight_plan.waypoints[wi].pos.longitude, dest_lat, dest_lon);
        if (dist_to_dest <= dist_wp_to_dest) {
            f->flight_plan.current_waypoint_index++;
        } else {
            break;
        }
    }
}

// IAS (Indicated Air Speed) schedule in knots — speed target for each phase.
// Below 3 048 m (10 000 ft) ATC restricts aircraft to 250 kts by regulation.

static double ias_schedule_knots(double altitude_m, FlightPhase phase) {
    if (phase == PHASE_DESCEND) return 280.0;    // descent speed
    if (altitude_m < 3048.0)    return 250.0;    // ATC speed limit below 10 000 ft
    return 300.0;                                 // normal climb IAS above 10 000 ft
}

// ── SECTION 3 — One physics time step ─────────────────────────────────────────

// Called by the child process once per simulation step.
// Reads current state from shm->flights[idx], computes the next state,
// writes results back into shared memory, and fills *out with the 25 intermediate
// values that the parent will use to print the per-step table.

static void compute_physics_step(SharedMemoryData *shm, int idx, StepData *out) {
    Flight *f  = &shm->flights[idx];
    double dt  = shm->config.time_step; // seconds per step (e.g. 600)

    // ── destination ──────────────────────────────────────────────────────────

    // We always fly toward the LAST waypoint (the destination airport).

    int last_wp  = f->legs[f->current_leg].waypoint_end_index;
    double dest_lat = f->flight_plan.waypoints[last_wp].pos.latitude;
    double dest_lon = f->flight_plan.waypoints[last_wp].pos.longitude;

    // How far is the aircraft from the destination right now?

    double dist_to_dest = haversine_m(f->current_pos.latitude, f->current_pos.longitude, dest_lat, dest_lon);

    // ── phase via waypoint progression ───────────────────────────────────────
    // Advance past any intermediate waypoints the aircraft has already flown by,
    // then read the phase from the current target waypoint.

    advance_waypoints(f, dist_to_dest, dest_lat, dest_lon);
    int wi = f->flight_plan.current_waypoint_index;
    FlightPhase phase = f->flight_plan.waypoints[wi].segment_phase;

    // Cruise altitude: top-of-climb waypoint for the current leg (waypoint_start_index).
    double cruise_alt = f->flight_plan.waypoints[f->legs[f->current_leg].waypoint_start_index].pos.altitude;

    // ── atmosphere at current altitude ────────────────────────────────────────

    double alt_m    = f->current_pos.altitude;
    double temp     = isa_temperature(alt_m);      // K
    double pressure = isa_pressure(alt_m);          // Pa
    double rho      = isa_air_density(alt_m);       // kg/m³
    double a_sound  = isa_speed_of_sound(alt_m);    // m/s

    // ── speed: IAS → Mach → TAS ───────────────────────────────────────────────

    // 1. Pick the target IAS from the schedule (knots)
    // 2. Convert IAS + density → Mach number
    // 3. Convert Mach → True Air Speed (m/s) — the actual speed through the air

    double ias_kts = ias_schedule_knots(alt_m, phase);
    double mach    = calculate_mach(ias_kts, rho);
    double tas     = calculate_tas(mach, alt_m);

    // ── aerodynamic forces ────────────────────────────────────────────────────

    double cl = calculate_lift_coefficient(f->current_mass, f->properties.wing_area, rho, tas);
    double cd = calculate_drag_coefficient(f->properties.cd0, cl, f->properties.aspect_ratio, f->properties.oswald_efficiency);
    double lift = calculate_lift_force(cl, f->properties.wing_area, rho, tas);
    double drag = calculate_drag_force(cd, f->properties.wing_area, rho, tas);

    // ── thrust ────────────────────────────────────────────────────────────────

    // Per-engine thrust from the altitude- and speed-corrected formula

    double thrust_per_engine = calculate_thrust(f->properties.thrust_sea_level_stopped, f->properties.thrust_sea_level_cruise, f->properties.thrust_lapse_rate, mach, rho);

    double total_thrust;
    switch (phase) {
        case PHASE_CRUISE:
            // Level flight: thrust = drag exactly, so net force = 0 → no climb
            total_thrust = drag;
            break;
        case PHASE_DESCEND:
            // Idle/near-idle descent: 10% of available thrust
            total_thrust = thrust_per_engine * f->properties.num_engines * 0.1;
            break;
        default: /* PHASE_CLIMB */
            // Full thrust on all engines during the climb phase
            total_thrust = thrust_per_engine * f->properties.num_engines;
            break;
    }

    // ── integrate one time step ───────────────────────────────────────────────

    // How fast is the aircraft climbing or descending this step? (m/s)

    double climb_rate = calculate_climb_rate(total_thrust, drag, tas, f->current_mass);

    // How many kg of fuel did we burn this step?

    double fuel_burned = calculate_fuel_burn(total_thrust, f->properties.tsfc, dt);

    // How many metres did we cover horizontally?

    double horiz_dist = calculate_step_distance(tas, climb_rate, dt);

    // ── position update ───────────────────────────────────────────────────────

    // Bearing from current position toward the destination

    double hdg = bearing_rad(f->current_pos.latitude, f->current_pos.longitude, dest_lat, dest_lon);

    // If the aircraft would reach the destination within this step, snap it
    // there rather than overshooting — otherwise it oscillates past the
    // destination every step and never triggers the arrival check.

    if (dist_to_dest <= horiz_dist) {
        fuel_burned *= dist_to_dest / horiz_dist; // burn only the travelled fraction
        horiz_dist = dist_to_dest;
        f->current_pos.latitude = dest_lat;
        f->current_pos.longitude = dest_lon;
        // The aircraft is landing this step regardless of which waypoint the
        // phase tracker was on — it must be descending to touch down.
        phase = PHASE_DESCEND;
        f->current_phase = phase;
    } else {
        // Move the lat/lon point along that bearing by horiz_dist metres
        advance_position(&f->current_pos.latitude, &f->current_pos.longitude, horiz_dist, hdg);
    }

    // Altitude changes by climb_rate * dt metres

    f->current_pos.altitude += climb_rate * dt;
    if (f->current_pos.altitude < 0.0) {
        f->current_pos.altitude = 0.0;
    }

    // Cap altitude at the target defined in the flight plan:
    // during climb, don't overshoot cruise altitude;
    // during descent, don't go below destination elevation.
    double target_alt = f->flight_plan.waypoints[wi].pos.altitude;
    if (phase == PHASE_CLIMB && f->current_pos.altitude > target_alt) {
        f->current_pos.altitude = target_alt;
    } else if (phase == PHASE_DESCEND && f->current_pos.altitude < target_alt) {
        f->current_pos.altitude = target_alt;
    }

    // ── mass update ───────────────────────────────────────────────────────────

    f->current_mass -= fuel_burned;
    double min_mass = f->properties.empty_weight + f->payload;
    if (f->current_mass < min_mass)
        f->current_mass = min_mass; // fuel can't go negative — floor at dry mass + payload

    // ── update velocity struct for the parent's status display ────────────────

    f->velocity.speed          = tas;
    f->velocity.vertical_speed = climb_rate;
    f->velocity.heading        = hdg * 180.0 / M_PI; // store in degrees

    // ── store current phase in shared memory (parent reads this) ──────────────

    f->current_phase = phase;

    // ── arrival check ─────────────────────────────────────────────────────────

    // After moving, check if we are now close enough to the destination.

    double new_dist = haversine_m(f->current_pos.latitude, f->current_pos.longitude,
                                   dest_lat, dest_lon);
    if (new_dist < ARRIVAL_THRESHOLD_M) {
        f->is_active = false; // signal to parent that this process is done
    }

    // ── fill output struct with all 25 intermediate values ────────────────────
    // The parent reads these via the pipe to print the per-step table.

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

// ── SECTION 4 — Child process main loop ───────────────────────────────────────

// This function IS the child process for one flight.
// It runs in a tight wait→compute→signal loop, synchronized with the parent
// using two named POSIX semaphores (lectured in SCOMP module 6):
//
//   step_sem  — parent posts this to tell the child "compute the next step"
//   main_sem  — child posts this to tell the parent "I finished my step"
//
// This is the EXECUTION ORDER synchronization pattern from the lectures:
//   both semaphores start at 0, so each side blocks until the other signals.

void run_flight_process(SharedMemoryData *shm, int flight_index, int pipe_write_fd, sem_t *step_sem, sem_t *main_sem) {
    Flight *f = &shm->flights[flight_index];

    // Set the initial phase from the current target waypoint (set by the parser).
    f->current_phase = f->flight_plan.waypoints[f->flight_plan.current_waypoint_index].segment_phase;

    // Use getpid() here instead of f->process_id — the parent sets process_id
    // AFTER fork(), so reading it from the child is a race condition.
    printf("[%s] Process started (PID %d). "
           "Initial mass: %.0f kg | Fuel: %.0f kg | Payload: %.0f kg\n",
           f->callsign, (int)getpid(),
           f->initial_mass, f->initial_fuel_mass, f->payload);
    fflush(stdout);

    // Startup barrier (SCOMP barrier pattern):
    // Signal the parent that this child is alive and ready.
    // The parent waits for one post per child before starting the sim loop,
    // which guarantees all "Process started" lines appear before any step output.
    sem_post(main_sem);

    while (1) {
        // ── WAIT ──────────────────────────────────────────────────────────────
        // Block here until the parent signals us to run.
        // sem_wait() decrements the semaphore; if it is 0 it blocks until
        // the parent calls sem_post(step_sem).

        sem_wait(step_sem);

        // ── TERMINATION CHECK ─────────────────────────────────────────────────
        // The parent may have set termination_requested (e.g. all other flights
        // landed, or an error occurred) — check before doing any work.

        if (shm->termination_requested || !f->is_active)
            break;

        // ── COMPUTE ───────────────────────────────────────────────────────────
        // Run one physics step. This writes updated position, mass, and velocity
        // directly into shm->flights[flight_index] (shared memory — no copy needed).
        // It also fills msg.step with all 25 intermediate values for the UI table.

        PipeMessage msg;
        msg.type = MSG_POSITION_UPDATE;
        msg.flight_id = f->id;

        compute_physics_step(shm, flight_index, &msg.step);

        // ── LEG TRANSITION ────────────────────────────────────────────────────
        // compute_physics_step() sets is_active=false when the aircraft arrives
        // at the current leg's destination.  If there is a next leg, reset all
        // per-leg state and flip is_active back to true — BEFORE sem_post so the
        // parent sees the updated state after sem_wait (happened-before guarantee).

        if (!f->is_active && f->current_leg < f->num_legs - 1) {
            f->current_leg++;
            Leg *next = &f->legs[f->current_leg];

            f->departure_offset_s                 = next->departure_offset_s;
            f->payload                            = next->payload;
            f->initial_fuel_mass                  = next->fuel_mass;
            f->current_mass                       = f->properties.empty_weight + next->payload + next->fuel_mass;
            f->flight_plan.current_waypoint_index = next->waypoint_start_index;
            f->leg_just_completed                 = true;
            f->is_active                          = true;  // reset BEFORE sem_post — no race
            f->initial_mass                       = f->properties.empty_weight + next->payload + next->fuel_mass;
            f->fuel_exhaustion_reported           = false;
        }

        msg.new_pos = f->current_pos; // copy of the updated position

        // ── REPORT via PIPE ───────────────────────────────────────────────────
        // Send the new position + all physics data to the parent through the pipe.
        // write() on a pipe is atomic for small messages (< PIPE_BUF bytes),
        // so the parent always receives a complete PipeMessage struct.

        write(pipe_write_fd, &msg, sizeof(msg));

        // ── SIGNAL ────────────────────────────────────────────────────────────
        // Tell the parent that this flight has finished its step.
        // sem_post() increments main_sem by 1.
        // The parent is doing sem_wait(main_sem) once for every active flight,
        // so it unblocks only after ALL active children have posted.

        sem_post(main_sem);

        // ── CHECK IF JUST LANDED ──────────────────────────────────────────────
        // compute_physics_step() may have set is_active = false if we arrived.
        // Exit the loop so the process terminates cleanly.

        if (!f->is_active)
            break;
    }

    // Child returns here → caller (in simulation_engine.c) calls exit(EXIT_SUCCESS)
}
