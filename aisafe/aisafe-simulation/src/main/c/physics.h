#ifndef PHYSICS_H
#define PHYSICS_H

// ---------- ISA atmosphere ----------

// T = T0 + lambda*h  (h <= 11000 m),  T = 216.65 K above tropopause
double isa_temperature(double altitude_m);

// p = P0*(1 - 0.0065*h/T0)^5.2561  below tropopause
// p = 22632 * exp(-g*(h-11000)/(R*T_trop))  above
double isa_pressure(double altitude_m);

// rho = p / (R * T)
double isa_air_density(double altitude_m);

// a = sqrt(gamma * R * T)
double isa_speed_of_sound(double altitude_m);

// ---------- Speed ----------

// Mach from IAS (knots) and current air density.
// Formula (from sim spec, H16):
//   M = sqrt(5 * ((rho0/rho * ((1 + 0.2*(IAS/661.5)^2)^3.5 - 1) + 1)^0.2857 - 1))
double calculate_mach(double ias_knots, double air_density);

// TAS (m/s) = Mach * speed_of_sound_at_altitude
double calculate_tas(double mach, double altitude_m);

// ---------- Aerodynamics ----------

// Cl = 2*W / (rho * S * V^2)
double calculate_lift_coefficient(double mass, double area,
                                  double air_density, double velocity);

// Cd = Cd0 + Cl^2 / (pi * AR * e)
double calculate_drag_coefficient(double cd0, double cl,
                                  double aspect_ratio, double e);

// L = 0.5 * rho * V^2 * S * Cl
double calculate_lift_force(double cl, double area,
                            double air_density, double velocity);

// D = 0.5 * rho * V^2 * S * Cd
double calculate_drag_force(double cd, double area,
                            double air_density, double velocity);

// ---------- Thrust ----------

// Thrust for ONE engine (N).
// T = (thrust_stopped - lambda * mach) * (rho/rho0)^lapse_rate
// lambda is derived from the two sea-level reference points:
//   lambda = (thrust_stopped - thrust_cruise) / 0.9
double calculate_thrust(double thrust_stopped, double thrust_cruise,
                        double lapse_rate, double mach, double air_density);

// ---------- Step computations ----------

// dh/dt (m/s) = (total_thrust - drag) * tas / (mass * g)
double calculate_climb_rate(double total_thrust, double drag,
                            double tas, double mass);

// Fuel burned in one time step (kg).
// dW = total_thrust * tsfc * dt / g
// TSFC unit: N/N/s (weight-specific fuel consumption)
double calculate_fuel_burn(double total_thrust, double tsfc, double dt);

// Horizontal distance covered in one time step (m).
// dist = tas * cos(asin(climb_rate / tas)) * dt
double calculate_step_distance(double tas, double climb_rate, double dt);

#endif /* PHYSICS_H */
