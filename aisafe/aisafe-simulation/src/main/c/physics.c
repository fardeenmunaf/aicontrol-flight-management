#include "physics.h"
#include <math.h>

#ifndef M_PI
#define M_PI 3.14159265358979323846
#endif

// ── ISA (International Standard Atmosphere) constants ─────────────────────────
// These are ICAO standard values — the same numbers used in real aviation.
// They define how temperature, pressure, and density change with altitude.

#define ISA_T0          288.15    // K   temperature at sea level (15 °C)
#define ISA_LAMBDA       -0.0065  // K/m lapse rate: air cools 6.5 °C per km of climb
#define ISA_P0       101325.0     // Pa  pressure at sea level (1 atm)
#define ISA_RHO0          1.225   // kg/m³ air density at sea level
#define ISA_R           287.05    // J/(kg·K) specific gas constant for dry air
#define ISA_GAMMA         1.4     // ratio of specific heats (Cp/Cv) for air
#define ISA_G             9.81    // m/s² gravitational acceleration
#define ISA_TROPOPAUSE 11000.0    // m   altitude where the troposphere ends
#define ISA_T_TROP      216.65    // K   temperature above the tropopause (constant)
#define ISA_P_TROP     22632.0    // Pa  pressure at the tropopause base

// Speed of sound at sea level, in knots — used in the Mach formula
#define SL_SPEED_SOUND_KNOTS 661.5

// ── ISA atmosphere ─────────────────────────────────────────────────────────────

// Temperature drops linearly with altitude in the troposphere (0–11 km),
// then stays constant in the lower stratosphere.

double isa_temperature(double altitude_m) {
    if (altitude_m <= ISA_TROPOPAUSE)
        return ISA_T0 + ISA_LAMBDA * altitude_m;   // e.g. at 10 000 m: 288.15 - 65 = 223.15 K
    return ISA_T_TROP;                              // constant above 11 000 m
}

// Pressure falls with altitude using the barometric formula.
// Two formulas because the temperature profile changes at the tropopause.

double isa_pressure(double altitude_m) {
    if (altitude_m <= ISA_TROPOPAUSE)
        // Troposphere: P = P0 * (1 - 0.0065*h/T0)^5.2561
        return ISA_P0 * pow(1.0 - 0.0065 * altitude_m / ISA_T0, 5.2561);
    // Stratosphere: isothermal, so exponential decay
    return ISA_P_TROP * exp(-ISA_G * (altitude_m - ISA_TROPOPAUSE)
                            / (ISA_R * ISA_T_TROP));
}

// Air density from the ideal gas law: ρ = P / (R * T)
// Lower density at altitude means less lift and less engine thrust.

double isa_air_density(double altitude_m) {
    double T = isa_temperature(altitude_m);
    double p = isa_pressure(altitude_m);
    return p / (ISA_R * T);
}

// Speed of sound: a = √(γ * R * T)
// Colder air at altitude → lower speed of sound → aircraft reaches Mach 1 at lower TAS.

double isa_speed_of_sound(double altitude_m) {
    double T = isa_temperature(altitude_m);
    return sqrt(ISA_GAMMA * ISA_R * T);
}

// ── Speed conversions ──────────────────────────────────────────────────────────

// Converts IAS (what the cockpit shows, in knots) to Mach number.
// IAS is the "air-pressure equivalent" speed; it reads lower than TAS at altitude
// because the air is thinner. This formula corrects for that using air density.
// Formula H16 from the simulation spec:
//   M = sqrt(5 * ((ρ0/ρ * ((1 + 0.2*(IAS/661.5)²)^3.5 - 1) + 1)^(2/7) - 1))

double calculate_mach(double ias_knots, double air_density) {
    if (air_density <= 0.0 || ias_knots <= 0.0) return 0.0;

    // impact pressure ratio: how much the pitot tube is compressed vs sea level
    double impact_ratio = (ISA_RHO0 / air_density)
                          * (pow(1.0 + 0.2 * pow(ias_knots / SL_SPEED_SOUND_KNOTS, 2.0), 3.5) - 1.0)
                          + 1.0;
    return sqrt(5.0 * (pow(impact_ratio, 2.0 / 7.0) - 1.0));
}

// TAS (True Air Speed) = Mach × local speed of sound.
// This is the actual speed of the aircraft through the air mass.

double calculate_tas(double mach, double altitude_m) {
    return mach * isa_speed_of_sound(altitude_m);
}

// ── Aerodynamics ───────────────────────────────────────────────────────────────

// Lift coefficient: how hard the wing is working to produce lift.
//   Cl = 2*W / (ρ * S * V²)
// W = weight (mass × g), ρ = air density, S = wing area, V = TAS.
// Higher altitude (lower ρ) or lower speed → Cl must increase to maintain lift.

