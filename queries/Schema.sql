    -- ============================================================
-- AISafe schema + bootstrap data
-- Naming sem underscores
-- PostgreSQL
-- ============================================================

-- ============================================================
-- DROP
-- ============================================================

DROP TABLE IF EXISTS simulationreportflightstatus CASCADE;
DROP TABLE IF EXISTS simulationreport CASCADE;
DROP TABLE IF EXISTS safetyviolationvelocityvector CASCADE;
DROP TABLE IF EXISTS safetyviolationinvolvedaircraft CASCADE;
DROP TABLE IF EXISTS safetyviolation CASCADE;
DROP TABLE IF EXISTS aircraftposition CASCADE;
DROP TABLE IF EXISTS simulationweatherdata CASCADE;
DROP TABLE IF EXISTS simulationflight CASCADE;
DROP TABLE IF EXISTS simulation CASCADE;
DROP TABLE IF EXISTS weatherdata CASCADE;
DROP TABLE IF EXISTS altitudeslot CASCADE;
DROP TABLE IF EXISTS segment CASCADE;
DROP TABLE IF EXISTS flightleg CASCADE;
DROP TABLE IF EXISTS flightplan CASCADE;
DROP TABLE IF EXISTS flight CASCADE;
DROP TABLE IF EXISTS flightroute CASCADE;
DROP TABLE IF EXISTS pilotcertifiedaircraftmodel CASCADE;
DROP TABLE IF EXISTS pilot CASCADE;
DROP TABLE IF EXISTS aircraft CASCADE;
DROP TABLE IF EXISTS aircontrolareacollaborator CASCADE;
DROP TABLE IF EXISTS airtransportcompanycollaborator CASCADE;
DROP TABLE IF EXISTS airtransportcompany CASCADE;
DROP TABLE IF EXISTS aircraftmodelenginemodel CASCADE;
DROP TABLE IF EXISTS aircraftmodel CASCADE;
DROP TABLE IF EXISTS enginemodel CASCADE;
DROP TABLE IF EXISTS airport CASCADE;
DROP TABLE IF EXISTS aircontrolarea CASCADE;
DROP TABLE IF EXISTS node CASCADE;
DROP TABLE IF EXISTS userrole CASCADE;
DROP TABLE IF EXISTS role CASCADE;
DROP TABLE IF EXISTS appuser CASCADE;

-- ============================================================
-- USERS / AUTH
-- ============================================================

CREATE TABLE appuser (
                         email VARCHAR(255) PRIMARY KEY,
                         passwordhash VARCHAR(255) NOT NULL,
                         name VARCHAR(255) NOT NULL,
                         phonenumber VARCHAR(50) NOT NULL,
                         activesecurityclearanceexpiry DATE,
                         skillsassessmentduedate DATE,
                         status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
                         enabled BOOLEAN NOT NULL DEFAULT TRUE,
                         CHECK (status IN ('ACTIVE', 'INACTIVE', 'DISABLED'))
);

CREATE TABLE role (
                      rolecode VARCHAR(50) PRIMARY KEY,
                      description VARCHAR(255) NOT NULL
);

CREATE TABLE userrole (
                          useremail VARCHAR(255) NOT NULL REFERENCES appuser(email),
                          rolecode VARCHAR(50) NOT NULL REFERENCES role(rolecode),
                          PRIMARY KEY (useremail, rolecode)
);

-- ============================================================
-- AIR CONTROL / AIRPORTS / NODES
-- ============================================================

CREATE TABLE aircontrolarea (
                                id BIGSERIAL PRIMARY KEY,
                                areacode VARCHAR(10) NOT NULL UNIQUE,
                                minimumrequiredarrivalfuel DOUBLE PRECISION NOT NULL CHECK (minimumrequiredarrivalfuel >= 0),
                                geographicboundaries TEXT NOT NULL
);

