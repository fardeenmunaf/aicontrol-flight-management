-- ============================================================
-- AISafe Database Schema â€” JPA-aligned names
-- PostgreSQL
-- ============================================================

DROP TABLE IF EXISTS SafetyViolation_recordedVectors CASCADE;
DROP TABLE IF EXISTS SafetyViolation_involvedAircraft CASCADE;
DROP TABLE IF EXISTS Simulation_capturedWeatherData CASCADE;
DROP TABLE IF EXISTS Simulation_trackedFlights CASCADE;
DROP TABLE IF EXISTS Pilot_certifiedAircraftModels CASCADE;
DROP TABLE IF EXISTS aircraftmodel_enginemodel CASCADE;
DROP TABLE IF EXISTS AltitudeSlot CASCADE;
DROP TABLE IF EXISTS Segment CASCADE;
DROP TABLE IF EXISTS FlightLeg CASCADE;
DROP TABLE IF EXISTS FlightPlan CASCADE;
DROP TABLE IF EXISTS Flight CASCADE;
DROP TABLE IF EXISTS FlightRoute CASCADE;
DROP TABLE IF EXISTS AircraftPosition CASCADE;
DROP TABLE IF EXISTS SafetyViolation CASCADE;
DROP TABLE IF EXISTS Simulation CASCADE;
DROP TABLE IF EXISTS SimulationReport CASCADE;
DROP TABLE IF EXISTS WeatherData CASCADE;
DROP TABLE IF EXISTS Aircraft CASCADE;
DROP TABLE IF EXISTS Pilot CASCADE;
DROP TABLE IF EXISTS AircraftModel CASCADE;
DROP TABLE IF EXISTS EngineModel CASCADE;
DROP TABLE IF EXISTS AirTransportCompany CASCADE;
DROP TABLE IF EXISTS Airport CASCADE;
DROP TABLE IF EXISTS AirControlArea CASCADE;
DROP TABLE IF EXISTS Node CASCADE;
DROP TABLE IF EXISTS T_ROLESET_T_ROLEASSIGNMENT CASCADE;
DROP TABLE IF EXISTS T_ROLEASSIGNMENT CASCADE;
DROP TABLE IF EXISTS T_ROLESET CASCADE;
DROP TABLE IF EXISTS T_SYSTEMUSER CASCADE;
DROP TABLE IF EXISTS user_roles CASCADE;
DROP TABLE IF EXISTS AppUser CASCADE;

-- ============================================================
-- IDENTITY & ACCESS
-- ============================================================

CREATE TABLE AppUser (
                         value                         VARCHAR(255) PRIMARY KEY,
                         email                         VARCHAR(255),
                         name                          VARCHAR(255),
                         phoneNumber                   VARCHAR(255),
                         activeSecurityClearanceExpiry DATE,
                         skillsAssessmentDueDate       DATE,
                         status                        VARCHAR(255)
);

CREATE TABLE user_roles (
                            user_id VARCHAR(255) NOT NULL REFERENCES AppUser(value),
                            roles   VARCHAR(255) NOT NULL,
                            PRIMARY KEY (user_id, roles)
);

CREATE TABLE T_ROLESET (
                           pk BIGINT PRIMARY KEY
);

CREATE TABLE T_ROLEASSIGNMENT (
                                  pk          BIGINT PRIMARY KEY,
                                  assignedOn  DATE,
                                  expired     BOOLEAN NOT NULL,
                                  roleName    VARCHAR(255),
                                  unassignedOn DATE
);

CREATE TABLE T_ROLESET_T_ROLEASSIGNMENT (
                                            RoleSet_pk     BIGINT NOT NULL REFERENCES T_ROLESET(pk),
                                            assignments_pk BIGINT NOT NULL REFERENCES T_ROLEASSIGNMENT(pk),
                                            PRIMARY KEY (RoleSet_pk, assignments_pk)
);

