-- ============================================================
-- BOOTSTRAP DATA
-- ============================================================

INSERT INTO role (rolecode, description) VALUES
                                             ('ADMIN', 'System administrator'),
                                             ('BACKOFFICE_OPERATOR', 'Backoffice operator'),
                                             ('AIR_TRANSPORT_COMPANY_COLLABORATOR', 'Air transport company collaborator'),
                                             ('PILOT', 'Pilot'),
                                             ('FLIGHT_CONTROL_OPERATOR', 'Flight Control Operator'),
                                             ('WEATHER_PERSON', 'Weather Person');

INSERT INTO appuser (
    email, passwordhash, name, phonenumber,
    activesecurityclearanceexpiry, skillsassessmentduedate,
    status, enabled
) VALUES
      ('admin@aisafe.com',   'Admin1234!',    'System Admin',         '+351910000001', '2028-12-31', '2027-12-31', 'ACTIVE', TRUE),
      ('bo@aisafe.com',      'Operator1234!', 'Backoffice Operator',  '+351910000002', '2028-12-31', '2027-12-31', 'ACTIVE', TRUE),
      ('pilot@aisafe.com',   'Pilot1234!',    'Default Pilot',        '+351910000003', '2028-12-31', '2027-12-31', 'ACTIVE', TRUE),
      ('fco@aisafe.com',     'Fco1234!',      'Default FCO',          '+351910000004', '2028-12-31', '2027-12-31', 'ACTIVE', TRUE),
      ('weather@aisafe.com', 'Weather1234!',  'Weather Person',       '+351910000005', '2028-12-31', '2027-12-31', 'ACTIVE', TRUE),
      ('atcc@aisafe.com',    'Atcc1234!',     'Company Collaborator', '+351910000006', '2028-12-31', '2027-12-31', 'ACTIVE', TRUE);

INSERT INTO userrole (useremail, rolecode) VALUES
                                               ('admin@aisafe.com', 'ADMIN'),
                                               ('bo@aisafe.com', 'BACKOFFICE_OPERATOR'),
                                               ('pilot@aisafe.com', 'PILOT'),
                                               ('fco@aisafe.com', 'FLIGHT_CONTROL_OPERATOR'),
                                               ('weather@aisafe.com', 'WEATHER_PERSON'),
                                               ('atcc@aisafe.com', 'AIR_TRANSPORT_COMPANY_COLLABORATOR');

INSERT INTO aircontrolarea (areacode, minimumrequiredarrivalfuel, geographicboundaries) VALUES
                                                                                            ('LPPC', 2000.0, 'Portugal Continental'),
                                                                                            ('LPPO', 1500.0, 'Azores'),
                                                                                            ('GCCC', 1800.0, 'Canary Islands');

INSERT INTO airport (
    name, town, country, iatacode, icaocode,
    latitude, longitude, elevation, aircontrolareaid
) VALUES
      ('Humberto Delgado',      'Lisboa',        'Portugal', 'LIS', 'LPPT', 38.7756,  -9.1354, 113.0, 1),
      ('Francisco Sa Carneiro', 'Porto',         'Portugal', 'OPO', 'LPPR', 41.2481,  -8.6814,  69.0, 1),
      ('Faro',                  'Faro',          'Portugal', 'FAO', 'LPFR', 37.0144,  -7.9659,  24.0, 1),
      ('Joao Paulo II',         'Ponta Delgada', 'Portugal', 'PDL', 'LPPD', 37.7412, -25.6979,  82.0, 2),
      ('Gran Canaria',          'Las Palmas',    'Spain',    'LPA', 'GCLP', 27.9319, -15.3866,  24.0, 3);

INSERT INTO node (id, designation, latitude, longitude) VALUES
                                                            ('NODE001', 'LISBON NORTH', 38.9000,  -9.0000),
                                                            ('NODE002', 'PORTO SOUTH',  41.0000,  -8.5000),
                                                            ('NODE003', 'FARO EAST',    37.1000,  -7.5000),
                                                            ('NODE004', 'ATLANTIC MID', 39.5000, -11.0000),
                                                            ('NODE005', 'AZORES APP',   38.0000, -25.0000);

