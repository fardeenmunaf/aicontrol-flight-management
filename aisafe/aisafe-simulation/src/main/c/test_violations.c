/* Tests for check_violations() (US107) — collision detection, fuel
 * exhaustion, and bookkeeping (deviation reset, inactive-flight skip).
 *
 * check_violations() is `static` inside simulation_engine.c, so this file
 * includes that translation unit directly (renaming its main so the two
 * don't collide) — the same technique used to unit-test other static
 * helpers without changing production code. */
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

/* Two active flights, far enough apart that no violation is detected by
 * default. Each test tweaks the fields it needs before calling
 * check_violations(). */
static void base_sim(SimulationData *sim, bool *was_active, pid_t *pids) {
    memset(sim, 0, sizeof(*sim));
    memset(was_active, 0, sizeof(bool) * MAX_FLIGHTS);
    memset(pids, 0, sizeof(pid_t) * MAX_FLIGHTS);

    sim->num_flights = 2;
    sim->config.collision_distance_threshold = 200.0;
    sim->config.safety_distance_threshold     = 2000.0;
    sim->config.reroute                       = false;
    sim->config.max_violations_threshold      = 0;

    strncpy(sim->flights[0].callsign, "AAA1", CALLSIGN_LEN - 1);
    strncpy(sim->flights[1].callsign, "BBB2", CALLSIGN_LEN - 1);

    sim->flights[0].is_active = true;
    sim->flights[1].is_active = true;
    was_active[0] = true;
    was_active[1] = true;

    /* Plenty of fuel left by default, so fuel-exhaustion checks don't fire
     * unless a test deliberately sets current_mass low. */
    for (int i = 0; i < 2; i++) {
        sim->flights[i].properties.empty_weight = 50000.0;
        sim->flights[i].payload                 = 5000.0;
        sim->flights[i].current_mass            = 80000.0;
    }

    /* Both far apart by default (~1 degree latitude ~ 111 km). */
    sim->flights[0].current_pos = (Position){41.0, -8.0, 10000.0, 0.0};
    sim->flights[1].current_pos = (Position){42.0, -8.0, 10000.0, 0.0};
    sim->prev_pos[0] = sim->flights[0].current_pos;
    sim->prev_pos[1] = sim->flights[1].current_pos;
}

static int test_collision_recorded(void) {
    SimulationData sim; bool was_active[MAX_FLIGHTS]; pid_t pids[MAX_FLIGHTS];
    base_sim(&sim, was_active, pids);

    /* Same position for both flights -> separation 0 < collision threshold. */
    sim.flights[1].current_pos = sim.flights[0].current_pos;
    sim.prev_pos[1] = sim.flights[1].current_pos;

    check_violations(&sim, was_active, pids, sim.num_flights);

    int ok = sim.flights[0].violation_detected &&
             sim.flights[1].violation_detected &&
             sim.report.total_violations_recorded == 1 &&
             sim.report.violations[0].flight_a_id == 0 &&
             sim.report.violations[0].flight_b_id == 1;
    return check("Collision is recorded as a two-flight ViolationEvent", ok);
}

static int test_safety_warning_not_recorded(void) {
    SimulationData sim; bool was_active[MAX_FLIGHTS]; pid_t pids[MAX_FLIGHTS];
    base_sim(&sim, was_active, pids);

    /* ~1 km apart: between collision (200 m) and safety (2000 m) thresholds. */
    sim.flights[1].current_pos = sim.flights[0].current_pos;
    sim.flights[1].current_pos.latitude += 0.009;
    sim.prev_pos[1] = sim.flights[1].current_pos;

    check_violations(&sim, was_active, pids, sim.num_flights);

    int ok = !sim.flights[0].violation_detected &&
             !sim.flights[1].violation_detected &&
             sim.report.total_violations_recorded == 0;
    return check("Proximity warning alone does not record a violation", ok);
}

static int test_no_violation_when_far_apart(void) {
    SimulationData sim; bool was_active[MAX_FLIGHTS]; pid_t pids[MAX_FLIGHTS];
    base_sim(&sim, was_active, pids);

    check_violations(&sim, was_active, pids, sim.num_flights);

    int ok = sim.total_violations_detected == 0 &&
             sim.report.total_violations_recorded == 0;
    return check("No violation recorded when flights are well separated", ok);
}

static int test_fuel_exhaustion_recorded(void) {
    SimulationData sim; bool was_active[MAX_FLIGHTS]; pid_t pids[MAX_FLIGHTS];
    base_sim(&sim, was_active, pids);

    /* Flight 1 plays no part; only flight 0's fuel state matters. */
    was_active[1] = false;
    sim.flights[1].is_active = false;

    sim.flights[0].properties.empty_weight = 50000.0;
    sim.flights[0].payload                 = 5000.0;
    sim.flights[0].current_mass            = 55000.0; /* fuel_remaining == 0 */
    sim.flights[0].fuel_exhaustion_reported = false;

    check_violations(&sim, was_active, pids, sim.num_flights);

    int ok = sim.flights[0].fuel_exhaustion_reported &&
             sim.flights[0].violation_detected &&
             sim.report.total_violations_recorded == 1 &&
             sim.report.violations[0].flight_a_id == sim.report.violations[0].flight_b_id;
    return check("Fuel exhaustion is recorded as a single-flight ViolationEvent", ok);
}

static int test_inactive_flight_skipped(void) {
    SimulationData sim; bool was_active[MAX_FLIGHTS]; pid_t pids[MAX_FLIGHTS];
    base_sim(&sim, was_active, pids);

    /* Overlapping positions, but flight 0 was not active this step. */
    sim.flights[1].current_pos = sim.flights[0].current_pos;
    sim.prev_pos[1] = sim.flights[1].current_pos;
    was_active[0] = false;

    check_violations(&sim, was_active, pids, sim.num_flights);

    int ok = sim.report.total_violations_recorded == 0 &&
             !sim.flights[0].violation_detected &&
             !sim.flights[1].violation_detected;
    return check("A flight inactive this step is excluded from violation checks", ok);
}

static int test_deviation_reset_each_step(void) {
    SimulationData sim; bool was_active[MAX_FLIGHTS]; pid_t pids[MAX_FLIGHTS];
    base_sim(&sim, was_active, pids);

    sim.deviation_rad[0] = M_PI / 12.0;
    sim.deviation_rad[1] = M_PI / 12.0;

    check_violations(&sim, was_active, pids, sim.num_flights);

    int ok = sim.deviation_rad[0] == 0.0 && sim.deviation_rad[1] == 0.0;
    return check("Reroute deviation is cleared at the start of each check", ok);
}

int main(void) {
    int failures = 0;

    failures += test_collision_recorded();
    failures += test_safety_warning_not_recorded();
    failures += test_no_violation_when_far_apart();
    failures += test_fuel_exhaustion_recorded();
    failures += test_inactive_flight_skipped();
    failures += test_deviation_reset_each_step();

    printf("\n%d failure(s)\n", failures);
    return failures > 0 ? 1 : 0;
}
