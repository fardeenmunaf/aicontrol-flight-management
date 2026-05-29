#include "physics.h"
#include <math.h>

#ifndef M_PI
#define M_PI 3.14159265358979323846
#endif

#define ISA_T0          288.15
#define ISA_LAMBDA       -0.0065
#define ISA_P0       101325.0
#define ISA_RHO0          1.225
#define ISA_R           287.05
#define ISA_GAMMA         1.4
#define ISA_G             9.81
#define ISA_TROPOPAUSE 11000.0
#define ISA_T_TROP      216.65
#define ISA_P_TROP     22632.0

#define SL_SPEED_SOUND_KNOTS 661.5

/**
 * @brief Returns the ISA temperature at a given altitude.
 *
 * Linear lapse in the troposphere (0–11 000 m); constant above.
 *
 * @param altitude_m Altitude in metres above MSL.
 * @return Temperature in Kelvin.
 */
double isa_temperature(double altitude_m) {
    if (altitude_m <= ISA_TROPOPAUSE)
        return ISA_T0 + ISA_LAMBDA * altitude_m;
    return ISA_T_TROP;
}

/**
 * @brief Returns the ISA static pressure at a given altitude.
 *
 * Uses the barometric formula for the troposphere and an isothermal
 * exponential decay above the tropopause.
 *
 * @param altitude_m Altitude in metres above MSL.
 * @return Pressure in Pascals.
 */
double isa_pressure(double altitude_m) {
    if (altitude_m <= ISA_TROPOPAUSE)
        return ISA_P0 * pow(1.0 - 0.0065 * altitude_m / ISA_T0, 5.2561);
    return ISA_P_TROP * exp(-ISA_G * (altitude_m - ISA_TROPOPAUSE)
                            / (ISA_R * ISA_T_TROP));
}

/**
 * @brief Returns the ISA air density at a given altitude.
 *
 * Derived from pressure and temperature via the ideal gas law: rho = P / (R * T).
 *
 * @param altitude_m Altitude in metres above MSL.
 * @return Air density in kg/m³.
 */
double isa_air_density(double altitude_m) {
    double T = isa_temperature(altitude_m);
    double p = isa_pressure(altitude_m);
    return p / (ISA_R * T);
}

/**
 * @brief Returns the ISA speed of sound at a given altitude.
 *
 * Formula: a = sqrt(gamma * R * T).
 *
 * @param altitude_m Altitude in metres above MSL.
 * @return Speed of sound in m/s.
 */
double isa_speed_of_sound(double altitude_m) {
    double T = isa_temperature(altitude_m);
    return sqrt(ISA_GAMMA * ISA_R * T);
}

/**
 * @brief Converts Indicated Air Speed to Mach number.
 *
 * Formula H16: M = sqrt(5 * ((rho0/rho * ((1 + 0.2*(IAS/661.5)^2)^3.5 - 1) + 1)^(2/7) - 1))
 *
 * @param ias_knots    Indicated air speed in knots.
 * @param air_density  Local air density in kg/m³.
 * @return Mach number (dimensionless). Returns 0.0 for invalid inputs.
 */
double calculate_mach(double ias_knots, double air_density) {
    if (air_density <= 0.0 || ias_knots <= 0.0) return 0.0;
    double impact_ratio = (ISA_RHO0 / air_density)
                          * (pow(1.0 + 0.2 * pow(ias_knots / SL_SPEED_SOUND_KNOTS, 2.0), 3.5) - 1.0)
                          + 1.0;
    return sqrt(5.0 * (pow(impact_ratio, 2.0 / 7.0) - 1.0));
}

/**
 * @brief Converts Mach number to True Air Speed.
 *
 * TAS = Mach * local speed of sound.
 *
 * @param mach       Mach number.
 * @param altitude_m Altitude in metres (used to look up local speed of sound).
 * @return True air speed in m/s.
 */
double calculate_tas(double mach, double altitude_m) {
    return mach * isa_speed_of_sound(altitude_m);
}

/**
 * @brief Computes the lift coefficient for steady level flight.
 *
 * Formula: Cl = 2 * W / (rho * S * V^2), where W = mass * g.
 *
 * @param mass        Aircraft mass in kg.
 * @param area        Wing area in m².
 * @param air_density Local air density in kg/m³.
 * @param velocity    True air speed in m/s.
 * @return Lift coefficient (dimensionless). Returns 0.0 for invalid inputs.
 */
double calculate_lift_coefficient(double mass, double area,
                                  double air_density, double velocity) {
    if (area <= 0.0 || air_density <= 0.0 || velocity <= 0.0) return 0.0;
    return (2.0 * mass * ISA_G) / (air_density * area * velocity * velocity);
}

/**
 * @brief Computes the total drag coefficient.
 *
 * Formula: Cd = Cd0 + Cl^2 / (pi * AR * e).
 * The second term is induced drag; it grows with Cl².
 *
 * @param cd0          Zero-lift drag coefficient.
 * @param cl           Lift coefficient.
 * @param aspect_ratio Wing aspect ratio (span² / area).
 * @param e            Oswald efficiency factor (0–1).
 * @return Drag coefficient (dimensionless).
 */