INSERT INTO enginemodel (
    name, enginetype, power, fueltype, efficiency,
    thrustsealevelstopped, thrustsealevelcruise,
    thrustspecificfuelconsumption, makername, makercountry,
    fueldensity, fuelspecificenergy
) VALUES
      ('CFM56-7B',  'TURBOFAN', 27000.0, 'JET_A1', 0.85, 121000.0,  98000.0, 0.0370, 'CFM International', 'USA',           0.804, 42.80),
      ('V2500-A5',  'TURBOFAN', 25000.0, 'JET_A1', 0.83, 111000.0,  90000.0, 0.0360, 'IAE',               'International', 0.804, 42.80),
      ('GE90-115B', 'TURBOFAN', 51000.0, 'JET_A1', 0.88, 513000.0, 420000.0, 0.0320, 'GE Aviation',       'USA',           0.804, 42.80);

INSERT INTO aircraftmodel (
    modelname, modeltype, enginetype, maximumrange,
    emptyweight, maximumtakeoffweight, maximumzerofuelweight,
    maximumfuelcapacity, serviceceiling, cruisespeed,
    wingarea, wingspan, dragcoefficient, liftcoefficient,
    numberofengines, maximumpassengercapacity,
    makername, makercountry
) VALUES
      ('737-800',   'PASSENGER', 'TURBOFAN',  5436.0,  41145.0,  79016.0,  62732.0,  26022.0, 12500.0, 230.0, 125.08, 35.8, 0.026, 1.5, 2, 189, 'Boeing', 'USA'),
      ('A320-200',  'PASSENGER', 'TURBOFAN',  6150.0,  42600.0,  78000.0,  61000.0,  24210.0, 12131.0, 228.0, 122.60, 34.1, 0.024, 1.4, 2, 180, 'Airbus', 'France'),
      ('777-300ER', 'PASSENGER', 'TURBOFAN', 13649.0, 178756.0, 299370.0, 242670.0, 181283.0, 13137.0, 250.0, 541.16, 60.9, 0.022, 1.6, 2, 396, 'Boeing', 'USA');

INSERT INTO aircraftmodelenginemodel (aircraftmodelid, enginemodelid) VALUES
                                                                          (1, 1),
                                                                          (2, 2),
                                                                          (3, 3);

INSERT INTO airtransportcompany (name, iatacode, icaocode) VALUES
                                                               ('TAP Air Portugal', 'TP', 'TAP'),
                                                               ('Ryanair', 'FR', 'RYR');

INSERT INTO airtransportcompanycollaborator (companyid, useremail, position, active) VALUES
    (1, 'atcc@aisafe.com', 'ATCC', TRUE);

INSERT INTO aircontrolareacollaborator (aircontrolareaid, useremail, position, active) VALUES
    (1, 'fco@aisafe.com', 'FCO', TRUE);

INSERT INTO pilot (useremail, companyid, status) VALUES
    ('pilot@aisafe.com', 1, 'ACTIVE');

INSERT INTO pilotcertifiedaircraftmodel (pilotid, aircraftmodelid) VALUES
                                                                       (1, 1),
                                                                       (1, 2);

INSERT INTO aircraft (
    registrationnumber, registrationcountry, operationalstatus, serviceentrydate,
    flightcrewcount, firstclassseats, businessclassseats, economyclassseats,
    aircraftmodelid, companyid
) VALUES
      ('CSTUA', 'Portugal', 'ACTIVE', '2018-06-01', 2, 0, 12, 150, 1, 1),
      ('CSTUB', 'Portugal', 'ACTIVE', '2019-09-15', 2, 0,  8, 162, 2, 1),
      ('EIDAL', 'Ireland',  'ACTIVE', '2017-03-20', 2, 0,  0, 189, 1, 2);

INSERT INTO flightroute (
    routecode, originairportid, destinationairportid,
    activefrom, activeuntil, companyid
) VALUES
      ('TP123', 1, 2, '2026-01-01', NULL, 1),
      ('TP456', 2, 3, '2026-01-01', NULL, 1),
      ('FR789', 1, 5, '2026-01-01', NULL, 2);

