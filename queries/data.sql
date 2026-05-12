-- ============================================================
-- AISafe Bootstrap Data
-- Alinhado com o Domain Model
-- ============================================================

-- ============================================================
-- IDENTITY & ACCESS
-- ============================================================

INSERT INTO authz_user (username, password, first_name, last_name, email, enabled, roles)
VALUES
    ('admin@aisafe.com',   'Admin1234!',    'System',    'Admin',    'admin@aisafe.com',   TRUE, 'ADMIN'),
    ('bo@aisafe.com',      'Operator1234!', 'Backoffice','Operator', 'bo@aisafe.com',      TRUE, 'BACKOFFICE_OPERATOR'),
    ('pilot@aisafe.com',   'Pilot1234!',    'Default',   'Pilot',    'pilot@aisafe.com',   TRUE, 'PILOT'),
    ('fco@aisafe.com',     'Fco1234!',      'Default',   'FCO',      'fco@aisafe.com',     TRUE, 'FLIGHT_CONTROL_OPERATOR'),
    ('weather@aisafe.com', 'Weather1234!',  'Weather',   'Person',   'weather@aisafe.com', TRUE, 'WEATHER_PERSON'),
    ('atcc@aisafe.com',    'Atcc1234!',     'Company',   'Collab',   'atcc@aisafe.com',    TRUE, 'AIR_TRANSPORT_COMPANY_COLLABORATOR');

INSERT INTO user_entity (email, name, phone_number, status, user_id, role)
VALUES
    ('admin@aisafe.com',   'System Admin',         '+351910000001', 'ACTIVE', 'admin@aisafe.com',   'ADMIN'),
    ('bo@aisafe.com',      'Backoffice Operator',  '+351910000002', 'ACTIVE', 'bo@aisafe.com',      'BACKOFFICE_OPERATOR'),
    ('pilot@aisafe.com',   'Default Pilot',        '+351910000003', 'ACTIVE', 'pilot@aisafe.com',   'PILOT'),
    ('fco@aisafe.com',     'Default FCO',          '+351910000004', 'ACTIVE', 'fco@aisafe.com',     'FLIGHT_CONTROL_OPERATOR'),
    ('weather@aisafe.com', 'Weather Person',       '+351910000005', 'ACTIVE', 'weather@aisafe.com', 'WEATHER_PERSON'),
    ('atcc@aisafe.com',    'Company Collaborator', '+351910000006', 'ACTIVE', 'atcc@aisafe.com',    'AIR_TRANSPORT_COMPANY_COLLABORATOR');

-- ============================================================
-- TOPOGRAPHY
-- ============================================================

INSERT INTO air_control_area (area_code, minimum_required_arrival_fuel, geographic_boundaries)
VALUES
    ('LPPC', 2000.0, 'Portugal Continental'),
    ('LPPO', 1500.0, 'Açores'),
    ('GCCC', 1800.0, 'Canárias');

INSERT INTO airport (name, town, country, iata_code, icao_code, latitude, longitude, elevation, air_control_area_id)
VALUES
    ('Humberto Delgado',      'Lisboa',        'Portugal', 'LIS', 'LPPT',  38.7756, -9.1354,  113.0, 1),
    ('Francisco Sá Carneiro', 'Porto',         'Portugal', 'OPO', 'LPPR',  41.2481, -8.6814,   69.0, 1),
    ('Faro',                  'Faro',          'Portugal', 'FAO', 'LPFR',  37.0144, -7.9659,   24.0, 1),
    ('João Paulo II',         'Ponta Delgada', 'Portugal', 'PDL', 'LPPD',  37.7412,-25.6979,   82.0, 2),
    ('Gran Canaria',          'Las Palmas',    'Espanha',  'LPA', 'GCLP',  27.9319,-15.3866,   24.0, 3);

INSERT INTO node (id, designation, latitude, longitude)
VALUES
    ('NODE-001', 'LISBON-NORTH',    38.9000, -9.0000),
    ('NODE-002', 'PORTO-SOUTH',     41.0000, -8.5000),
    ('NODE-003', 'FARO-EAST',       37.1000, -7.5000),
    ('NODE-004', 'ATLANTIC-MID',    39.5000,-11.0000),
    ('NODE-005', 'AZORES-APPROACH', 38.0000,-25.0000);

