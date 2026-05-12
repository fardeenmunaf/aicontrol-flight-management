-- ============================================================
-- AISafe Bootstrap Data
-- Alinhado com o Domain Model
-- ============================================================

-- ============================================================
-- IDENTITY & ACCESS (JPA tables)
-- ============================================================

INSERT INTO T_ROLESET (pk) VALUES (1), (2), (3), (4), (5), (6);

-- Ensure sequences continue after seeded IDs.
SELECT setval('T_ROLESET_SEQ', (SELECT MAX(pk) FROM T_ROLESET));

INSERT INTO T_ROLEASSIGNMENT (pk, assignedOn, expired, roleName, unassignedOn)
VALUES
    (1, '2026-05-12', FALSE, 'ADMIN', NULL),
    (2, '2026-05-12', FALSE, 'BACKOFFICE_OPERATOR', NULL),
    (3, '2026-05-12', FALSE, 'PILOT', NULL),
    (4, '2026-05-12', FALSE, 'FLIGHT_CONTROL_OPERATOR', NULL),
    (5, '2026-05-12', FALSE, 'WEATHER_PERSON', NULL),
    (6, '2026-05-12', FALSE, 'AIR_TRANSPORT_COMPANY_COLLABORATOR', NULL);

SELECT setval('T_ROLEASSIGNMENT_SEQ', (SELECT MAX(pk) FROM T_ROLEASSIGNMENT));

INSERT INTO T_ROLESET_T_ROLEASSIGNMENT (RoleSet_pk, assignments_pk)
VALUES
    (1, 1),
    (2, 2),
    (3, 3),
    (4, 4),
    (5, 5),
    (6, 6);

INSERT INTO T_SYSTEMUSER (username, active, createdOn, deactivatedOn, email, firstName, lastName, password, resetToken, roles_pk, version)
VALUES
    ('admin@aisafe.com',   TRUE, '2026-05-12', NULL, 'admin@aisafe.com',   'System',    'Admin',    'Admin1234!',    NULL, 1, 0),
    ('bo@aisafe.com',      TRUE, '2026-05-12', NULL, 'bo@aisafe.com',      'Backoffice','Operator', 'Operator1234!', NULL, 2, 0),
    ('pilot@aisafe.com',   TRUE, '2026-05-12', NULL, 'pilot@aisafe.com',   'Default',   'Pilot',    'Pilot1234!',    NULL, 3, 0),
    ('fco@aisafe.com',     TRUE, '2026-05-12', NULL, 'fco@aisafe.com',     'Default',   'FCO',      'Fco1234!',      NULL, 4, 0),
    ('weather@aisafe.com', TRUE, '2026-05-12', NULL, 'weather@aisafe.com', 'Weather',   'Person',   'Weather1234!',  NULL, 5, 0),
    ('atcc@aisafe.com',    TRUE, '2026-05-12', NULL, 'atcc@aisafe.com',    'Company',   'Collab',   'Atcc1234!',     NULL, 6, 0);

INSERT INTO AppUser (value, email, name, phoneNumber, status)
VALUES
    ('admin@aisafe.com',   'admin@aisafe.com',   'System Admin',         '+351910000001', 'ACTIVE'),
    ('bo@aisafe.com',      'bo@aisafe.com',      'Backoffice Operator',  '+351910000002', 'ACTIVE'),
    ('pilot@aisafe.com',   'pilot@aisafe.com',   'Default Pilot',        '+351910000003', 'ACTIVE'),
    ('fco@aisafe.com',     'fco@aisafe.com',     'Default FCO',          '+351910000004', 'ACTIVE'),
    ('weather@aisafe.com', 'weather@aisafe.com', 'Weather Person',       '+351910000005', 'ACTIVE'),
    ('atcc@aisafe.com',    'atcc@aisafe.com',    'Company Collaborator', '+351910000006', 'ACTIVE');

INSERT INTO user_roles (user_id, roles)
VALUES
    ('admin@aisafe.com',   'ADMIN'),
    ('admin@aisafe.com',   'BACKOFFICE_OPERATOR'),
    ('bo@aisafe.com',      'BACKOFFICE_OPERATOR'),
    ('pilot@aisafe.com',   'PILOT'),
    ('fco@aisafe.com',     'FLIGHT_CONTROL_OPERATOR'),
    ('weather@aisafe.com', 'WEATHER_PERSON'),
    ('atcc@aisafe.com',    'AIR_TRANSPORT_COMPANY_COLLABORATOR');

-- ============================================================
-- TOPOGRAPHY
-- ============================================================