INSERT INTO flight (
    flightdesignator, flighttype,
    scheduleddeparturetime, scheduledarrivaltime, flightrouteid
) VALUES
      ('TP123A', 'REGULAR', '2026-05-10 08:00:00', '2026-05-10 09:00:00', 1),
      ('TP456B', 'REGULAR', '2026-05-10 14:00:00', '2026-05-10 15:30:00', 2),
      ('FR789C', 'CHARTER', '2026-05-11 07:00:00', '2026-05-11 10:30:00', 3);

INSERT INTO flightplan (
    id, flightid, version, departuredatetime, fuelquantity,
    passengerload, cargoload, planstatus, aircraftid, pilotid,
    weatherattached, testedat, testresult
) VALUES
      ('fp001', 'TP123A', 1, '2026-05-10 08:00:00', 15000.0, 150, 500.0, 'DRAFT', 'CSTUA', 1, TRUE, NULL, NULL),
      ('fp002', 'TP456B', 1, '2026-05-10 14:00:00', 12000.0, 162, 300.0, 'DRAFT', 'CSTUB', 1, TRUE, NULL, NULL);

INSERT INTO flightleg (
    flightplanid, sequencenumber, departureairportid, arrivalairportid,
    departuretime, arrivaltime, fuelquantity
) VALUES
      ('fp001', 1, 1, 2, '2026-05-10 08:00:00', '2026-05-10 09:00:00', 15000.0),
      ('fp002', 1, 2, 3, '2026-05-10 14:00:00', '2026-05-10 15:30:00', 12000.0);

INSERT INTO segment (
    flightlegid, sequencenumber, startnodeid, endnodeid, winddirection, windspeed
) VALUES
      (1, 1, 'NODE001', 'NODE002', 270.0, 15.5),
      (2, 1, 'NODE002', 'NODE003', 260.0, 12.0);

INSERT INTO altitudeslot (
    segmentid, minimumaltitude, maximumaltitude, width
) VALUES
      (1, 8000.0, 12000.0, 50.0),
      (2, 7000.0, 11000.0, 50.0);

INSERT INTO weatherdata (
    weatherdate, source, windspeed, winddirection, aircontrolareaid
) VALUES
      ('2026-05-10', 'MANUAL',     15.5, 270.0, 1),
      ('2026-05-10', 'CSVIMPORT',  12.0, 245.0, 2),
      ('2026-05-11', 'MANUAL',     20.0, 300.0, 1);

INSERT INTO simulation (
    id, startdatetime, enddatetime, geographicarea,
    safetythresholds, performancesettings, simstatus, aircontrolareaid
) VALUES
    ('sim001', '2026-05-10 07:00:00', '2026-05-10 20:00:00',
     'Portugal Continental', '500m', 'default', 'PENDING', 1);

INSERT INTO simulationflight (simulationid, flightdesignator) VALUES
                                                                  ('sim001', 'TP123A'),
                                                                  ('sim001', 'TP456B');

INSERT INTO simulationweatherdata (simulationid, weatherdataid) VALUES
                                                                    ('sim001', 1),
                                                                    ('sim001', 3);

INSERT INTO aircraftposition (
    simulationid, aircraftregistrationnumber, positiontimestamp,
    altitude, speed, direction
) VALUES
      ('sim001', 'CSTUA', '2026-05-10 08:30:00', 10000.0, 230.0, 45.0),
      ('sim001', 'CSTUB', '2026-05-10 14:45:00',  9500.0, 228.0, 90.0);

INSERT INTO safetyviolation (
    simulationid, violationtimestamp, severity
) VALUES
    ('sim001', '2026-05-10 08:35:00', 'LOW');

INSERT INTO safetyviolationinvolvedaircraft (
    safetyviolationid, aircraftregistrationnumber
) VALUES
      (1, 'CSTUA'),
      (1, 'CSTUB');

INSERT INTO safetyviolationvelocityvector (
    safetyviolationid, aircraftregistrationnumber, speed, direction
) VALUES
      (1, 'CSTUA', 230.0, 45.0),
      (1, 'CSTUB', 228.0, 90.0);

INSERT INTO simulationreport (
    simulationid, totalflights, validationresult, generatedat
) VALUES
    ('sim001', 2, 'PENDING', '2026-05-12 00:00:00');

INSERT INTO simulationreportflightstatus (
    reportid, flightdesignator, executionstatus
) VALUES
      (1, 'TP123A', 'PENDING'),
      (1, 'TP456B', 'PENDING');