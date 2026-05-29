#ifndef PHYSICS_H
#define PHYSICS_H

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

#endif /* PHYSICS_H */