-- ============================================================
-- ENGINE CATALOG
-- EngineModel com EngineMaker + FuelProperties embedded
-- ============================================================

INSERT INTO engine_model (name, engine_type, power, fuel_type, efficiency,
                          thrust_sea_level_stopped, thrust_sea_level_cruise,
                          thrust_specific_fuel_consumption, maker_name, maker_country,
                          fuel_density, fuel_specific_energy)
VALUES
    ('CFM56-7B',  'TURBOFAN', 27000.0, 'JET_A1', 0.85, 121000.0,  98000.0, 0.0370, 'CFM International', 'USA',           0.804, 42.80),
    ('V2500-A5',  'TURBOFAN', 25000.0, 'JET_A1', 0.83, 111000.0,  90000.0, 0.0360, 'IAE',               'International', 0.804, 42.80),
    ('GE90-115B', 'TURBOFAN', 51000.0, 'JET_A1', 0.88, 513000.0, 420000.0, 0.0320, 'GE Aviation',       'USA',           0.804, 42.80);

-- ============================================================
-- AIRCRAFT CATALOG
-- AircraftModel com AircraftMaker embedded
-- ============================================================

INSERT INTO aircraft_model (model_type, empty_weight, maximum_take_off_weight,
                            maximum_zero_fuel_weight, maximum_fuel_capacity, service_ceiling,
                            cruise_speed, wing_area, wing_span, drag_coefficient, lift_coefficient,
                            maker_name, maker_country)
VALUES
    ('PASSENGER',  41145.0,  79016.0,  62732.0,  26022.0, 12500.0, 230.0, 125.08, 35.8, 0.026, 1.5, 'Boeing', 'USA'),
    ('PASSENGER',  42600.0,  78000.0,  61000.0,  18728.0, 12131.0, 228.0, 122.60, 34.1, 0.024, 1.4, 'Airbus', 'France'),
    ('PASSENGER', 178756.0, 299370.0, 242670.0, 226117.0, 13137.0, 250.0, 541.16, 60.9, 0.022, 1.6, 'Boeing', 'USA');

INSERT INTO aircraft_model_engine_models (aircraft_model_id, engine_model_id)
VALUES (1, 1), (2, 2), (3, 3);

-- ============================================================
-- FLEET
-- AirTransportCompany --> users, aircraft, flightroutes
-- ============================================================

INSERT INTO air_transport_company (name, iata_code, icao_code)
VALUES
    ('TAP Air Portugal', 'TP', 'TAP'),
    ('Ryanair',          'FR', 'RYR');

INSERT INTO air_transport_company_users (company_id, user_id)
VALUES (1, 'atcc@aisafe.com'), (1, 'pilot@aisafe.com');

INSERT INTO aircraft (registration_number, registration_country, operational_status,
                      flight_crew_count, first_class_seats, business_class_seats, economy_class_seats,
                      aircraft_model_id, company_id)
VALUES
    ('CS-TUA', 'Portugal', 'ACTIVE', 2, 0, 12, 150, 1, 1),
    ('CS-TUB', 'Portugal', 'ACTIVE', 2, 0,  8, 162, 2, 1),
    ('EI-DAL', 'Ireland',  'ACTIVE', 2, 0,  0, 189, 1, 2);

INSERT INTO pilot (status, user_id)
VALUES ('ACTIVE', 'pilot@aisafe.com');

INSERT INTO pilot_certified_models (pilot_id, aircraft_model_id)
VALUES (1, 1), (1, 2);

-- ============================================================
-- FLIGHT PLANNING
-- FlightRoute --> company
-- Flight --> FlightRoute
-- FlightPlan --> Flight, Aircraft, Pilot
-- FlightLeg --> FlightPlan, Airports
-- Segment --> FlightLeg, Nodes
-- AltitudeSlot --> Segment
-- ============================================================

