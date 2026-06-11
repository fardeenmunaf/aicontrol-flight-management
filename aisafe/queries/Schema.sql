-- ============================================================
-- AISafe Database Schema
-- PostgreSQL
-- ============================================================

DROP TABLE IF EXISTS CustomerCollaborator CASCADE;
DROP TABLE IF EXISTS PilotCompanyAssignment CASCADE;
DROP TABLE IF EXISTS Pilot_AircraftModel CASCADE;
DROP TABLE IF EXISTS aircraftmodel_enginemodel CASCADE;
DROP TABLE IF EXISTS ProfileEntry CASCADE;
DROP TABLE IF EXISTS AltitudeSlot CASCADE;
DROP TABLE IF EXISTS Segment CASCADE;
DROP TABLE IF EXISTS FlightLeg CASCADE;
DROP TABLE IF EXISTS FlightPlan CASCADE;
DROP TABLE IF EXISTS Flight CASCADE;
DROP TABLE IF EXISTS FlightRoute CASCADE;
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

DROP SEQUENCE IF EXISTS T_ROLESET_SEQ CASCADE;
DROP SEQUENCE IF EXISTS T_ROLEASSIGNMENT_SEQ CASCADE;
DROP SEQUENCE IF EXISTS FlightLeg_SEQ CASCADE;
DROP SEQUENCE IF EXISTS Segment_SEQ CASCADE;
DROP SEQUENCE IF EXISTS AltitudeSlot_SEQ CASCADE;
DROP SEQUENCE IF EXISTS ProfileEntry_SEQ CASCADE;

-- ============================================================
-- IDENTITY & ACCESS
-- ============================================================

CREATE TABLE AppUser (
                         user_id                       VARCHAR(255) PRIMARY KEY,
                         email                         VARCHAR(255),
                         name                          VARCHAR(255),
                         phoneNumber                   VARCHAR(255),
                         activeSecurityClearanceExpiry DATE,
                         skillsAssessmentDueDate       DATE,
                         status                        VARCHAR(255)
);

CREATE TABLE user_roles (
                            user_id VARCHAR(255) NOT NULL REFERENCES AppUser(user_id),
                            roles   VARCHAR(255) NOT NULL,
                            PRIMARY KEY (user_id, roles)
);

CREATE TABLE T_ROLESET (
                           pk BIGINT PRIMARY KEY
);