CREATE TABLE airport (
                         id BIGSERIAL PRIMARY KEY,
                         name VARCHAR(255) NOT NULL,
                         town VARCHAR(255) NOT NULL,
                         country VARCHAR(255) NOT NULL,
                         iatacode VARCHAR(3) NOT NULL UNIQUE,
                         icaocode VARCHAR(4) NOT NULL UNIQUE,
                         latitude DOUBLE PRECISION NOT NULL,
                         longitude DOUBLE PRECISION NOT NULL,
                         elevation DOUBLE PRECISION NOT NULL,
                         aircontrolareaid BIGINT NOT NULL REFERENCES aircontrolarea(id)
);

CREATE TABLE node (
                      id VARCHAR(255) PRIMARY KEY,
                      designation VARCHAR(255) NOT NULL,
                      latitude DOUBLE PRECISION NOT NULL,
                      longitude DOUBLE PRECISION NOT NULL
);

-- ============================================================
-- ENGINE MODELS
-- ============================================================

CREATE TABLE enginemodel (
                             id BIGSERIAL PRIMARY KEY,
                             name VARCHAR(255) NOT NULL,
                             enginetype VARCHAR(100) NOT NULL,
                             power DOUBLE PRECISION NOT NULL CHECK (power > 0),
                             fueltype VARCHAR(100) NOT NULL,
                             efficiency DOUBLE PRECISION NOT NULL CHECK (efficiency > 0),
                             thrustsealevelstopped DOUBLE PRECISION NOT NULL CHECK (thrustsealevelstopped > 0),
                             thrustsealevelcruise DOUBLE PRECISION NOT NULL CHECK (thrustsealevelcruise > 0),
                             thrustspecificfuelconsumption DOUBLE PRECISION NOT NULL CHECK (thrustspecificfuelconsumption > 0),
                             makername VARCHAR(255) NOT NULL,
                             makercountry VARCHAR(255) NOT NULL,
                             fueldensity DOUBLE PRECISION,
                             fuelspecificenergy DOUBLE PRECISION,
                             UNIQUE (name, makername)
);

-- ============================================================
-- AIRCRAFT MODELS
-- ============================================================

CREATE TABLE aircraftmodel (
                               id BIGSERIAL PRIMARY KEY,
                               modelname VARCHAR(255) NOT NULL,
                               modeltype VARCHAR(20) NOT NULL,
                               enginetype VARCHAR(100) NOT NULL,
                               maximumrange DOUBLE PRECISION NOT NULL CHECK (maximumrange > 0),
                               emptyweight DOUBLE PRECISION NOT NULL CHECK (emptyweight > 0),
                               maximumtakeoffweight DOUBLE PRECISION NOT NULL CHECK (maximumtakeoffweight > 0),
                               maximumzerofuelweight DOUBLE PRECISION NOT NULL CHECK (maximumzerofuelweight > 0),
                               maximumfuelcapacity DOUBLE PRECISION NOT NULL CHECK (maximumfuelcapacity > 0),
                               serviceceiling DOUBLE PRECISION NOT NULL CHECK (serviceceiling > 0),
                               cruisespeed DOUBLE PRECISION NOT NULL CHECK (cruisespeed > 0),
                               wingarea DOUBLE PRECISION NOT NULL CHECK (wingarea > 0),
                               wingspan DOUBLE PRECISION NOT NULL CHECK (wingspan > 0),
                               dragcoefficient DOUBLE PRECISION NOT NULL CHECK (dragcoefficient > 0),
                               liftcoefficient DOUBLE PRECISION NOT NULL CHECK (liftcoefficient > 0),
                               numberofengines INT NOT NULL CHECK (numberofengines > 0),
                               maximumpassengercapacity INT NOT NULL CHECK (maximumpassengercapacity > 0),
                               makername VARCHAR(255) NOT NULL,
                               makercountry VARCHAR(255) NOT NULL,
                               UNIQUE (modelname, makername),
                               CHECK (modeltype IN ('PASSENGER', 'CARGO', 'MIXED'))
);

CREATE TABLE aircraftmodelenginemodel (
                                          aircraftmodelid BIGINT NOT NULL REFERENCES aircraftmodel(id),
                                          enginemodelid BIGINT NOT NULL REFERENCES enginemodel(id),
                                          PRIMARY KEY (aircraftmodelid, enginemodelid)
);

