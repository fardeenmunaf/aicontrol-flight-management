#include "physics.h"
#include <math.h>

#ifndef M_PI
#define M_PI 3.14159265358979323846
#endif

double calculate_lift_force(double cl, double area, double air_density, double velocity) {
    if (area <= 0 || air_density <= 0 || velocity < 0) return 0.0;
    return cl * area * air_density * (velocity * velocity) / 2.0;
}

double calculate_drag_force(double cd, double area, double air_density, double velocity) {
    if (area <= 0 || air_density <= 0 || velocity < 0) return 0.0;
    return cd * area * air_density * (velocity * velocity) / 2.0;
}

double calculate_lift_coefficient(double mass, double area, double air_density, double velocity) {
    if (area <= 0 || air_density <= 0 || velocity <= 0) return 0.0;
    double weight = mass * PHYSICS_GRAVITY;
    return (2.0 * weight) / (air_density * area * velocity * velocity);
}

double calculate_drag_coefficient(double cd0, double cl, double aspect_ratio, double e) {
    if (aspect_ratio <= 0 || e <= 0) return cd0;
    return cd0 + ((cl * cl) / (M_PI * aspect_ratio * e));
}

double calculate_thrust_at_altitude(double thrust_sea_level, double density_altitude, double density_sea_level) {
    if (density_sea_level <= 0 || density_altitude < 0) return 0.0;
    // Assuming a linear density ratio for thrust scaling as a baseline. 
    // Wait, some formulas use power of 0.7 or 1.0. The standard turbofan might be ~ (rho/rho0).
    return thrust_sea_level * (density_altitude / density_sea_level);
}

double calculate_range(double true_air_speed, double tsfc, double lift_force, double drag_force, double initial_weight, double final_weight) {
    if (tsfc <= 0 || drag_force <= 0 || final_weight <= 0 || initial_weight <= final_weight) return 0.0;
    
    double lift_to_drag = lift_force / drag_force;
    double weight_ratio_ln = log(initial_weight / final_weight);
    
    return (true_air_speed / tsfc) * lift_to_drag * weight_ratio_ln;
}