CREATE TABLE T_SYSTEMUSER (
                              username      VARCHAR(255) PRIMARY KEY,
                              active        BOOLEAN NOT NULL,
                              createdOn     DATE,
                              deactivatedOn DATE,
                              email         VARCHAR(255),
                              firstName     VARCHAR(255),
                              lastName      VARCHAR(255),
                              password      VARCHAR(255),
                              resetToken    VARCHAR(255),
                              roles_pk      BIGINT NOT NULL REFERENCES T_ROLESET(pk),
                              version       BIGINT
);

CREATE SEQUENCE T_ROLESET_SEQ START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE T_ROLEASSIGNMENT_SEQ START WITH 1 INCREMENT BY 50;

-- ============================================================
-- TOPOGRAPHY
-- ============================================================

CREATE TABLE AirControlArea (
    value                      VARCHAR(255) PRIMARY KEY,
    areaCode                   VARCHAR(255) NOT NULL,
    minimumRequiredArrivalFuel DOUBLE PRECISION NOT NULL,
    geographicBoundaries       TEXT NOT NULL,
    version                    BIGINT
);

CREATE TABLE Airport (
    airport_id                          VARCHAR(255) PRIMARY KEY,
    name                                VARCHAR(255) NOT NULL,
    town                                VARCHAR(255) NOT NULL,
    country                             VARCHAR(255) NOT NULL,
    iataCode                            VARCHAR(255) NOT NULL,
    icaoCode                            VARCHAR(255) NOT NULL,
    elevation                           DOUBLE PRECISION NOT NULL,
    coordinates_latitude                DOUBLE PRECISION NOT NULL,
    coordinates_longitude               DOUBLE PRECISION NOT NULL,
    airControlArea_value                VARCHAR(255) NOT NULL REFERENCES AirControlArea(value),
    version                             BIGINT
);

CREATE TABLE Node (
                      nodeId_value          VARCHAR(255) PRIMARY KEY,
                      designation           VARCHAR(255) NOT NULL,
                      coordinates_latitude  DOUBLE PRECISION NOT NULL,
                      coordinates_longitude DOUBLE PRECISION NOT NULL,
                      version               BIGINT
);

-- ============================================================
-- ENGINE CATALOG
-- ============================================================

CREATE TABLE EngineModel (
                             engine_model_id                     VARCHAR(255) PRIMARY KEY,
                             name                                VARCHAR(255) NOT NULL,
                             type                                VARCHAR(255),
                             power                               DOUBLE PRECISION,
                             efficiency                          DOUBLE PRECISION,
                             thrustSeaLevelStopped               DOUBLE PRECISION,
                             thrustSeaLevelCruise                DOUBLE PRECISION,
                             thrustSpecificFuelConsumption       DOUBLE PRECISION,
                             maker_name                          VARCHAR(255) NOT NULL,
                             maker_country                       VARCHAR(255) NOT NULL,
                             fuel_type                           VARCHAR(255),
                             fuelProperties_density              DOUBLE PRECISION,
                             fuelProperties_specificEnergy       DOUBLE PRECISION,
                             version                             BIGINT,
                             UNIQUE (name, maker_name)
);

-- ============================================================
-- AIRCRAFT CATALOG
-- ============================================================

CREATE TABLE AircraftModel (
                               aircraftModelId_value       VARCHAR(255) PRIMARY KEY,
                               type                        VARCHAR(255),
                               emptyWeight                 DOUBLE PRECISION,
                               maximumTakeOffWeight        DOUBLE PRECISION,
                               maximumZeroFuelWeight       DOUBLE PRECISION,
                               maximumFuelCapacity         DOUBLE PRECISION,
                               serviceCeiling              DOUBLE PRECISION,
                               cruiseSpeed                 DOUBLE PRECISION,
                               wingArea                    DOUBLE PRECISION,
                               wingSpan                    DOUBLE PRECISION,
                               dragCoefficient             DOUBLE PRECISION,
                               liftCoefficient             DOUBLE PRECISION,
                               numberOfEngines             INT,
                               modelName                   VARCHAR(255),
                               maker_name                  VARCHAR(255),
                               maker_country               VARCHAR(255),
                               version                     BIGINT
);

