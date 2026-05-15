#include <stdio.h>
#include <assert.h>
#include <math.h>
#include "physics.h"

int main() {
    double lift = calculate_lift_force(0.5, 120.0, 1.225, 250.0);
    double drag = calculate_drag_force(0.05, 120.0, 1.225, 250.0);
    double cl = calculate_lift_coefficient(75000.0, 120.0, 1.225, 250.0);
    double cd = calculate_drag_coefficient(0.02, 0.16, 9.0, 0.8);
    double thrust = calculate_thrust_at_altitude(100000.0, 0.65, 1.225);
    double range = calculate_range(250.0, 0.000015, 2296875.0, 229687.5, 735498.0, 600000.0);

    printf("Lift: %f\\n", lift);
    printf("Drag: %f\\n", drag);
    printf("Cl: %f\\n", cl);
    printf("Cd: %f\\n", cd);
    printf("Thrust: %f\\n", thrust);
    printf("Range: %f\\n", range);

    return 0;
}
