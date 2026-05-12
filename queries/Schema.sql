-- ============================================================
-- AISafe Database Schema — alinhado com o Domain Model
-- PostgreSQL
-- ============================================================

DROP TABLE IF EXISTS safety_violation_velocity_vectors CASCADE;
DROP TABLE IF EXISTS aircraft_position_velocity_vector CASCADE;
DROP TABLE IF EXISTS simulation_flights CASCADE;
DROP TABLE IF EXISTS safety_violation CASCADE;
DROP TABLE IF EXISTS aircraft_position CASCADE;
DROP TABLE IF EXISTS simulation CASCADE;
DROP TABLE IF EXISTS simulation_report CASCADE;
DROP TABLE IF EXISTS altitude_slot CASCADE;
DROP TABLE IF EXISTS segment CASCADE;
DROP TABLE IF EXISTS flight_leg CASCADE;
DROP TABLE IF EXISTS flight_plan CASCADE;
DROP TABLE IF EXISTS flight_route_scheduled_flights CASCADE;
DROP TABLE IF EXISTS flight_route CASCADE;
DROP TABLE IF EXISTS flight CASCADE;
DROP TABLE IF EXISTS pilot_certified_models CASCADE;
DROP TABLE IF EXISTS pilot CASCADE;
DROP TABLE IF EXISTS aircraft CASCADE;
DROP TABLE IF EXISTS aircraft_model_engine_models CASCADE;
DROP TABLE IF EXISTS aircraft_model CASCADE;
DROP TABLE IF EXISTS engine_model CASCADE;
DROP TABLE IF EXISTS air_transport_company_users CASCADE;
DROP TABLE IF EXISTS air_transport_company CASCADE;
DROP TABLE IF EXISTS weather_data CASCADE;
DROP TABLE IF EXISTS airport CASCADE;
DROP TABLE IF EXISTS air_control_area CASCADE;
DROP TABLE IF EXISTS node CASCADE;
DROP TABLE IF EXISTS authz_user CASCADE;
DROP TABLE IF EXISTS user_entity CASCADE;

-- ============================================================
-- IDENTITY & ACCESS
-- User Aggregate: User + Role
-- ============================================================

CREATE TABLE authz_user (
                            username   VARCHAR(255) PRIMARY KEY,
                            password   VARCHAR(255) NOT NULL,
                            first_name VARCHAR(255) NOT NULL,
                            last_name  VARCHAR(255) NOT NULL,
                            email      VARCHAR(255) NOT NULL UNIQUE,
                            enabled    BOOLEAN      NOT NULL DEFAULT TRUE,
                            roles      VARCHAR(500)
);

CREATE TABLE user_entity (
                             id                               BIGSERIAL    PRIMARY KEY,
                             email                            VARCHAR(255) NOT NULL UNIQUE,
                             name                             VARCHAR(255) NOT NULL,
                             phone_number                     VARCHAR(50)  NOT NULL,
                             active_security_clearance_expiry DATE,
                             skills_assessment_due_date       DATE,
                             status                           VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
                             user_id                          VARCHAR(255) NOT NULL,
                             role                             VARCHAR(50)  NOT NULL
);

-- ============================================================
-- TOPOGRAPHY
-- AirControlArea Aggregate
-- Airport Aggregate (Coordinates embedded)
-- Node Aggregate (Coordinates embedded)
-- ============================================================

CREATE TABLE air_control_area (
                                  id                            BIGSERIAL        PRIMARY KEY,
                                  area_code                     VARCHAR(10)      NOT NULL UNIQUE,
                                  minimum_required_arrival_fuel DOUBLE PRECISION NOT NULL,
                                  geographic_boundaries         TEXT             NOT NULL
);

CREATE TABLE airport (
                         id                  BIGSERIAL        PRIMARY KEY,
                         name                VARCHAR(255)     NOT NULL,
                         town                VARCHAR(255)     NOT NULL,
                         country             VARCHAR(255)     NOT NULL,
                         iata_code           VARCHAR(3)       NOT NULL UNIQUE,
                         icao_code           VARCHAR(4)       NOT NULL UNIQUE,
                         latitude            DOUBLE PRECISION NOT NULL,
                         longitude           DOUBLE PRECISION NOT NULL,
                         elevation           DOUBLE PRECISION NOT NULL,
                         air_control_area_id BIGINT           NOT NULL REFERENCES air_control_area(id)
);

