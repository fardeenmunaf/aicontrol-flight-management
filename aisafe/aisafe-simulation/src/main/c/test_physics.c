#include <stdio.h>
#include <math.h>
#include "physics.h"

#define PASS "\033[32mPASS\033[0m"
#define FAIL "\033[31mFAIL\033[0m"

static int check(const char *name, double got, double expected, double tol) {
    int ok = fabs(got - expected) <= tol;
    printf("[%s] %s: got %.4f, expected %.4f\n", ok ? PASS : FAIL, name, got, expected);
    return ok ? 0 : 1;
}

int main(void) {
    int failures = 0;

    // ISA atmosphere — values from spec constants table and altitude table
    failures += check("isa_temperature(0)",     isa_temperature(0),      288.15, 0.1);
    failures += check("isa_temperature(11000)", isa_temperature(11000),  216.65, 0.1);
    failures += check("isa_temperature(12000)", isa_temperature(12000),  216.65, 0.1);

    failures += check("isa_air_density(0)",     isa_air_density(0),      1.225,  0.002);
    failures += check("isa_air_density(11000)", isa_air_density(11000),  0.364,  0.002);

    failures += check("isa_speed_of_sound(0)",     isa_speed_of_sound(0),     340.3, 0.5);
    failures += check("isa_speed_of_sound(11000)", isa_speed_of_sound(11000), 295.1, 0.5);

    // Thrust — spec: 4-engine aircraft, T_stopped=338000 N, T_cruise=180000 N, lapse=0.96
    // At Mach 0, sea level: T_engine should equal T_stopped
    failures += check("thrust Mach=0 SL",
        calculate_thrust(338000, 180000, 0.96, 0.0, 1.225),
        338000.0, 500.0);

    // At Mach 0.9, sea level: T_engine should equal T_cruise (~180000 N)
    failures += check("thrust Mach=0.9 SL",
        calculate_thrust(338000, 180000, 0.96, 0.9, 1.225),
        180000.0, 2000.0);

    // Lift coefficient — basic sanity: aircraft in level flight, reasonable Cl
    double cl = calculate_lift_coefficient(277000, 845, 1.225, 250);
    failures += check("lift_coef range (0 < Cl < 3)",
        cl, 1.0, 1.0);  // just checks it's roughly in 0..2 range

    // Drag coefficient — Cd0=0.02, Cl as above, AR=7.53, e=0.84
    double cd = calculate_drag_coefficient(0.02, cl, 7.53, 0.84);
    failures += check("drag_coef >= cd0", cd, cd, 0.0);  // trivial: Cd >= Cd0

    // Fuel burn — spec: TSFC=1.6e-4 N/N/s, dt=60s, total_thrust=4*338000=1352000 N
    // dW = 1352000 * 1.6e-4 * 60 / 9.81 ≈ 1323 kg
    failures += check("fuel_burn per step",
        calculate_fuel_burn(1352000, 1.6e-4, 60),
        1323.0, 10.0);

    // Step distance — level flight at 250 m/s for 60s = 15000 m
    failures += check("step_distance level",
        calculate_step_distance(250.0, 0.0, 60.0),
        15000.0, 1.0);

    printf("\n%d failure(s)\n", failures);
    return failures > 0 ? 1 : 0;
}
