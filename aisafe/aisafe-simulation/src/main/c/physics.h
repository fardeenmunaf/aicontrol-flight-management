#ifndef PHYSICS_H
#define PHYSICS_H

#define PHYSICS_GRAVITY 9.80665

/**
 * @brief Calculates the lift force acting on the aircraft.
 * 
 * @param cl Lift coefficient
 * @param area Area of the wings (m^2)
 * @param air_density Density of the air (kg/m^3)
 * @param velocity Velocity of the aircraft relative to the air (m/s)
 * @return Lift force (N)
 */
double calculate_lift_force(double cl, double area, double air_density, double velocity);

/**
 * @brief Calculates the drag force acting on the aircraft.
 * 
 * @param cd Drag coefficient
 * @param area Area of the wings (m^2)
 * @param air_density Density of the air (kg/m^3)
 * @param velocity Velocity of the aircraft relative to the air (m/s)
 * @return Drag force (N)
 */
double calculate_drag_force(double cd, double area, double air_density, double velocity);

/**
 * @brief Calculates the lift coefficient.
 * 
 * @param mass Mass of the aircraft (kg)
 * @param area Area of the wings (m^2)
 * @param air_density Density of the air (kg/m^3)
 * @param velocity Velocity of the aircraft relative to the air (m/s)
 * @return Lift coefficient
 */
double calculate_lift_coefficient(double mass, double area, double air_density, double velocity);

/**
 * @brief Calculates the drag coefficient.
 * 
 * @param cd0 Zero-lift drag coefficient
 * @param cl Lift coefficient
 * @param aspect_ratio Aspect ratio of the wing (wing span^2 / wing area)
 * @param e Oswald efficiency number
 * @return Drag coefficient
 */
double calculate_drag_coefficient(double cd0, double cl, double aspect_ratio, double e);

/**
 * @brief Calculates the engine thrust at a specific altitude.
 * 
 * @param thrust_sea_level Thrust at sea level (N)
 * @param density_altitude Air density at current altitude (kg/m^3)
 * @param density_sea_level Air density at sea level (kg/m^3)
 * @return Thrust at the given altitude (N)
 */
double calculate_thrust_at_altitude(double thrust_sea_level, double density_altitude, double density_sea_level);

/**
 * @brief Calculates the range of the aircraft in cruise flight.
 * 
 * @param true_air_speed True air speed (m/s)
 * @param tsfc Thrust specific fuel consumption (N/N/s or kg/N/s depending on convention, using SI units)
 * @param lift_force Lift force (N)
 * @param drag_force Drag force (N)
 * @param initial_weight Initial weight of the aircraft (N)
 * @param final_weight Final weight of the aircraft (N)
 * @return Range of the aircraft (m)
 */
double calculate_range(double true_air_speed, double tsfc, double lift_force, double drag_force, double initial_weight, double final_weight);

#endif /* PHYSICS_H */
