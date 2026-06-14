/* Tests for finalize_and_write_report() (US109) — the per-flight status
 * fields (report.flight_statuses[] / report.total_flights) it populates
 * before forking report_generator.
 *
 * finalize_and_write_report() is `static` inside simulation_engine.c, so
 * this file includes that translation unit directly (renaming its main so
 * the two don't collide) — the same technique used in test_violations.c.
 * The fork+execl("./report_generator") it performs is harmless here even
 * if that binary isn't present: the child simply exits and the parent
 * reaps it, after the report fields below have already been written. */
#define main __sim_engine_main
#include "simulation_engine.c"
#undef main

#include <string.h>

#define PASS "\033[32mPASS\033[0m"
#define FAIL "\033[31mFAIL\033[0m"

static int check(const char *name, int ok) {
    printf("[%s] %s\n", ok ? PASS : FAIL, name);
    return ok ? 0 : 1;
}

static void base_sim(SimulationData *sim, pid_t *pids) {
    memset(sim, 0, sizeof(*sim));
    memset(pids, 0, sizeof(pid_t) * MAX_FLIGHTS);

    sim->num_flights = 2;
    sim->config.end_time = 36000.0; /* 10h sim window */

    for (int i = 0; i < 2; i++) {
        pids[i] = 1000 + i; /* non-zero: this flight was forked */
        sim->flights[i].properties.empty_weight = 50000.0;
        sim->flights[i].payload                 = 5000.0;
        sim->flights[i].current_mass            = 70000.0; /* 15000 kg fuel left */
        sim->flights[i].departure_offset_s      = 0.0;
        sim->flights[i].current_leg             = 1;
    }
    strncpy(sim->flights[0].callsign, "AAA1", CALLSIGN_LEN - 1);
    strncpy(sim->flights[1].callsign, "BBB2", CALLSIGN_LEN - 1);
}

static int test_completed_flight_status(void) {
    SimulationData sim; pid_t pids[MAX_FLIGHTS];
    base_sim(&sim, pids);

    /* Flight 0 landed normally: no longer active, no violation. */
    sim.flights[0].is_active        = false;
    sim.flights[0].violation_detected = false;

    finalize_and_write_report(&sim, sim.current_time, pids);

    const FlightStatus *s = &sim.report.flight_statuses[0];
    int ok = sim.report.total_flights == 2 &&
             strcmp(s->callsign, "AAA1") == 0 &&
             s->completed_successfully &&
             !s->had_violation &&
             s->fuel_remaining == 15000.0 &&
             s->num_legs_completed == 2; /* current_leg + 1 (not active) */
    return check("A landed flight without violations is marked completed", ok);
}

static int test_flight_with_violation_not_completed(void) {
    SimulationData sim; pid_t pids[MAX_FLIGHTS];
    base_sim(&sim, pids);

    /* Flight 0 was destroyed by a collision. */
    sim.flights[0].is_active          = false;
    sim.flights[0].violation_detected = true;

    finalize_and_write_report(&sim, sim.current_time, pids);

    const FlightStatus *s = &sim.report.flight_statuses[0];
    int ok = !s->completed_successfully && s->had_violation;
    return check("A flight with a recorded violation is not marked completed", ok);
}

static int test_still_active_flight_not_completed(void) {
    SimulationData sim; pid_t pids[MAX_FLIGHTS];
    base_sim(&sim, pids);

    /* Flight 0 is still mid-flight when the simulation ends. */
    sim.flights[0].is_active = true;

    finalize_and_write_report(&sim, sim.current_time, pids);

    const FlightStatus *s = &sim.report.flight_statuses[0];
    int ok = !s->completed_successfully && s->num_legs_completed == 1; /* current_leg + 0 */
    return check("A flight still active at sim end is not marked completed", ok);
}

static int test_negative_fuel_clamped_to_zero(void) {
    SimulationData sim; pid_t pids[MAX_FLIGHTS];
    base_sim(&sim, pids);

    /* Flight 0 burned more fuel than it had (fuel exhaustion). */
    sim.flights[0].current_mass = 40000.0; /* 40000 - 50000 - 5000 = -15000 */

    finalize_and_write_report(&sim, sim.current_time, pids);

    const FlightStatus *s = &sim.report.flight_statuses[0];
    return check("Negative fuel_remaining is clamped to 0", s->fuel_remaining == 0.0);
}

static int test_unforked_flight_excluded(void) {
    SimulationData sim; pid_t pids[MAX_FLIGHTS];
    base_sim(&sim, pids);

    /* Flight 1 was never forked (out of simulation scope). */
    pids[1] = 0;
    sim.flights[1].process_id = 0;

    finalize_and_write_report(&sim, sim.current_time, pids);

    int ok = sim.report.total_flights == 1 &&
             strcmp(sim.report.flight_statuses[0].callsign, "AAA1") == 0;
    return check("total_flights counts only flights that were actually forked", ok);
}

int main(void) {
    int failures = 0;

    failures += test_completed_flight_status();
    failures += test_flight_with_violation_not_completed();
    failures += test_still_active_flight_not_completed();
    failures += test_negative_fuel_clamped_to_zero();
    failures += test_unforked_flight_excluded();

    printf("\n%d failure(s)\n", failures);
    return failures > 0 ? 1 : 0;
}