-- ============================================================
-- COMPANIES / COLLABORATORS
-- ============================================================

CREATE TABLE airtransportcompany (
                                     id BIGSERIAL PRIMARY KEY,
                                     name VARCHAR(255) NOT NULL UNIQUE,
                                     iatacode VARCHAR(2) NOT NULL UNIQUE,
                                     icaocode VARCHAR(3) NOT NULL UNIQUE
);

CREATE TABLE airtransportcompanycollaborator (
                                                 companyid BIGINT NOT NULL REFERENCES airtransportcompany(id),
                                                 useremail VARCHAR(255) NOT NULL REFERENCES appuser(email),
                                                 position VARCHAR(100) NOT NULL,
                                                 active BOOLEAN NOT NULL DEFAULT TRUE,
                                                 PRIMARY KEY (companyid, useremail)
);

CREATE TABLE aircontrolareacollaborator (
                                            aircontrolareaid BIGINT NOT NULL REFERENCES aircontrolarea(id),
                                            useremail VARCHAR(255) NOT NULL REFERENCES appuser(email),
                                            position VARCHAR(100) NOT NULL,
                                            active BOOLEAN NOT NULL DEFAULT TRUE,
                                            PRIMARY KEY (aircontrolareaid, useremail)
);

-- ============================================================
-- AIRCRAFT / PILOTS
-- ============================================================

CREATE TABLE aircraft (
                          registrationnumber VARCHAR(20) PRIMARY KEY,
                          registrationcountry VARCHAR(255) NOT NULL,
                          operationalstatus VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
                          serviceentrydate DATE NOT NULL,
                          flightcrewcount INT NOT NULL CHECK (flightcrewcount > 0),
                          firstclassseats INT NOT NULL DEFAULT 0 CHECK (firstclassseats >= 0),
                          businessclassseats INT NOT NULL DEFAULT 0 CHECK (businessclassseats >= 0),
                          economyclassseats INT NOT NULL DEFAULT 0 CHECK (economyclassseats >= 0),
                          aircraftmodelid BIGINT NOT NULL REFERENCES aircraftmodel(id),
                          companyid BIGINT NOT NULL REFERENCES airtransportcompany(id),
                          CHECK (operationalstatus IN ('ACTIVE', 'INACTIVE', 'RETIRED'))
);