CREATE TABLE node (
                      id          VARCHAR(255)     PRIMARY KEY,
                      designation VARCHAR(255)     NOT NULL,
                      latitude    DOUBLE PRECISION NOT NULL,
                      longitude   DOUBLE PRECISION NOT NULL
);

-- ============================================================
-- ENGINE CATALOG
-- EngineModel Aggregate (EngineMaker + FuelProperties embedded)
-- ============================================================

CREATE TABLE engine_model (
                              id                               BIGSERIAL        PRIMARY KEY,
                              name                             VARCHAR(255)     NOT NULL,
                              engine_type                      VARCHAR(100)     NOT NULL,
                              power                            DOUBLE PRECISION NOT NULL,
                              fuel_type                        VARCHAR(100)     NOT NULL,
                              efficiency                       DOUBLE PRECISION NOT NULL,
                              thrust_sea_level_stopped         DOUBLE PRECISION NOT NULL,
                              thrust_sea_level_cruise          DOUBLE PRECISION NOT NULL,
                              thrust_specific_fuel_consumption DOUBLE PRECISION NOT NULL,
                              maker_name                       VARCHAR(255)     NOT NULL,
                              maker_country                    VARCHAR(255)     NOT NULL,
                              fuel_density                     DOUBLE PRECISION,
                              fuel_specific_energy             DOUBLE PRECISION
);

-- ============================================================
-- AIRCRAFT CATALOG
-- AircraftModel Aggregate (AircraftMaker embedded)
-- AircraftModel --> EngineModel (cross-aggregate, many-to-many)
-- ============================================================

CREATE TABLE aircraft_model (
                                id                       BIGSERIAL        PRIMARY KEY,
                                model_type               VARCHAR(100)     NOT NULL,
                                empty_weight             DOUBLE PRECISION NOT NULL,
                                maximum_take_off_weight  DOUBLE PRECISION NOT NULL,
                                maximum_zero_fuel_weight DOUBLE PRECISION NOT NULL,
                                maximum_fuel_capacity    DOUBLE PRECISION NOT NULL,
                                service_ceiling          DOUBLE PRECISION NOT NULL,
                                cruise_speed             DOUBLE PRECISION NOT NULL,
                                wing_area                DOUBLE PRECISION NOT NULL,
                                wing_span                DOUBLE PRECISION NOT NULL,
                                drag_coefficient         DOUBLE PRECISION NOT NULL,
                                lift_coefficient         DOUBLE PRECISION NOT NULL,
                                maker_name               VARCHAR(255)     NOT NULL,
                                maker_country            VARCHAR(255)     NOT NULL
);

CREATE TABLE aircraft_model_engine_models (
                                              aircraft_model_id BIGINT NOT NULL REFERENCES aircraft_model(id),
                                              engine_model_id   BIGINT NOT NULL REFERENCES engine_model(id),
                                              PRIMARY KEY (aircraft_model_id, engine_model_id)
);

-- ============================================================
-- FLEET
-- AirTransportCompany Aggregate
-- AirTransportCompany --> User (employs)
-- AirTransportCompany --> Aircraft (owns)
-- AirTransportCompany --> FlightRoute (operates) — FK na FlightRoute
-- Aircraft Aggregate (CabinConfiguration embedded)
-- Aircraft --> AircraftModel (cross-aggregate)
-- Pilot Aggregate
-- Pilot --> User (account id)
-- Pilot --> AircraftModel (certified for)
-- ============================================================

CREATE TABLE air_transport_company (
                                       id        BIGSERIAL    PRIMARY KEY,
                                       name      VARCHAR(255) NOT NULL UNIQUE,
                                       iata_code VARCHAR(2)   NOT NULL UNIQUE,
                                       icao_code VARCHAR(3)   NOT NULL UNIQUE
);