double calculate_lift_coefficient(double mass, double area,
                                  double air_density, double velocity) {
    if (area <= 0.0 || air_density <= 0.0 || velocity <= 0.0) return 0.0;
    return (2.0 * mass * ISA_G) / (air_density * area * velocity * velocity);
}

// Drag coefficient: total drag = zero-lift drag + induced drag.
//   Cd = Cd0 + Cl² / (π * AR * e)
// The second term is "induced drag" — drag created as a side-effect of producing lift.
// It grows with Cl², so slow/heavy flight = lots of induced drag.

double calculate_drag_coefficient(double cd0, double cl,
                                  double aspect_ratio, double e) {
    if (aspect_ratio <= 0.0 || e <= 0.0) return cd0;
    return cd0 + (cl * cl) / (M_PI * aspect_ratio * e);
}

// Lift force in Newtons: L = 0.5 * ρ * V² * S * Cl
// In steady level flight this equals the aircraft weight (mass × g).

double calculate_lift_force(double cl, double area,
                            double air_density, double velocity) {
    if (area <= 0.0 || air_density <= 0.0 || velocity < 0.0) return 0.0;
    return 0.5 * air_density * velocity * velocity * area * cl;
}

// Drag force in Newtons: D = 0.5 * ρ * V² * S * Cd
// The engine must overcome this to maintain speed.

double calculate_drag_force(double cd, double area,
                            double air_density, double velocity) {
    if (area <= 0.0 || air_density <= 0.0 || velocity < 0.0) return 0.0;
    return 0.5 * air_density * velocity * velocity * area * cd;
}

// ── Thrust ─────────────────────────────────────────────────────────────────────

// Per-engine thrust in Newtons. Formula O16 from the simulation spec.
//
// Step 1 — sea-level thrust slope:
//   lambda = (T_stopped - T_cruise) / 0.9
//   T_sea_level = T_stopped - lambda * Mach
//   This is a linear interpolation: at Mach 0 → T_stopped; at Mach 0.9 → T_cruise.
//
// Step 2 — altitude correction:
//   T = T_sea_level * (ρ / ρ0)^lapse_rate
//   Thinner air at altitude reduces thrust because the engine ingests less air mass.

double calculate_thrust(double thrust_stopped, double thrust_cruise,
                        double lapse_rate, double mach, double air_density) {
    double lambda = (thrust_stopped - thrust_cruise) / 0.9;
    double sea_level_thrust = thrust_stopped - lambda * mach;
    if (sea_level_thrust < 0.0) sea_level_thrust = 0.0;
    return sea_level_thrust * pow(air_density / ISA_RHO0, lapse_rate);
}

// ── Step integration ───────────────────────────────────────────────────────────

// Climb rate (m/s) — how fast the aircraft is going up or down. Formula Q16:
//   dh/dt = (Thrust - Drag) * TAS / (mass * g)
// Physics: excess thrust (Thrust > Drag) accelerates the aircraft; in the
// vertical direction this becomes a climb rate. If Thrust < Drag → descent.

double calculate_climb_rate(double total_thrust, double drag,
                            double tas, double mass) {
    if (mass <= 0.0 || tas <= 0.0) return 0.0;
    return (total_thrust - drag) * tas / (mass * ISA_G);
}

// Fuel burned in one time step (kg). Formula S16:
//   dW = Thrust * TSFC * dt / g
// TSFC (Thrust Specific Fuel Consumption) is in N/N/s:
//   for every 1 N of thrust sustained for 1 second, TSFC kg of fuel are burned.
// Dividing by g converts from weight-flow to mass-flow.

double calculate_fuel_burn(double total_thrust, double tsfc, double dt) {
    if (total_thrust <= 0.0 || tsfc <= 0.0 || dt <= 0.0) return 0.0;
    return total_thrust * tsfc * dt / ISA_G;
}

// Horizontal distance covered in one step (m). Formula T16:
//   dist = TAS * cos(climb_angle) * dt
// Not all of TAS goes forward — some goes upward (when climbing) or downward.
// The horizontal component = TAS * cos(asin(climb_rate / TAS)).

double calculate_step_distance(double tas, double climb_rate, double dt) {
    if (tas <= 0.0 || dt <= 0.0) return 0.0;
    double sin_angle = climb_rate / tas;             // sin of the climb angle
    if (sin_angle >  1.0) sin_angle =  1.0;         // clamp to valid asin range
    if (sin_angle < -1.0) sin_angle = -1.0;
    return tas * cos(asin(sin_angle)) * dt;          // horizontal component × time
}
