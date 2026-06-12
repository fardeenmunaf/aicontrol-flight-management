#ifndef PHYSICS_H
#define PHYSICS_H

#include "structs.h"

/** @brief Returns the ISA temperature (K) at altitude_m metres above MSL. */
double isa_temperature(double altitude_m);

/** @brief Returns the ISA static pressure (Pa) at altitude_m metres above MSL. */
double isa_pressure(double altitude_m);

/** @brief Returns the ISA air density (kg/m³) at altitude_m metres above MSL. */
double isa_air_density(double altitude_m);

/** @brief Returns the ISA speed of sound (m/s) at altitude_m metres above MSL. */
double isa_speed_of_sound(double altitude_m);

/**
 * @brief Converts Indicated Air Speed to Mach number (formula H16).
 *
 * @param ias_knots   Indicated air speed in knots.
 * @param air_density Local air density in kg/m³.
 * @return Mach number. Returns 0.0 for invalid inputs.
 */
double calculate_mach(double ias_knots, double air_density);

/**
 * @brief Converts Mach number to True Air Speed (m/s).
 *
 * @param mach       Mach number.
 * @param altitude_m Altitude in metres (used to look up local speed of sound).
 * @return True air speed in m/s.
 */
double calculate_tas(double mach, double altitude_m);

/**
 * @brief Computes the lift coefficient: Cl = 2*W / (rho * S * V^2).
 *
 * @param mass        Aircraft mass in kg.
 * @param area        Wing area in m².
 * @param air_density Local air density in kg/m³.
 * @param velocity    True air speed in m/s.
 * @return Lift coefficient (dimensionless). Returns 0.0 for invalid inputs.
 */
double calculate_lift_coefficient(double mass, double area,
                                  double air_density, double velocity);

/**
 * @brief Computes the total drag coefficient: Cd = Cd0 + Cl^2 / (pi * AR * e).
 *
 * @param cd0          Zero-lift drag coefficient.
 * @param cl           Lift coefficient.
 * @param aspect_ratio Wing aspect ratio.
 * @param e            Oswald efficiency factor (0–1).
 * @return Drag coefficient (dimensionless).
 */
double calculate_drag_coefficient(double cd0, double cl,
                                  double aspect_ratio, double e);

/**
 * @brief Computes the lift force (N): L = 0.5 * rho * V^2 * S * Cl.
 *
 * @param cl          Lift coefficient.
 * @param area        Wing area in m².
 * @param air_density Local air density in kg/m³.
 * @param velocity    True air speed in m/s.
 * @return Lift force in Newtons. Returns 0.0 for invalid inputs.
 */
double calculate_lift_force(double cl, double area,
                            double air_density, double velocity);

/**
 * @brief Computes the drag force (N): D = 0.5 * rho * V^2 * S * Cd.
 *
 * @param cd          Drag coefficient.
 * @param area        Wing area in m².
 * @param air_density Local air density in kg/m³.
 * @param velocity    True air speed in m/s.
 * @return Drag force in Newtons. Returns 0.0 for invalid inputs.
 */
double calculate_drag_force(double cd, double area,
                            double air_density, double velocity);

/**
 * @brief Computes per-engine thrust (N) at the given flight conditions (formula O16).
 *
 * @param thrust_stopped Sea-level static thrust (Mach 0) in Newtons.
 * @param thrust_cruise  Sea-level cruise thrust (Mach 0.9) in Newtons.
 * @param lapse_rate     Altitude correction exponent.
 * @param mach           Current Mach number.
 * @param air_density    Local air density in kg/m³.
 * @return Thrust per engine in Newtons.
 */
double calculate_thrust(double thrust_stopped, double thrust_cruise,
                        double lapse_rate, double mach, double air_density);

/**
 * @brief Computes the vertical climb rate (m/s) for one time step (formula Q16).
 *
 * @param total_thrust Total thrust from all engines in Newtons.
 * @param drag         Total drag force in Newtons.
 * @param tas          True air speed in m/s.
 * @param mass         Current aircraft mass in kg.
 * @return Climb rate in m/s (positive = climbing, negative = descending).
 */
double calculate_climb_rate(double total_thrust, double drag,
                            double tas, double mass);

/**
 * @brief Computes fuel mass burned during one time step (kg) (formula S16).
 *
 * @param total_thrust Total thrust from all engines in Newtons.
 * @param tsfc         Thrust Specific Fuel Consumption in N/N/s.
 * @param dt           Time step duration in seconds.
 * @return Fuel burned in kg. Returns 0.0 for invalid inputs.
 */
double calculate_fuel_burn(double total_thrust, double tsfc, double dt);

/**
 * @brief Computes horizontal ground distance covered in one time step (m) (formula T16).
 *
 * @param tas        True air speed in m/s.
 * @param climb_rate Vertical speed in m/s.
 * @param dt         Time step duration in seconds.
 * @return Horizontal distance in metres. Returns 0.0 for invalid inputs.
 */
double calculate_step_distance(double tas, double climb_rate, double dt);

/**
 * @brief Returns the minimum 3D separation (m) between two flights over a time step.
 *
 * Models each flight as moving linearly from its previous position to its current
 * position. Finds the time t in [0,1] (normalised step duration) at which the
 * relative distance is smallest, using flat-earth Cartesian approximation.
 * This catches collisions that occur between discrete step samples.
 *
 * @param a0 Flight A position at step start.
 * @param a1 Flight A position at step end.
 * @param b0 Flight B position at step start.
 * @param b1 Flight B position at step end.
 * @return Minimum separation in metres over the step.
 */
double min_separation_m(const Position *a0, const Position *a1,
                        const Position *b0, const Position *b1);

/**
 * @brief Projects a position forward along heading_deg (degrees) by dist_m.
 *
 * Used by the reroute lookahead to extrapolate a flight's path. Altitude unchanged.
 *
 * @param p           Starting position.
 * @param heading_deg Bearing in degrees (0 = north, 90 = east).
 * @param dist_m      Distance to project in metres.
 * @return Projected position.
 */
/**
 * @brief Computes ground speed (m/s) and ground track (radians) from TAS, heading, and wind.
 *
 * Ground velocity = TAS vector (along heading_rad) + wind vector.
 * Wind speed is capped at 150 m/s internally.
 *
 * @param h_tas         Horizontal true air speed in m/s.
 * @param heading_rad   Aircraft heading in radians (north = 0, east = π/2).
 * @param wind_speed_ms Wind speed in m/s.
 * @param wind_dir_deg  Direction wind blows FROM in degrees (meteorological convention).
 * @param gs_out        Output: ground speed in m/s.
 * @param track_out     Output: ground track in radians (north = 0, east = π/2).
 */
void calculate_ground_velocity(double h_tas, double heading_rad,
                                double wind_speed_ms, double wind_dir_deg,
                                double *gs_out, double *track_out);

Position advance_pos(Position p, double heading_deg, double dist_m);

#endif /* PHYSICS_H */