INSERT INTO AirControlArea (airControlAreaId_value, areaCode, minimumRequiredArrivalFuel, geographicBoundaries, version)
VALUES
    ('aca-001', 'ACA-N', 500.0, 'NORTH_AREA', 0),
    ('aca-002', 'ACA-S', 600.0, 'SOUTH_AREA', 0),
    ('aca-003', 'ACA-E', 450.0, 'EAST_AREA', 0),
    ('aca-004', 'ACA-W', 550.0, 'WEST_AREA', 0);

INSERT INTO Airport (
    airport_id, name, town, country, iataCode, icaoCode, elevation,
    coordinates_latitude, coordinates_longitude, airControlArea_airControlAreaId_value, version
)
VALUES
    ('apt-001', 'Lisbon Portela', 'Lisbon', 'Portugal', 'LIS', 'LPPT', 114.0, 38.7742, -9.1342, 'aca-001', 0),
    ('apt-002', 'Porto', 'Porto', 'Portugal', 'OPO', 'LPPR', 69.0, 41.2356, -8.6781, 'aca-001', 0),
    ('apt-003', 'Heathrow', 'London', 'United Kingdom', 'LHR', 'EGLL', 83.0, 51.4700, -0.4543, 'aca-004', 0),
    ('apt-004', 'Frankfurt', 'Frankfurt', 'Germany', 'FRA', 'EDDF', 364.0, 50.0379, 8.5622, 'aca-003', 0);

INSERT INTO Node (nodeId_value, designation, coordinates_latitude, coordinates_longitude, version)
VALUES
    ('node-001', 'NORTH-1', 40.1234, -8.1234, 0),
    ('node-002', 'NORTH-2', 40.2234, -8.2234, 0),
    ('node-003', 'EAST-1',  39.9234, -7.9234, 0),
    ('node-004', 'WEST-1',  41.1234, -9.2234, 0);

-- ============================================================
-- ENGINE CATALOG
-- ============================================================

INSERT INTO EngineModel (
    engine_model_id, name, type, power, efficiency, thrustSeaLevelStopped, thrustSeaLevelCruise,
    thrustSpecificFuelConsumption, maker_name, maker_country, fuel_type,
    fuelProperties_density, fuelProperties_specificEnergy, version
)
VALUES
    ('eng-001', 'CFM56-7B', 'TURBOFAN', 12000.0, 0.85, 10500.0, 8200.0, 0.60, 'CFM', 'France', 'JET_A', 0.80, 43.0, 0),
    ('eng-002', 'PW4000',  'TURBOFAN', 16000.0, 0.88, 14000.0, 11000.0, 0.58, 'Pratt & Whitney', 'USA', 'JET_A', 0.81, 43.0, 0),
    ('eng-003', 'AE2100', 'TURBOPROP',  5200.0, 0.90,  4800.0,  4200.0, 0.65, 'Rolls-Royce', 'UK',  'JET_A', 0.79, 43.0, 0),
    ('eng-004', 'IO-550', 'PISTON',     300.0,  0.75,   260.0,   210.0, 0.75, 'Continental', 'USA', 'AVGAS', 0.72, 44.0, 0);

-- ============================================================
-- AIRCRAFT CATALOG
-- ============================================================

INSERT INTO AircraftModel (
    aircraftModelId_value, type, emptyWeight, maximumTakeOffWeight, maximumZeroFuelWeight,
    maximumFuelCapacity, serviceCeiling, cruiseSpeed, wingArea, wingSpan,
    dragCoefficient, liftCoefficient, numberOfEngines, modelName, maker_name, maker_country, version
)
VALUES
    ('am-001', 'PASSENGER', 41000.0, 77000.0, 61000.0, 21000.0, 12500.0, 828.0, 124.0, 35.8, 0.02, 0.50, 2, 'A320', 'Airbus', 'France', 0),
    ('am-002', 'PASSENGER', 42000.0, 79000.0, 62000.0, 22000.0, 12500.0, 840.0, 124.6, 35.9, 0.02, 0.50, 2, 'B737-800', 'Boeing', 'USA', 0),
    ('am-003', 'CARGO',     9000.0,  15000.0, 12000.0,  4000.0,  8500.0, 550.0,  58.0,  28.0, 0.03, 0.45, 2, 'ATR-72F', 'ATR', 'France', 0),
    ('am-004', 'MIXED',     1800.0,   3600.0,  2800.0,   800.0,  6000.0, 280.0,  16.2,  11.0, 0.04, 0.40, 1, 'C208B', 'Cessna', 'USA', 0);

INSERT INTO aircraftmodel_enginemodel (aircraft_model_id, engine_model_id)
VALUES
    ('am-001', 'eng-001'),
    ('am-002', 'eng-002'),
    ('am-003', 'eng-003'),
    ('am-004', 'eng-004');

-- ============================================================
-- FLEET
-- ============================================================