CREATE TABLE pilot (
                       id BIGSERIAL PRIMARY KEY,
                       useremail VARCHAR(255) NOT NULL UNIQUE REFERENCES appuser(email),
                       companyid BIGINT NOT NULL REFERENCES airtransportcompany(id),
                       status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
                       CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE TABLE pilotcertifiedaircraftmodel (
                                             pilotid BIGINT NOT NULL REFERENCES pilot(id),
                                             aircraftmodelid BIGINT NOT NULL REFERENCES aircraftmodel(id),
                                             PRIMARY KEY (pilotid, aircraftmodelid)
);

-- ============================================================
-- ROUTES / FLIGHTS / FLIGHT PLANS
-- ============================================================

CREATE TABLE flightroute (
                             id BIGSERIAL PRIMARY KEY,
                             routecode VARCHAR(6) NOT NULL UNIQUE,
                             originairportid BIGINT NOT NULL REFERENCES airport(id),
                             destinationairportid BIGINT NOT NULL REFERENCES airport(id),
                             activefrom DATE NOT NULL,
                             activeuntil DATE,
                             companyid BIGINT NOT NULL REFERENCES airtransportcompany(id),
                             CHECK (originairportid <> destinationairportid)
);

CREATE TABLE flight (
                        flightdesignator VARCHAR(10) PRIMARY KEY,
                        flighttype VARCHAR(20) NOT NULL,
                        scheduleddeparturetime TIMESTAMP NOT NULL,
                        scheduledarrivaltime TIMESTAMP NOT NULL,
                        flightrouteid BIGINT NOT NULL REFERENCES flightroute(id),
                        CHECK (flighttype IN ('REGULAR', 'CHARTER')),
                        CHECK (scheduledarrivaltime > scheduleddeparturetime)
);

CREATE TABLE flightplan (
                            id VARCHAR(255) PRIMARY KEY,
                            flightid VARCHAR(10) NOT NULL REFERENCES flight(flightdesignator),
                            version BIGINT NOT NULL DEFAULT 1,
                            departuredatetime TIMESTAMP NOT NULL,
                            fuelquantity DOUBLE PRECISION NOT NULL CHECK (fuelquantity > 0),
                            passengerload INT NOT NULL DEFAULT 0 CHECK (passengerload >= 0),
                            cargoload DOUBLE PRECISION NOT NULL DEFAULT 0 CHECK (cargoload >= 0),
                            planstatus VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
                            aircraftid VARCHAR(20) NOT NULL REFERENCES aircraft(registrationnumber),
                            pilotid BIGINT NOT NULL REFERENCES pilot(id),
                            weatherattached BOOLEAN NOT NULL DEFAULT FALSE,
                            testedat TIMESTAMP,
                            testresult VARCHAR(20),
                            UNIQUE (flightid, version),
                            CHECK (planstatus IN ('DRAFT', 'SUBMITTED', 'VALIDATED', 'REJECTED')),
                            CHECK (testresult IS NULL OR testresult IN ('PASS', 'FAIL'))
);

CREATE TABLE flightleg (
                           id BIGSERIAL PRIMARY KEY,
                           flightplanid VARCHAR(255) NOT NULL REFERENCES flightplan(id),
                           sequencenumber INT NOT NULL CHECK (sequencenumber > 0),
                           departureairportid BIGINT NOT NULL REFERENCES airport(id),
                           arrivalairportid BIGINT NOT NULL REFERENCES airport(id),
                           departuretime TIMESTAMP NOT NULL,
                           arrivaltime TIMESTAMP NOT NULL,
                           fuelquantity DOUBLE PRECISION NOT NULL CHECK (fuelquantity > 0),
                           UNIQUE (flightplanid, sequencenumber),
                           CHECK (departureairportid <> arrivalairportid),
                           CHECK (arrivaltime > departuretime)
);

CREATE TABLE segment (
                         id BIGSERIAL PRIMARY KEY,
                         flightlegid BIGINT NOT NULL REFERENCES flightleg(id),
                         sequencenumber INT NOT NULL CHECK (sequencenumber > 0),
                         startnodeid VARCHAR(255) NOT NULL REFERENCES node(id),
                         endnodeid VARCHAR(255) NOT NULL REFERENCES node(id),
                         winddirection DOUBLE PRECISION NOT NULL,
                         windspeed DOUBLE PRECISION NOT NULL CHECK (windspeed >= 0),
                         UNIQUE (flightlegid, sequencenumber),
                         CHECK (startnodeid <> endnodeid)
);

CREATE TABLE altitudeslot (
                              id BIGSERIAL PRIMARY KEY,
                              segmentid BIGINT NOT NULL REFERENCES segment(id),
                              minimumaltitude DOUBLE PRECISION NOT NULL CHECK (minimumaltitude > 0),
                              maximumaltitude DOUBLE PRECISION NOT NULL CHECK (maximumaltitude > minimumaltitude),
                              width DOUBLE PRECISION NOT NULL CHECK (width > 0)
);

-- ============================================================
-- WEATHER
-- ============================================================

CREATE TABLE weatherdata (
                             id BIGSERIAL PRIMARY KEY,
                             weatherdate DATE NOT NULL,
                             source VARCHAR(100) NOT NULL DEFAULT 'MANUAL',
                             windspeed DOUBLE PRECISION NOT NULL CHECK (windspeed >= 0),
                             winddirection DOUBLE PRECISION NOT NULL,
                             aircontrolareaid BIGINT NOT NULL REFERENCES aircontrolarea(id),
                             UNIQUE (weatherdate, source, aircontrolareaid)
);

-- ============================================================
-- SIMULATION
-- ============================================================

CREATE TABLE simulation (
                            id VARCHAR(255) PRIMARY KEY,
                            startdatetime TIMESTAMP NOT NULL,
                            enddatetime TIMESTAMP NOT NULL,
                            geographicarea TEXT,
                            safetythresholds TEXT,
                            performancesettings TEXT,
                            simstatus VARCHAR(20) NOT NULL DEFAULT 'PENDING',
                            aircontrolareaid BIGINT NOT NULL REFERENCES aircontrolarea(id),
                            CHECK (simstatus IN ('PENDING', 'RUNNING', 'COMPLETED', 'FAILED', 'ABORTED')),
                            CHECK (enddatetime > startdatetime)
);

CREATE TABLE simulationflight (
                                  simulationid VARCHAR(255) NOT NULL REFERENCES simulation(id),
                                  flightdesignator VARCHAR(10) NOT NULL REFERENCES flight(flightdesignator),
                                  PRIMARY KEY (simulationid, flightdesignator)
);

CREATE TABLE simulationweatherdata (
                                       simulationid VARCHAR(255) NOT NULL REFERENCES simulation(id),
                                       weatherdataid BIGINT NOT NULL REFERENCES weatherdata(id),
                                       PRIMARY KEY (simulationid, weatherdataid)
);

CREATE TABLE aircraftposition (
                                  id BIGSERIAL PRIMARY KEY,
                                  simulationid VARCHAR(255) NOT NULL REFERENCES simulation(id),
                                  aircraftregistrationnumber VARCHAR(20) NOT NULL REFERENCES aircraft(registrationnumber),
                                  positiontimestamp TIMESTAMP NOT NULL,
                                  altitude DOUBLE PRECISION NOT NULL CHECK (altitude >= 0),
                                  speed DOUBLE PRECISION NOT NULL CHECK (speed >= 0),
                                  direction DOUBLE PRECISION NOT NULL
);

CREATE TABLE safetyviolation (
                                 id BIGSERIAL PRIMARY KEY,
                                 simulationid VARCHAR(255) NOT NULL REFERENCES simulation(id),
                                 violationtimestamp TIMESTAMP NOT NULL,
                                 severity VARCHAR(20) NOT NULL,
                                 CHECK (severity IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL'))
);

CREATE TABLE safetyviolationinvolvedaircraft (
                                                 safetyviolationid BIGINT NOT NULL REFERENCES safetyviolation(id),
                                                 aircraftregistrationnumber VARCHAR(20) NOT NULL REFERENCES aircraft(registrationnumber),
                                                 PRIMARY KEY (safetyviolationid, aircraftregistrationnumber)
);

CREATE TABLE safetyviolationvelocityvector (
                                               id BIGSERIAL PRIMARY KEY,
                                               safetyviolationid BIGINT NOT NULL REFERENCES safetyviolation(id),
                                               aircraftregistrationnumber VARCHAR(20) REFERENCES aircraft(registrationnumber),
                                               speed DOUBLE PRECISION NOT NULL CHECK (speed >= 0),
                                               direction DOUBLE PRECISION NOT NULL
);

-- ============================================================
-- REPORTS
-- ============================================================

CREATE TABLE simulationreport (
                                  id BIGSERIAL PRIMARY KEY,
                                  simulationid VARCHAR(255) NOT NULL UNIQUE REFERENCES simulation(id),
                                  totalflights INT NOT NULL DEFAULT 0 CHECK (totalflights >= 0),
                                  validationresult VARCHAR(10) NOT NULL DEFAULT 'PENDING',
                                  generatedat TIMESTAMP NOT NULL DEFAULT NOW(),
                                  CHECK (validationresult IN ('PENDING', 'PASS', 'FAIL'))
);

CREATE TABLE simulationreportflightstatus (
                                              reportid BIGINT NOT NULL REFERENCES simulationreport(id),
                                              flightdesignator VARCHAR(10) NOT NULL REFERENCES flight(flightdesignator),
                                              executionstatus VARCHAR(20) NOT NULL,
                                              PRIMARY KEY (reportid, flightdesignator),
                                              CHECK (executionstatus IN ('PENDING', 'RUNNING', 'COMPLETED', 'FAILED', 'ABORTED'))
);