CREATE TABLE aircraftmodel_enginemodel (
                                           aircraft_model_id VARCHAR(255) NOT NULL REFERENCES AircraftModel(aircraftModelId_value),
                                           engine_model_id   VARCHAR(255) NOT NULL REFERENCES EngineModel(engine_model_id),
                                           PRIMARY KEY (aircraft_model_id, engine_model_id)
);

-- ============================================================
-- FLEET
-- ============================================================

CREATE TABLE AirTransportCompany (
                                     value   VARCHAR(255) PRIMARY KEY,
                                     name    VARCHAR(255),
                                     iataCode VARCHAR(255),
                                     icaoCode VARCHAR(255),
                                     version BIGINT
);

CREATE TABLE Aircraft (
                          aircraftId_value                        VARCHAR(255) PRIMARY KEY,
                          cabinConfiguration_economyClassSeats    INT,
                          cabinConfiguration_businessClassSeats   INT,
                          cabinConfiguration_firstClassSeats      INT,
                          aircraftModel_aircraftModelId_value     VARCHAR(255) REFERENCES AircraftModel(aircraftModelId_value),
                          registrationNumber                      VARCHAR(255),
                          registrationCountry                     VARCHAR(255),
                          operationStatus                         VARCHAR(255),
                          flightCrewCount                          INT,
                          version                                 BIGINT
);

CREATE TABLE Pilot (
                       pilot_id VARCHAR(255) PRIMARY KEY,
                       user_id  VARCHAR(255),
                       status   VARCHAR(255),
                       version  BIGINT
);

CREATE TABLE Pilot_certifiedAircraftModels (
                                               Pilot_pilot_id                            VARCHAR(255) NOT NULL REFERENCES Pilot(pilot_id),
                                               certifiedAircraftModels_aircraftModelId_value VARCHAR(255) NOT NULL REFERENCES AircraftModel(aircraftModelId_value),
                                               PRIMARY KEY (Pilot_pilot_id, certifiedAircraftModels_aircraftModelId_value)
);

-- ============================================================
-- FLIGHT PLANNING
-- ============================================================

CREATE TABLE FlightRoute (
                             flightRouteId_value   VARCHAR(255) PRIMARY KEY,
                             designation_designation VARCHAR(255),
                             activeFrom            DATE,
                             activeUntil           DATE,
                             version               BIGINT
);

CREATE TABLE Flight (
                        flightId_value          VARCHAR(255) PRIMARY KEY,
                        flightDesignator        VARCHAR(255),
                        type                    VARCHAR(255),
                        scheduledDepartureTime  TIMESTAMP,
                        scheduledArrivalTime    TIMESTAMP,
                        version                 BIGINT
);

CREATE TABLE FlightPlan (
                            flightPlanId_value   VARCHAR(255) PRIMARY KEY,
                            departureDateTime    TIMESTAMP,
                            fuelQuantity         DOUBLE PRECISION,
                            passengerLoad        INT,
                            cargoLoad            DOUBLE PRECISION,
                            status               VARCHAR(255),
                            version              BIGINT
);

CREATE TABLE FlightLeg (
                           id                   BIGSERIAL PRIMARY KEY,
                           sequenceNumber       INT,
                           departureTime        TIMESTAMP,
                           arrivalTime          TIMESTAMP,
                           fuelQuantity         DOUBLE PRECISION,
                           departure_airport_id VARCHAR(255),
                           arrival_airport_id   VARCHAR(255),
                           flight_plan_ref      VARCHAR(255) REFERENCES FlightPlan(flightPlanId_value)
);

CREATE TABLE Segment (
                         id             BIGSERIAL PRIMARY KEY,
                         windDirection  DOUBLE PRECISION,
                         windSpeed      DOUBLE PRECISION,
                         start_node_id  VARCHAR(255),
                         end_node_id    VARCHAR(255),
                         flight_leg_id  BIGINT REFERENCES FlightLeg(id)
);