double calculate_drag_coefficient(double cd0, double cl,
                                  double aspect_ratio, double e) {
    if (aspect_ratio <= 0.0 || e <= 0.0) return cd0;
    return cd0 + (cl * cl) / (M_PI * aspect_ratio * e);
}

/**
 * @brief Computes the aerodynamic lift force.
 *
 * Formula: L = 0.5 * rho * V^2 * S * Cl.
 *
 * @param cl          Lift coefficient.
 * @param area        Wing area in m².
 * @param air_density Local air density in kg/m³.
 * @param velocity    True air speed in m/s.
 * @return Lift force in Newtons. Returns 0.0 for invalid inputs.
 */
double calculate_lift_force(double cl, double area,
                            double air_density, double velocity) {
    if (area <= 0.0 || air_density <= 0.0 || velocity < 0.0) return 0.0;
    return 0.5 * air_density * velocity * velocity * area * cl;
}

/**
 * @brief Computes the aerodynamic drag force.
 *
 * Formula: D = 0.5 * rho * V^2 * S * Cd.
 *
 * @param cd          Drag coefficient.
 * @param area        Wing area in m².
 * @param air_density Local air density in kg/m³.
 * @param velocity    True air speed in m/s.
 * @return Drag force in Newtons. Returns 0.0 for invalid inputs.
 */
double calculate_drag_force(double cd, double area,
                            double air_density, double velocity) {
    if (area <= 0.0 || air_density <= 0.0 || velocity < 0.0) return 0.0;
    return 0.5 * air_density * velocity * velocity * area * cd;
}

/**
 * @brief Computes the thrust produced by one engine at the given conditions.
 *
 * Formula O16: T = (T_stopped - lambda * mach) * (rho/rho0)^lapse_rate,
 * where lambda = (T_stopped - T_cruise) / 0.9.
 *
 * @param thrust_stopped Sea-level static thrust (Mach 0) in Newtons.
 * @param thrust_cruise  Sea-level cruise thrust (Mach 0.9) in Newtons.
 * @param lapse_rate     Altitude correction exponent.
 * @param mach           Current Mach number.
 * @param air_density    Local air density in kg/m³.
 * @return Thrust per engine in Newtons.
 */
double calculate_thrust(double thrust_stopped, double thrust_cruise,
                        double lapse_rate, double mach, double air_density) {
    double lambda = (thrust_stopped - thrust_cruise) / 0.9;
    double sea_level_thrust = thrust_stopped - lambda * mach;
    if (sea_level_thrust < 0.0) sea_level_thrust = 0.0;
    return sea_level_thrust * pow(air_density / ISA_RHO0, lapse_rate);
}

/**
 * @brief Computes the vertical climb rate for one time step.
 *
 * Formula Q16: dh/dt = (Thrust - Drag) * TAS / (mass * g).
 * A negative result means the aircraft is descending.
 *
 * @param total_thrust Total thrust from all engines in Newtons.
 * @param drag         Total drag force in Newtons.
 * @param tas          True air speed in m/s.
 * @param mass         Current aircraft mass in kg.
 * @return Climb rate in m/s (positive = climbing, negative = descending).
 */
double calculate_climb_rate(double total_thrust, double drag,
                            double tas, double mass) {
    if (mass <= 0.0 || tas <= 0.0) return 0.0;
    return (total_thrust - drag) * tas / (mass * ISA_G);
}

/**
 * @brief Computes the fuel mass burned during one time step.
 *
 * Formula S16: dW = Thrust * TSFC * dt / g.
 *
 * @param total_thrust Total thrust from all engines in Newtons.
 * @param tsfc         Thrust Specific Fuel Consumption in N/N/s.
 * @param dt           Time step duration in seconds.
 * @return Fuel burned in kg. Returns 0.0 for invalid inputs.
 */
double calculate_fuel_burn(double total_thrust, double tsfc, double dt) {
    if (total_thrust <= 0.0 || tsfc <= 0.0 || dt <= 0.0) return 0.0;
    return total_thrust * tsfc * dt / ISA_G;
}

/**
 * @brief Computes the horizontal ground distance covered in one time step.
 *
 * Formula T16: dist = TAS * cos(climb_angle) * dt.
 * Accounts for the fact that part of TAS goes into vertical movement.
 *
 * @param tas        True air speed in m/s.
 * @param climb_rate Vertical speed in m/s.
 * @param dt         Time step duration in seconds.
 * @return Horizontal distance in metres. Returns 0.0 for invalid inputs.
 */
double calculate_step_distance(double tas, double climb_rate, double dt) {
    if (tas <= 0.0 || dt <= 0.0) return 0.0;
    double sin_angle = climb_rate / tas;
    if (sin_angle >  1.0) sin_angle =  1.0;
    if (sin_angle < -1.0) sin_angle = -1.0;
    return tas * cos(asin(sin_angle)) * dt;
}