INSERT INTO AirTransportCompany (value, name, iataCode, icaoCode, version)
VALUES
    ('atc-001', 'TAP Air Portugal', 'TP', 'TAP', 0),
    ('atc-002', 'British Airways', 'BA', 'BAW', 0),
    ('atc-003', 'Lufthansa', 'LH', 'DLH', 0),
    ('atc-004', 'Air France', 'AF', 'AFR', 0);

INSERT INTO Aircraft (
    aircraftId_value, cabinConfiguration_economyClassSeats, cabinConfiguration_businessClassSeats,
    cabinConfiguration_firstClassSeats, aircraftModel_aircraftModelId_value,
    registrationNumber, registrationCountry, operationStatus, flightCrewCount, version
)
VALUES
    ('ac-001', 138, 24, 8,  'am-001', 'CS-TAA', 'PT', 'ACTIVE', 6, 0),
    ('ac-002', 150, 16, 0,  'am-002', 'G-BAAA', 'UK', 'ACTIVE', 6, 0),
    ('ac-003',  68,  0, 0,  'am-003', 'D-ATRF', 'DE', 'ACTIVE', 4, 0),
    ('ac-004',  12,  0, 0,  'am-004', 'F-CCES', 'FR', 'ACTIVE', 2, 0);

INSERT INTO Pilot (pilot_id, user_id, status, version)
VALUES
    ('pilot-001', 'pilot@aisafe.com', 'ACTIVE', 0),
    ('pilot-002', 'bo@aisafe.com',    'ACTIVE', 0),
    ('pilot-003', 'fco@aisafe.com',   'INACTIVE', 0),
    ('pilot-004', 'atcc@aisafe.com',  'ACTIVE', 0);

INSERT INTO Pilot_certifiedAircraftModels (Pilot_pilot_id, certifiedAircraftModels_aircraftModelId_value)
VALUES
    ('pilot-001', 'am-001'),
    ('pilot-001', 'am-002'),
    ('pilot-002', 'am-003'),
    ('pilot-004', 'am-004');

-- ============================================================
-- FLIGHT PLANNING
-- ============================================================

INSERT INTO FlightRoute (flightRouteId_value, designation_designation, activeFrom, activeUntil, version)
VALUES
    ('fr-001', 'TP123', '2026-05-01', NULL, 0),
    ('fr-002', 'BA45',  '2026-05-01', NULL, 0),
    ('fr-003', 'LH999', '2026-05-01', NULL, 0),
    ('fr-004', 'AF77',  '2026-05-01', NULL, 0);

INSERT INTO Flight (flightId_value, flightDesignator, type, scheduledDepartureTime, scheduledArrivalTime, version)
VALUES
    ('fl-001', 'TP1234', 'REGULAR', '2026-05-12 08:00:00', '2026-05-12 10:15:00', 0),
    ('fl-002', 'BA450',  'REGULAR', '2026-05-12 09:30:00', '2026-05-12 12:10:00', 0),
    ('fl-003', 'LH9991', 'CHARTER', '2026-05-12 13:00:00', '2026-05-12 16:20:00', 0),
    ('fl-004', 'AF770',  'REGULAR', '2026-05-12 15:45:00', '2026-05-12 18:05:00', 0);

INSERT INTO FlightPlan (flightPlanId_value, departureDateTime, fuelQuantity, passengerLoad, cargoLoad, status, version)
VALUES
    ('fp-001', '2026-05-12 08:00:00', 8200.0, 150, 1200.0, 'DRAFT', 0),
    ('fp-002', '2026-05-12 09:30:00', 7600.0, 140, 800.0, 'TESTED', 0),
    ('fp-003', '2026-05-12 13:00:00', 5400.0, 70, 500.0, 'DRAFT', 0),
    ('fp-004', '2026-05-12 15:45:00', 2200.0, 10, 200.0, 'VOID', 0);

INSERT INTO FlightLeg (id, sequenceNumber, departureTime, arrivalTime, fuelQuantity, departure_airport_id, arrival_airport_id, flight_plan_ref)
VALUES
    (1, 1, '2026-05-12 08:00:00', '2026-05-12 09:05:00', 4200.0, 'apt-001', 'apt-002', 'fp-001'),
    (2, 2, '2026-05-12 09:30:00', '2026-05-12 10:15:00', 4000.0, 'apt-002', 'apt-003', 'fp-001'),
    (3, 1, '2026-05-12 09:30:00', '2026-05-12 12:10:00', 7600.0, 'apt-003', 'apt-004', 'fp-002'),
    (4, 1, '2026-05-12 13:00:00', '2026-05-12 16:20:00', 5400.0, 'apt-001', 'apt-004', 'fp-003');