CREATE TABLE air_transport_company_users (
                                             company_id BIGINT       NOT NULL REFERENCES air_transport_company(id),
                                             user_id    VARCHAR(255) NOT NULL,
                                             PRIMARY KEY (company_id, user_id)
);

CREATE TABLE aircraft (
                          registration_number  VARCHAR(20)      PRIMARY KEY,
                          registration_country VARCHAR(255)     NOT NULL,
                          operational_status   VARCHAR(30)      NOT NULL DEFAULT 'ACTIVE',
                          flight_crew_count    INT              NOT NULL,
                          first_class_seats    INT              NOT NULL DEFAULT 0,
                          business_class_seats INT              NOT NULL DEFAULT 0,
                          economy_class_seats  INT              NOT NULL DEFAULT 0,
                          aircraft_model_id    BIGINT           NOT NULL REFERENCES aircraft_model(id),
                          company_id           BIGINT           NOT NULL REFERENCES air_transport_company(id)
);

CREATE TABLE pilot (
                       id      BIGSERIAL    PRIMARY KEY,
                       status  VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
                       user_id VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE pilot_certified_models (
                                        pilot_id          BIGINT NOT NULL REFERENCES pilot(id),
                                        aircraft_model_id BIGINT NOT NULL REFERENCES aircraft_model(id),
                                        PRIMARY KEY (pilot_id, aircraft_model_id)
);

-- ============================================================
-- FLIGHT PLANNING
-- FlightRoute Aggregate
-- AirTransportCompany --> FlightRoute (operates)
-- Flight Aggregate
-- FlightRoute --> Flight (schedules)
-- Flight --> FlightPlan (uses)
-- FlightPlan Aggregate (FlightLeg + Segment + AltitudeSlot)
-- FlightPlan --> Aircraft, Pilot (cross-aggregate)
-- FlightLeg --> Airport departure/arrival (cross-aggregate)
-- Segment --> Node start/end (cross-aggregate)
-- ============================================================

CREATE TABLE flight_route (
                              id          VARCHAR(255) PRIMARY KEY,
                              designation VARCHAR(6)   NOT NULL UNIQUE,
                              active_from DATE         NOT NULL,
                              active_until DATE,
                              company_id  BIGINT       NOT NULL REFERENCES air_transport_company(id)
);

CREATE TABLE flight (
                        flight_designator        VARCHAR(10)  PRIMARY KEY,
                        flight_type              VARCHAR(20)  NOT NULL,
                        scheduled_departure_time TIMESTAMP    NOT NULL,
                        scheduled_arrival_time   TIMESTAMP    NOT NULL,
                        flight_route_id          VARCHAR(255) REFERENCES flight_route(id)
);

CREATE TABLE flight_plan (
                             id                  VARCHAR(255)     PRIMARY KEY,
                             departure_date_time TIMESTAMP        NOT NULL,
                             fuel_quantity       DOUBLE PRECISION NOT NULL,
                             passenger_load      INT              NOT NULL DEFAULT 0,
                             cargo_load          DOUBLE PRECISION NOT NULL DEFAULT 0,
                             plan_status         VARCHAR(10)      NOT NULL DEFAULT 'DRAFT',
                             aircraft_id         VARCHAR(20)      REFERENCES aircraft(registration_number),
                             pilot_id            BIGINT           REFERENCES pilot(id),
                             flight_id           VARCHAR(10)      REFERENCES flight(flight_designator),
                             version             BIGINT
);

CREATE TABLE flight_leg (
                            id                   BIGSERIAL        PRIMARY KEY,
                            sequence_number      INT              NOT NULL,
                            departure_time       TIMESTAMP        NOT NULL,
                            arrival_time         TIMESTAMP        NOT NULL,
                            fuel_quantity        DOUBLE PRECISION NOT NULL,
                            departure_airport_id BIGINT           NOT NULL REFERENCES airport(id),
                            arrival_airport_id   BIGINT           NOT NULL REFERENCES airport(id),
                            flight_plan_id       VARCHAR(255)     NOT NULL REFERENCES flight_plan(id)
);

CREATE TABLE segment (
                         id             BIGSERIAL        PRIMARY KEY,
                         wind_direction DOUBLE PRECISION NOT NULL,
                         wind_speed     DOUBLE PRECISION NOT NULL,
                         start_node_id  VARCHAR(255)     NOT NULL REFERENCES node(id),
                         end_node_id    VARCHAR(255)     NOT NULL REFERENCES node(id),
                         flight_leg_id  BIGINT           NOT NULL REFERENCES flight_leg(id)
);

CREATE TABLE altitude_slot (
                               id               BIGSERIAL        PRIMARY KEY,
                               minimum_altitude DOUBLE PRECISION NOT NULL,
                               maximum_altitude DOUBLE PRECISION NOT NULL,
                               width            DOUBLE PRECISION NOT NULL,
                               segment_id       BIGINT           NOT NULL REFERENCES segment(id)
);

-- ============================================================
-- WEATHER
-- WeatherData Aggregate
-- ============================================================

CREATE TABLE weather_data (
                              id                  BIGSERIAL        PRIMARY KEY,
                              weather_date        DATE             NOT NULL,
                              wind_speed          DOUBLE PRECISION NOT NULL,
                              wind_direction      DOUBLE PRECISION NOT NULL,
                              air_control_area_id BIGINT           NOT NULL REFERENCES air_control_area(id)
);

-- ============================================================
-- SIMULATION
-- Simulation Aggregate
-- AircraftPosition (VelocityVector embedded — speed + direction)
-- SafetyViolation (2+ VelocityVectors em tabela separada)
-- Simulation --> AirControlArea, Flight (cross-aggregate)
-- SimulationReport Aggregate --> Simulation
-- ============================================================

CREATE TABLE simulation (
                            id                   VARCHAR(255) PRIMARY KEY,
                            start_date_time      TIMESTAMP    NOT NULL,
                            end_date_time        TIMESTAMP    NOT NULL,
                            geographic_area      TEXT,
                            safety_thresholds    TEXT,
                            performance_settings TEXT,
                            sim_status           VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
                            air_control_area_id  BIGINT       REFERENCES air_control_area(id)
);

-- Simulation --> Flight (many-to-many: "tracks 1..*")
CREATE TABLE simulation_flights (
                                    simulation_id     VARCHAR(255) NOT NULL REFERENCES simulation(id),
                                    flight_designator VARCHAR(10)  NOT NULL REFERENCES flight(flight_designator),
                                    PRIMARY KEY (simulation_id, flight_designator)
);

-- AircraftPosition com VelocityVector embutido (speed + direction)
CREATE TABLE aircraft_position (
                                   id            BIGSERIAL        PRIMARY KEY,
                                   pos_timestamp TIMESTAMP        NOT NULL,
                                   altitude      DOUBLE PRECISION NOT NULL,
                                   speed         DOUBLE PRECISION NOT NULL,
                                   direction     DOUBLE PRECISION NOT NULL,
                                   aircraft_id   VARCHAR(20)      NOT NULL REFERENCES aircraft(registration_number),
                                   simulation_id VARCHAR(255)     NOT NULL REFERENCES simulation(id)
);

-- SafetyViolation com 2+ VelocityVectors em tabela separada
CREATE TABLE safety_violation (
                                  id             BIGSERIAL    PRIMARY KEY,
                                  viol_timestamp TIMESTAMP    NOT NULL,
                                  severity       VARCHAR(20)  NOT NULL,
                                  simulation_id  VARCHAR(255) NOT NULL REFERENCES simulation(id)
);

CREATE TABLE safety_violation_velocity_vectors (
                                                   id                  BIGSERIAL        PRIMARY KEY,
                                                   safety_violation_id BIGINT           NOT NULL REFERENCES safety_violation(id),
                                                   speed               DOUBLE PRECISION NOT NULL,
                                                   direction           DOUBLE PRECISION NOT NULL
);

-- SimulationReport Aggregate
CREATE TABLE simulation_report (
                                   id                BIGSERIAL    PRIMARY KEY,
                                   total_flights     INT          NOT NULL DEFAULT 0,
                                   validation_result VARCHAR(10)  NOT NULL DEFAULT 'PENDING',
                                   generated_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
                                   simulation_id     VARCHAR(255) REFERENCES simulation(id)
);