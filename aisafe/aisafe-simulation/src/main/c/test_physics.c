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

    failures += check("isa_temperature(0)",     isa_temperature(0),      288.15, 0.1);
    failures += check("isa_temperature(11000)", isa_temperature(11000),  216.65, 0.1);
    failures += check("isa_temperature(12000)", isa_temperature(12000),  216.65, 0.1);

    failures += check("isa_air_density(0)",     isa_air_density(0),      1.225,  0.002);
    failures += check("isa_air_density(11000)", isa_air_density(11000),  0.364,  0.002);

    failures += check("isa_speed_of_sound(0)",     isa_speed_of_sound(0),     340.3, 0.5);
    failures += check("isa_speed_of_sound(11000)", isa_speed_of_sound(11000), 295.1, 0.5);

    failures += check("thrust Mach=0 SL",
        calculate_thrust(338000, 180000, 0.96, 0.0, 1.225),
        338000.0, 500.0);

    failures += check("thrust Mach=0.9 SL",
        calculate_thrust(338000, 180000, 0.96, 0.9, 1.225),
        180000.0, 2000.0);

    double cl = calculate_lift_coefficient(277000, 845, 1.225, 250);
    failures += check("lift_coef range (0 < Cl < 3)",
        cl, 1.0, 1.0);

    double cd = calculate_drag_coefficient(0.02, cl, 7.53, 0.84);
    failures += check("drag_coef >= cd0", cd, cd, 0.0);

    failures += check("fuel_burn per step",
        calculate_fuel_burn(1352000, 1.6e-4, 60),
        1323.0, 10.0);

    failures += check("step_distance level",
        calculate_step_distance(250.0, 0.0, 60.0),
        15000.0, 1.0);

    printf("\n%d failure(s)\n", failures);
    return failures > 0 ? 1 : 0;
}