INSERT INTO flight_route (id, designation, active_from, active_until, company_id)
VALUES
    ('route-001', 'TP123', '2026-01-01', NULL, 1),
    ('route-002', 'TP456', '2026-01-01', NULL, 1),
    ('route-003', 'FR789', '2026-01-01', NULL, 2);

INSERT INTO flight (flight_designator, flight_type, scheduled_departure_time, scheduled_arrival_time, flight_route_id)
VALUES
    ('TP123A', 'REGULAR', '2026-05-10 08:00:00', '2026-05-10 09:00:00', 'route-001'),
    ('TP456B', 'REGULAR', '2026-05-10 14:00:00', '2026-05-10 15:30:00', 'route-002'),
    ('FR789C', 'CHARTER', '2026-05-11 07:00:00', '2026-05-11 09:00:00', 'route-003');

INSERT INTO flight_plan (id, departure_date_time, fuel_quantity, passenger_load,
                         cargo_load, plan_status, aircraft_id, pilot_id, flight_id)
VALUES
    ('fp-001', '2026-05-10 08:00:00', 15000.0, 150, 500.0, 'DRAFT', 'CS-TUA', 1, 'TP123A'),
    ('fp-002', '2026-05-10 14:00:00', 12000.0, 162, 300.0, 'DRAFT', 'CS-TUB', 1, 'TP456B');

INSERT INTO flight_leg (sequence_number, departure_time, arrival_time, fuel_quantity,
                        departure_airport_id, arrival_airport_id, flight_plan_id)
VALUES
    (1, '2026-05-10 08:00:00', '2026-05-10 09:00:00', 15000.0, 1, 2, 'fp-001'),
    (1, '2026-05-10 14:00:00', '2026-05-10 15:30:00', 12000.0, 2, 3, 'fp-002');

INSERT INTO segment (wind_direction, wind_speed, start_node_id, end_node_id, flight_leg_id)
VALUES
    (270.0, 15.5, 'NODE-001', 'NODE-002', 1),
    (260.0, 12.0, 'NODE-002', 'NODE-003', 2);

INSERT INTO altitude_slot (minimum_altitude, maximum_altitude, width, segment_id)
VALUES
    (8000.0, 12000.0, 50.0, 1),
    (7000.0, 11000.0, 50.0, 2);

-- ============================================================
-- WEATHER
-- ============================================================

INSERT INTO weather_data (weather_date, wind_speed, wind_direction, air_control_area_id)
VALUES
    ('2026-05-10', 15.5, 270.0, 1),
    ('2026-05-10', 12.0, 245.0, 2),
    ('2026-05-11', 20.0, 300.0, 1);

-- ============================================================
-- SIMULATION
-- Simulation --> AirControlArea, Flights (many-to-many)
-- AircraftPosition com VelocityVector embedded
-- SafetyViolation com VelocityVectors em tabela separada
-- ============================================================

INSERT INTO simulation (id, start_date_time, end_date_time, geographic_area,
                        safety_thresholds, performance_settings, sim_status, air_control_area_id)
VALUES
    ('sim-001', '2026-05-10 07:00:00', '2026-05-10 20:00:00',
     'Portugal Continental', '500m', 'default', 'PENDING', 1);

INSERT INTO simulation_flights (simulation_id, flight_designator)
VALUES
    ('sim-001', 'TP123A'),
    ('sim-001', 'TP456B');

INSERT INTO aircraft_position (pos_timestamp, altitude, speed, direction, aircraft_id, simulation_id)
VALUES
    ('2026-05-10 08:30:00', 10000.0, 230.0, 45.0, 'CS-TUA', 'sim-001'),
    ('2026-05-10 14:45:00',  9500.0, 228.0, 90.0, 'CS-TUB', 'sim-001');

INSERT INTO safety_violation (viol_timestamp, severity, simulation_id)
VALUES
    ('2026-05-10 08:35:00', 'LOW', 'sim-001');

INSERT INTO safety_violation_velocity_vectors (safety_violation_id, speed, direction)
VALUES
    (1, 230.0, 45.0),
    (1, 228.0, 90.0);

INSERT INTO simulation_report (total_flights, validation_result, generated_at, simulation_id)
VALUES
    (2, 'PENDING', '2026-05-02 18:00:00', 'sim-001');