CREATE TABLE T_ROLEASSIGNMENT (
                                  pk           BIGINT PRIMARY KEY,
                                  assignedOn   DATE,
                                  expired      BOOLEAN NOT NULL,
                                  roleName     VARCHAR(255),
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
CREATE SEQUENCE FlightLeg_SEQ START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE Segment_SEQ START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE AltitudeSlot_SEQ START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE ProfileEntry_SEQ START WITH 1 INCREMENT BY 50;

-- ============================================================
-- TOPOGRAPHY
-- ============================================================

CREATE TABLE AirControlArea (
                                air_control_area_id        VARCHAR(255) PRIMARY KEY,
                                areaCode                   VARCHAR(255) NOT NULL,
                                minimumRequiredArrivalFuel DOUBLE PRECISION NOT NULL,
                                geographicBoundaries       TEXT NOT NULL,
                                version                    BIGINT
);

CREATE TABLE Airport (
                         airport_id                         VARCHAR(255) PRIMARY KEY,
                         name                               VARCHAR(255) NOT NULL,
                         town                               VARCHAR(255) NOT NULL,
                         country                            VARCHAR(255) NOT NULL,
                         iataCode                           VARCHAR(255) NOT NULL,
                         icaoCode                           VARCHAR(255) NOT NULL,
                         elevation                          DOUBLE PRECISION NOT NULL,
                         latitude                           DOUBLE PRECISION NOT NULL,
                         longitude                          DOUBLE PRECISION NOT NULL,
                         airControlArea_air_control_area_id VARCHAR(255) NOT NULL REFERENCES AirControlArea(air_control_area_id),
                         version                            BIGINT
);

CREATE TABLE Node (
                      node_id               VARCHAR(255) PRIMARY KEY,
                      designation           VARCHAR(255) NOT NULL,
                      latitude              DOUBLE PRECISION NOT NULL,
                      longitude             DOUBLE PRECISION NOT NULL,
                      version               BIGINT
);

-- ============================================================
-- ENGINE CATALOG
-- ============================================================

CREATE TABLE EngineModel (
                             engine_model_id               VARCHAR(255) PRIMARY KEY,
                             name                          VARCHAR(255) NOT NULL,
                             type                          VARCHAR(255),
                             power                         DOUBLE PRECISION,
                             efficiency                    DOUBLE PRECISION,
                             thrustSeaLevelStopped         DOUBLE PRECISION,
                             thrustSeaLevelCruise          DOUBLE PRECISION,
                             thrustSpecificFuelConsumption DOUBLE PRECISION,
                             maker_name                    VARCHAR(255) NOT NULL,
                             maker_country                 VARCHAR(255) NOT NULL,
                             fuel_type                     VARCHAR(255),
                             density                       DOUBLE PRECISION,
                             specificEnergy                DOUBLE PRECISION,
                             version                       BIGINT,
                             UNIQUE (name, maker_name)
);

-- ============================================================
-- AIRCRAFT CATALOG
-- ============================================================

CREATE TABLE AircraftModel (
                               aircraft_model_id     VARCHAR(255) PRIMARY KEY,
                               type                  VARCHAR(255),
                               emptyWeight           DOUBLE PRECISION,
                               maximumTakeOffWeight  DOUBLE PRECISION,
                               maximumZeroFuelWeight DOUBLE PRECISION,
                               maximumFuelCapacity   DOUBLE PRECISION,
                               serviceCeiling        DOUBLE PRECISION,
                               cruiseSpeed           DOUBLE PRECISION,
                               wingArea              DOUBLE PRECISION,
                               wingSpan              DOUBLE PRECISION,
                               dragCoefficient       DOUBLE PRECISION,
                               liftCoefficient       DOUBLE PRECISION,
                               numberOfEngines       INT,
                               modelName             VARCHAR(255),
                               name                  VARCHAR(255),
                               country               VARCHAR(255),
                               version               BIGINT
);

CREATE TABLE aircraftmodel_enginemodel (
                                           aircraft_model_id VARCHAR(255) NOT NULL REFERENCES AircraftModel(aircraft_model_id),
                                           engine_model_id   VARCHAR(255) NOT NULL REFERENCES EngineModel(engine_model_id),
                                           PRIMARY KEY (aircraft_model_id, engine_model_id)
);

-- ============================================================
-- FLEET
-- ============================================================

CREATE TABLE AirTransportCompany (
                                     air_transport_company_id VARCHAR(255) PRIMARY KEY,
                                     name                     VARCHAR(255),
                                     iataCode                 VARCHAR(255),
                                     icaoCode                 VARCHAR(255),
                                     version                  BIGINT
);

CREATE TABLE Aircraft (
                          aircraft_id                              VARCHAR(255) PRIMARY KEY,
                          economyClassSeats                        INT,
                          businessClassSeats                       INT,
                          firstClassSeats                          INT,
                          aircraftModel_aircraft_model_id          VARCHAR(255) REFERENCES AircraftModel(aircraft_model_id),
                          airTransportCompany_air_transport_company_id VARCHAR(255) REFERENCES AirTransportCompany(air_transport_company_id),
                          engineModel_engine_model_id              VARCHAR(255) REFERENCES EngineModel(engine_model_id),
                          registrationNumber                       VARCHAR(255),
                          registrationCountry                      VARCHAR(255),
                          operationStatus                          VARCHAR(255),
                          flightCrewCount                          INT,
                          version                                  BIGINT
);

CREATE TABLE Pilot (
                       pilot_id VARCHAR(255) PRIMARY KEY,
                       user_id  VARCHAR(255),
                       status   VARCHAR(255),
                       version  BIGINT
);

CREATE TABLE PilotCompanyAssignment (
                                     pilot_user_id VARCHAR(255) PRIMARY KEY,
                                     company_id    VARCHAR(255) NOT NULL REFERENCES AirTransportCompany(air_transport_company_id)
);

CREATE TABLE Pilot_AircraftModel (
                                     Pilot_pilot_id                            VARCHAR(255) NOT NULL REFERENCES Pilot(pilot_id),
                                     certifiedAircraftModels_aircraft_model_id VARCHAR(255) NOT NULL REFERENCES AircraftModel(aircraft_model_id),
                                     PRIMARY KEY (Pilot_pilot_id, certifiedAircraftModels_aircraft_model_id)
);

CREATE TABLE CustomerCollaborator (
                                      customer_collaborator_id VARCHAR(255) PRIMARY KEY,
                                      user_id                  VARCHAR(255),
                                      customerType             VARCHAR(255),
                                      air_transport_company_id VARCHAR(255) REFERENCES AirTransportCompany(air_transport_company_id),
                                      air_control_area_id      VARCHAR(255) REFERENCES AirControlArea(air_control_area_id),
                                      position                 VARCHAR(255),
                                      active                   BOOLEAN,
                                      version                  BIGINT
);

-- ============================================================
-- FLIGHT PLANNING
-- ============================================================

CREATE TABLE FlightRoute (
                             flight_route_id                          VARCHAR(255) PRIMARY KEY,
                             designation                              VARCHAR(255),
                             activeFrom                               DATE NOT NULL,
                             activeUntil                              DATE,
                             originAirport_airport_id                 VARCHAR(255) NOT NULL REFERENCES Airport(airport_id),
                             destinationAirport_airport_id            VARCHAR(255) NOT NULL REFERENCES Airport(airport_id),
                             company_air_transport_company_id         VARCHAR(255) NOT NULL REFERENCES AirTransportCompany(air_transport_company_id),
                             version                                  BIGINT
);

CREATE TABLE Flight (
                        flight_id              VARCHAR(255) PRIMARY KEY,
                        flightDesignator       VARCHAR(255),
                        type                   VARCHAR(255),
                        scheduledDepartureTime TIMESTAMP,
                        scheduledArrivalTime   TIMESTAMP,
                        aircraft_aircraft_id   VARCHAR(255) REFERENCES Aircraft(aircraft_id),
                        version                BIGINT
);

CREATE TABLE FlightPlan (
                            flight_plan_id    VARCHAR(255) PRIMARY KEY,
                            status            VARCHAR(255),
                            flightDesignator  VARCHAR(255),
                            type              VARCHAR(255),
                            aircraftId        VARCHAR(255),
                            pilotRef          VARCHAR(255),
                            route_reference   VARCHAR(255),
                            route_origin      VARCHAR(255),
                            route_destination VARCHAR(255),
                            departure_time    TIMESTAMP,
                            dsl_text          TEXT,
                            version           BIGINT
);

CREATE TABLE FlightLeg (
                           id BIGINT PRIMARY KEY,
                           sequenceNumber       INT,
                           departureTime        TIMESTAMP,
                           arrivalTime          TIMESTAMP,
                           fuelQuantity         DOUBLE PRECISION,
                           departure_airport_id VARCHAR(255),
                           arrival_airport_id   VARCHAR(255),
                           flight_plan_id       VARCHAR(255) REFERENCES FlightPlan(flight_plan_id)
);

CREATE TABLE Segment (
                         id BIGINT PRIMARY KEY,
                         windDirection DOUBLE PRECISION,
                         windSpeed     DOUBLE PRECISION,
                         start_node_id VARCHAR(255),
                         end_node_id   VARCHAR(255),
                         flight_leg_id BIGINT REFERENCES FlightLeg(id)
);

CREATE TABLE AltitudeSlot (
                              id BIGINT PRIMARY KEY,
                              minimumAltitude DOUBLE PRECISION,
                              maximumAltitude DOUBLE PRECISION,
                              width           DOUBLE PRECISION,
                              segment_id      BIGINT REFERENCES Segment(id)
);

CREATE TABLE ProfileEntry (
                              id            BIGINT PRIMARY KEY,
                              phase         VARCHAR(255),
                              altitude      DOUBLE PRECISION,
                              altitudeUnit  VARCHAR(255),
                              speed         DOUBLE PRECISION,
                              speedUnit     VARCHAR(255),
                              rate          DOUBLE PRECISION,
                              rateUnit      VARCHAR(255),
                              flight_leg_id BIGINT REFERENCES FlightLeg(id)
);

-- ============================================================
-- WEATHER
-- ============================================================

CREATE TABLE WeatherData (
                             weather_data_id VARCHAR(255) PRIMARY KEY,
                             acaCode         VARCHAR(255),
                             latitude1       DOUBLE PRECISION,
                             longitude1      DOUBLE PRECISION,
                             latitude2       DOUBLE PRECISION,
                             longitude2      DOUBLE PRECISION,
                             altitudeInf     DOUBLE PRECISION,
                             altitudeSup     DOUBLE PRECISION,
                             direction       DOUBLE PRECISION,
                             windSpeed       DOUBLE PRECISION,
                             date            DATE,
                             startHour       TIME,
                             endHour         TIME,
                             version         BIGINT
);

-- ============================================================
-- SIMULATION
-- ============================================================

CREATE TABLE SimulationReport (
                                  simulation_report_id VARCHAR(255) PRIMARY KEY,
                                  totalFlights         INT,
                                  validationResult     VARCHAR(255),
                                  generatedAt          TIMESTAMP,
                                  simulation_id        VARCHAR(255),
                                  version              BIGINT
);