INSERT INTO Segment (id, windDirection, windSpeed, start_node_id, end_node_id, flight_leg_id)
VALUES
    (1, 45.0, 12.0, 'node-001', 'node-002', 1),
    (2, 90.0, 15.0, 'node-002', 'node-003', 2),
    (3, 120.0, 18.0, 'node-003', 'node-004', 3),
    (4, 200.0, 10.0, 'node-004', 'node-001', 4);

INSERT INTO AltitudeSlot (id, minimumAltitude, maximumAltitude, width, segment_id)
VALUES
    (1, 10000.0, 14000.0, 2000.0, 1),
    (2, 14000.0, 18000.0, 2000.0, 1),
    (3, 12000.0, 16000.0, 2000.0, 2),
    (4, 16000.0, 20000.0, 2000.0, 3);

-- ============================================================
-- WEATHER
-- ============================================================

INSERT INTO WeatherData (weatherDataId_value, airControlArea, dateTime, windSpeed, windDirection, temperature, pressure, version)
VALUES
    ('wd-001', 'ACA-N', '2026-05-12 06:00:00', 8.0, 45.0, 18.0, 1013.0, 0),
    ('wd-002', 'ACA-S', '2026-05-12 07:00:00', 5.0, 90.0, 22.0, 1010.0, 0),
    ('wd-003', 'ACA-E', '2026-05-12 08:00:00', 12.0, 120.0, 16.0, 1016.0, 0),
    ('wd-004', 'ACA-W', '2026-05-12 09:00:00', 10.0, 200.0, 14.0, 1012.0, 0);

-- ============================================================
-- SIMULATION
-- ============================================================

INSERT INTO Simulation (
    simulationId_value, startDateTime, endDateTime, geographicArea, safetyThresholds,
    performanceSettings, status, airControlArea_airControlAreaId_value, version
)
VALUES
    ('sim-001', '2026-05-12 08:00:00', '2026-05-12 10:30:00', 'AREA-N', 'STD', 'DEFAULT', 'CREATED', 'aca-001', 0),
    ('sim-002', '2026-05-12 09:00:00', '2026-05-12 12:30:00', 'AREA-W', 'STD', 'DEFAULT', 'RUNNING', 'aca-004', 0),
    ('sim-003', '2026-05-12 13:00:00', '2026-05-12 16:45:00', 'AREA-E', 'STD', 'DEFAULT', 'COMPLETED', 'aca-003', 0);

INSERT INTO Simulation_trackedFlights (Simulation_simulationId_value, trackedFlights_flightId_value)
VALUES
    ('sim-001', 'fl-001'),
    ('sim-001', 'fl-002'),
    ('sim-002', 'fl-003'),
    ('sim-003', 'fl-004');

INSERT INTO Simulation_capturedWeatherData (Simulation_simulationId_value, capturedWeatherData_weatherDataId_value)
VALUES
    ('sim-001', 'wd-001'),
    ('sim-001', 'wd-002'),
    ('sim-002', 'wd-003'),
    ('sim-003', 'wd-004');

INSERT INTO AircraftPosition (id, simulation_simulationId_value, timestamp, altitude, velocityVector_speed, velocityVector_direction)
VALUES
    (1, 'sim-001', '2026-05-12 08:15:00', 12000.0, 220.0, 45.0),
    (2, 'sim-001', '2026-05-12 08:45:00', 14000.0, 230.0, 60.0),
    (3, 'sim-002', '2026-05-12 09:30:00', 11000.0, 210.0, 90.0),
    (4, 'sim-003', '2026-05-12 14:00:00', 15000.0, 240.0, 120.0);

INSERT INTO SafetyViolation (id, simulation_simulationId_value, timestamp, severity)
VALUES
    (1, 'sim-001', '2026-05-12 09:05:00', 'MEDIUM'),
    (2, 'sim-003', '2026-05-12 15:20:00', 'HIGH');

INSERT INTO SafetyViolation_recordedVectors (SafetyViolation_id, recordedVectors_speed, recordedVectors_direction)
VALUES
    (1, 240.0, 50.0),
    (1, 245.0, 55.0),
    (2, 260.0, 110.0),
    (2, 255.0, 115.0);

INSERT INTO SafetyViolation_involvedAircraft (SafetyViolation_id, involvedAircraft_aircraftId_value)
VALUES
    (1, 'ac-001'),
    (1, 'ac-002'),
    (2, 'ac-003'),
    (2, 'ac-004');

INSERT INTO SimulationReport (simulation_report_id, totalFlights, validationResult, generatedAt, simulation_id, version)
VALUES
    ('sr-001', 2, 'PENDING', '2026-05-12 10:35:00', 'sim-001', 0),
    ('sr-002', 1, 'PASSED',  '2026-05-12 12:35:00', 'sim-002', 0),
    ('sr-003', 1, 'FAILED',  '2026-05-12 16:50:00', 'sim-003', 0);