CREATE TABLE AltitudeSlot (
                              id               BIGSERIAL PRIMARY KEY,
                              minimumAltitude  DOUBLE PRECISION,
                              maximumAltitude  DOUBLE PRECISION,
                              width            DOUBLE PRECISION,
                              segment_id       BIGINT REFERENCES Segment(id)
);

-- ============================================================
-- WEATHER
-- ============================================================

CREATE TABLE WeatherData (
                             weatherDataId_value VARCHAR(255) PRIMARY KEY,
                             airControlArea      VARCHAR(255),
                             dateTime            TIMESTAMP,
                             windSpeed           DOUBLE PRECISION,
                             windDirection       DOUBLE PRECISION,
                             temperature         DOUBLE PRECISION,
                             pressure            DOUBLE PRECISION,
                             version             BIGINT
);

-- ============================================================
-- SIMULATION
-- ============================================================

CREATE TABLE Simulation (
    simulationId_value              VARCHAR(255) PRIMARY KEY,
    startDateTime                   TIMESTAMP,
    endDateTime                     TIMESTAMP,
    geographicArea                  TEXT,
    safetyThresholds                TEXT,
    performanceSettings             TEXT,
    status                          VARCHAR(255),
    airControlArea_value            VARCHAR(255) REFERENCES AirControlArea(value),
    version                         BIGINT
);

CREATE TABLE Simulation_trackedFlights (
                                           Simulation_simulationId_value VARCHAR(255) NOT NULL REFERENCES Simulation(simulationId_value),
                                           trackedFlights_flightId_value VARCHAR(255) NOT NULL REFERENCES Flight(flightId_value),
                                           PRIMARY KEY (Simulation_simulationId_value, trackedFlights_flightId_value)
);

CREATE TABLE Simulation_capturedWeatherData (
                                                Simulation_simulationId_value        VARCHAR(255) NOT NULL REFERENCES Simulation(simulationId_value),
                                                capturedWeatherData_weatherDataId_value VARCHAR(255) NOT NULL REFERENCES WeatherData(weatherDataId_value),
                                                PRIMARY KEY (Simulation_simulationId_value, capturedWeatherData_weatherDataId_value)
);

CREATE TABLE AircraftPosition (
                                  id                      BIGSERIAL PRIMARY KEY,
                                  simulation_simulationId_value VARCHAR(255) REFERENCES Simulation(simulationId_value),
                                  timestamp               TIMESTAMP,
                                  altitude                DOUBLE PRECISION,
                                  velocityVector_speed    DOUBLE PRECISION,
                                  velocityVector_direction DOUBLE PRECISION
);

CREATE TABLE SafetyViolation (
                                 id                      BIGSERIAL PRIMARY KEY,
                                 simulation_simulationId_value VARCHAR(255) REFERENCES Simulation(simulationId_value),
                                 timestamp               TIMESTAMP,
                                 severity                VARCHAR(255)
);

CREATE TABLE SafetyViolation_recordedVectors (
                                                 SafetyViolation_id       BIGINT NOT NULL REFERENCES SafetyViolation(id),
                                                 recordedVectors_speed    DOUBLE PRECISION NOT NULL,
                                                 recordedVectors_direction DOUBLE PRECISION NOT NULL,
                                                 PRIMARY KEY (SafetyViolation_id, recordedVectors_speed, recordedVectors_direction)
);

CREATE TABLE SafetyViolation_involvedAircraft (
                                                  SafetyViolation_id            BIGINT NOT NULL REFERENCES SafetyViolation(id),
                                                  involvedAircraft_aircraftId_value VARCHAR(255) NOT NULL REFERENCES Aircraft(aircraftId_value),
                                                  PRIMARY KEY (SafetyViolation_id, involvedAircraft_aircraftId_value)
);

CREATE TABLE SimulationReport (
                                  simulation_report_id VARCHAR(255) PRIMARY KEY,
                                  totalFlights         INT,
                                  validationResult     VARCHAR(255),
                                  generatedAt          TIMESTAMP,
                                  simulation_id        VARCHAR(255),
                                  version              BIGINT
);