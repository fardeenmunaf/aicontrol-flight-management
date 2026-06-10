-- ============================================================
-- AISafe Bootstrap Data
-- PostgreSQL
-- Generated from Java bootstraps + XML files
-- ============================================================

INSERT INTO T_ROLESET (pk) VALUES (1), (2), (3), (4), (5), (6), (7), (8);

INSERT INTO T_ROLEASSIGNMENT (pk, assignedOn, expired, roleName, unassignedOn)
VALUES
    (1, '2026-05-12', FALSE, 'ADMIN',            NULL),
    (2, '2026-05-12', FALSE, 'BACKOFFICE_OPERATOR', NULL),
    (4, '2026-05-12', FALSE, 'FCO',              NULL),
    (5, '2026-05-12', FALSE, 'WEATHER_PERSON',   NULL),
    (6, '2026-05-12', FALSE, 'ATCC',             NULL),
    (7, '2026-05-12', FALSE, 'ATCC',             NULL),
    (8, '2026-05-12', FALSE, 'FCO',              NULL),
    (3, '2026-05-12', FALSE, 'PILOT',            NULL);

INSERT INTO T_ROLESET_T_ROLEASSIGNMENT (RoleSet_pk, assignments_pk)
VALUES (1,1),(2,2),(3,3),(4,4),(5,5),(6,6),(7,7),(8,8);

INSERT INTO T_SYSTEMUSER (username, active, createdOn, deactivatedOn, email, firstName, lastName, password, resetToken, roles_pk, version)
VALUES
    ('admin@aisafe.com',  TRUE, '2026-05-12', NULL, 'admin@aisafe.com',  'System',    'Admin',   'Admin1234!',    NULL, 1, 0),
    ('bo@aisafe.com',     TRUE, '2026-05-12', NULL, 'bo@aisafe.com',     'Backoffice','Ops',     'Operator1234!', NULL, 2, 0),
    ('pilot@tap.com',     TRUE, '2026-05-12', NULL, 'pilot@tap.com',     'TAP',       'Pilot',   'Pilot1234!',    NULL, 3, 0),
    ('pilot1@tap.com',    TRUE, '2026-05-12', NULL, 'pilot1@tap.com',    'TAP',       'Pilot One',   'Pilot1234!',    NULL, 3, 0),
    ('pilot2@ryanair.ie',  TRUE, '2026-05-12', NULL, 'pilot2@ryanair.ie',  'RyanAir',   'Pilot Two',   'Pilot1234!',    NULL, 3, 0),
    ('pilot3@lufthansa.de', TRUE, '2026-05-12', NULL, 'pilot3@lufthansa.de', 'Lufthansa', 'Pilot Three', 'Pilot1234!', NULL, 3, 0),
    ('fco@aisafe.com',    TRUE, '2026-05-12', NULL, 'fco@aisafe.com',    'Default',   'FCO',     'Fco1234!',      NULL, 4, 0),
    ('weather@aisafe.com',TRUE, '2026-05-12', NULL, 'weather@aisafe.com','Weather',   'Person',  'Weather1234!',  NULL, 5, 0),
    ('atcc1@tap.pt',      TRUE, '2026-05-12', NULL, 'atcc1@tap.pt',      'ATCC',      'One',     'Password123',   NULL, 6, 0),
    ('atcc1@ryanair.ie',  TRUE, '2026-05-12', NULL, 'atcc1@ryanair.ie',  'ATCC',      'RyanAir', 'Password123',   NULL, 7, 0),
    ('fco1@aisafe.pt',    TRUE, '2026-05-12', NULL, 'fco1@aisafe.pt',    'FCO',       'One',     'Password123',   NULL, 8, 0);

INSERT INTO AppUser (user_id, email, name, phoneNumber, status)
VALUES
    ('admin@aisafe.com',   'admin@aisafe.com',   'System Admin',    '+351000000000', 'ACTIVE'),
    ('bo@aisafe.com',      'bo@aisafe.com',       'Backoffice Ops',  '+351000000000', 'ACTIVE'),

    ('fco@aisafe.com',     'fco@aisafe.com',      'Default FCO',     '+351000000000', 'ACTIVE'),
    ('weather@aisafe.com', 'weather@aisafe.com',  'Weather Person',  '+351000000000', 'ACTIVE'),
    ('pilot1@tap.com',     'pilot1@tap.com',      'TAP Pilot One',   '+351000000002', 'ACTIVE'),
    ('pilot2@ryanair.ie',  'pilot2@ryanair.ie',   'RyanAir Pilot Two', '+351000000003', 'ACTIVE'),
    ('pilot3@lufthansa.de','pilot3@lufthansa.de', 'Lufthansa Pilot Three', '+351000000004', 'ACTIVE'),
    ('atcc1@tap.pt',       'atcc1@tap.pt',        'ATCC One',        '910000001',     'ACTIVE'),
    ('atcc1@ryanair.ie',   'atcc1@ryanair.ie',    'ATCC RyanAir',    '911000001',     'ACTIVE'),
    ('fco1@aisafe.pt',     'fco1@aisafe.pt',      'FCO One',         '920000001',     'ACTIVE'),
    ('pilot@tap.com',      'pilot@tap.com',       'TAP Pilot',       '+351000000001', 'ACTIVE');

INSERT INTO user_roles (user_id, roles)
VALUES
    ('admin@aisafe.com',   'ADMIN'),
    ('bo@aisafe.com',      'BACKOFFICE_OPERATOR'),

    ('fco@aisafe.com',     'FLIGHT_CONTROL_OPERATOR'),
    ('weather@aisafe.com', 'WEATHER_PERSON'),
    ('pilot1@tap.com',     'PILOT'),
    ('pilot2@ryanair.ie',   'PILOT'),
    ('pilot3@lufthansa.de', 'PILOT'),
    ('atcc1@tap.pt',       'AIR_TRANSPORT_COMPANY_COLLABORATOR'),
    ('atcc1@ryanair.ie',   'AIR_TRANSPORT_COMPANY_COLLABORATOR'),
    ('fco1@aisafe.pt',     'FLIGHT_CONTROL_OPERATOR'),
    ('pilot@tap.com',      'PILOT');

-- ============================================================
-- AIR TRANSPORT COMPANIES
-- ============================================================

INSERT INTO AirTransportCompany (air_transport_company_id, name, iataCode, icaoCode, version)
VALUES
    ('atc-001', 'TAP Air Portugal',            'TP', 'TAP', 0),
    ('atc-002', 'RyanAir',                     'FR', 'RYR', 0),
    ('atc-003', 'EasyJet',                     'U2', 'EZY', 0),
    ('atc-004', 'Lufthansa',                   'LH', 'DLH', 0),
    ('atc-005', 'Air France',                  'AF', 'AFR', 0),
    ('atc-006', 'British Airways',             'BA', 'BAW', 0),
    ('atc-007', 'KLM Royal Dutch Airlines',    'KL', 'KLM', 0),
    ('atc-008', 'Iberia',                      'IB', 'IBE', 0),
    ('atc-009', 'Alitalia',                    'AZ', 'AZA', 0),
    ('atc-010', 'Swiss International Airlines','LX', 'SWR', 0);

-- ============================================================
-- AIR CONTROL AREAS
-- ============================================================

INSERT INTO AirControlArea (air_control_area_id, areaCode, minimumRequiredArrivalFuel, geographicBoundaries, version)
VALUES
    ('aca-LPPC', 'LPPC', 200.0, '37.0,-9.5;37.0,-6.0;42.0,-6.0;42.0,-9.5', 0),
    ('aca-LPPO', 'LPPO', 180.0, '36.5,-31.5;36.5,-24.0;40.0,-24.0;40.0,-31.5', 0),
    ('aca-LPMA', 'LPMA', 150.0, '32.0,-18.0;32.0,-15.0;34.0,-15.0;34.0,-18.0', 0),
    ('aca-GCCC', 'GCCC', 150.0, '27.0,-18.5;27.0,-13.0;29.5,-13.0;29.5,-18.5', 0),
    ('aca-LECM', 'LECM', 220.0, '36.0,-9.0;36.0,4.5;44.0,4.5;44.0,-9.0', 0),
    ('aca-LFFF', 'LFFF', 250.0, '42.5,-5.0;42.5,8.5;51.0,8.5;51.0,-5.0', 0),
    ('aca-EDGG', 'EDGG', 230.0, '47.5,6.0;47.5,15.0;55.0,15.0;55.0,6.0', 0),
    ('aca-EGTT', 'EGTT', 210.0, '49.0,-8.0;49.0,2.0;61.0,2.0;61.0,-8.0', 0),
    ('aca-EISN', 'EISN', 200.0, '51.0,-11.0;51.0,-5.0;56.0,-5.0;56.0,-11.0', 0),
    ('aca-BIRD', 'BIRD', 250.0, '63.0,-25.0;63.0,-13.0;67.0,-13.0;67.0,-25.0', 0),
    ('aca-ENOR', 'ENOR', 220.0, '58.0,4.0;58.0,32.0;71.0,32.0;71.0,4.0', 0),
    ('aca-EBBU', 'EBBU', 190.0, '49.5,2.5;49.5,6.5;51.5,6.5;51.5,2.5', 0),
    ('aca-EHAA', 'EHAA', 195.0, '51.0,2.5;51.0,7.5;55.5,7.5;55.5,2.5', 0),
    ('aca-LIBB', 'LIBB', 240.0, '37.0,6.5;37.0,18.5;47.0,18.5;47.0,6.5', 0),
    ('aca-LSAS', 'LSAS', 180.0, '45.5,5.5;45.5,11.0;48.0,11.0;48.0,5.5', 0),
    ('aca-KUSA', 'KUSA', 300.0, '24.0,-125.0;24.0,-65.0;50.0,-65.0;50.0,-125.0', 0),
    ('aca-PHZH', 'PHZH', 200.0, '18.0,-162.0;18.0,-154.0;23.0,-154.0;23.0,-162.0', 0),
    ('aca-CZEG', 'CZEG', 250.0, '50.0,-140.0;50.0,-50.0;70.0,-50.0;70.0,-140.0', 0),
    ('aca-MMFR', 'MMFR', 200.0, '14.0,-120.0;14.0,-85.0;33.0,-85.0;33.0,-120.0', 0),
    ('aca-SKED', 'SKED', 200.0, '-5.0,-85.0;-5.0,-65.0;15.0,-65.0;15.0,-85.0', 0),
    ('aca-SBAZ', 'SBAZ', 300.0, '-35.0,-75.0;-35.0,-30.0;5.0,-30.0;5.0,-75.0', 0),
    ('aca-UUWV', 'UUWV', 300.0, '40.0,30.0;40.0,140.0;75.0,140.0;75.0,30.0', 0),
    ('aca-FAJA', 'FAJA', 250.0, '-35.0,10.0;-35.0,40.0;-20.0,40.0;-20.0,10.0', 0),
    ('aca-FZZA', 'FZZA', 250.0, '-20.0,10.0;-20.0,40.0;15.0,40.0;15.0,10.0', 0),
    ('aca-RJJJ', 'RJJJ', 250.0, '25.0,125.0;25.0,150.0;45.0,150.0;45.0,125.0', 0),
    ('aca-ZBAA', 'ZBAA', 300.0, '15.0,75.0;15.0,135.0;55.0,135.0;55.0,75.0', 0),
    ('aca-WSJC', 'WSJC', 250.0, '-10.0,90.0;-10.0,120.0;15.0,120.0;15.0,90.0', 0),
    ('aca-YBBB', 'YBBB', 300.0, '-45.0,110.0;-45.0,160.0;-10.0,160.0;-10.0,110.0', 0);

-- ============================================================
-- AIRPORTS (TestSet00_Airports.xml — ICAO = 'X' + IATA)
-- ============================================================

INSERT INTO Airport (airport_id, name, town, country, iataCode, icaoCode, elevation, latitude, longitude, airControlArea_air_control_area_id, version)
VALUES
    ('apt-OPO', 'Francisco de Sá Carneiro Airport', 'Porto', 'Portugal', 'OPO', 'XOPO', 69.0, 41.2481003, -8.6813898, 'aca-LPPC', 0),
    ('apt-LIS', 'Humberto Delgado Airport', 'Lisbon', 'Portugal', 'LIS', 'XLIS', 114.0, 38.7812996, -9.1359196, 'aca-LPPC', 0),
    ('apt-PDL', 'Joao Paulo II Airport', 'Ponta Delgada', 'Portugal', 'PDL', 'XPDL', 79.0, 37.7411995, -25.6979008, 'aca-LPPO', 0),
    ('apt-FNC', 'Madeira Airport', 'Funchal', 'Portugal', 'FNC', 'XFNC', 59.0, 32.6978989, -16.7744999, 'aca-LPMA', 0),
    ('apt-MAD', 'Adolfo Suárez Madrid Barajas International Airport', 'Madrid', 'Spain', 'MAD', 'XMAD', 610.0, 40.4936, -3.56676, 'aca-LECM', 0),
    ('apt-PMI', 'Palma de Mallorca Airport', 'Palma de Mallorca', 'Spain', 'PMI', 'XPMI', 7.0, 39.5517006, 2.7388101, 'aca-LECM', 0),
    ('apt-LPA', 'Gran Canaria Airport', 'Las Palmas', 'Spain', 'LPA', 'XLPA', 23.0, 27.9319, -15.3865995, 'aca-GCCC', 0),
    ('apt-CDG', 'Charles de Gaulle International Airport', 'Paris', 'France', 'CDG', 'XCDG', 119.0, 49.0127983, 2.55, 'aca-LFFF', 0),
    ('apt-FRA', 'Frankfurt am Main International Airport', 'Frankfurt am Main', 'Germany', 'FRA', 'XFRA', 111.0, 50.0264015, 8.5431299, 'aca-EDGG', 0),
    ('apt-BER', 'Berlin Brandenburg Airport', 'Berlin', 'Germany', 'BER', 'XBER', 47.0, 52.5166667, 13.4, 'aca-EDGG', 0),
    ('apt-LHR', 'London Heathrow Airport', 'London', 'UK', 'LHR', 'XLHR', 25.0, 51.4706001, -0.461941, 'aca-EGTT', 0),
    ('apt-GLA', 'Glasgow International Airport', 'Glasgow', 'UK', 'GLA', 'XGLA', 8.0, 55.8718987, -4.4330602, 'aca-EGTT', 0),
    ('apt-SNN', 'Shannon Airport', 'Limerick', 'Ireland', 'SNN', 'XSNN', 14.0, 52.7019997, -8.9248199, 'aca-EISN', 0),
    ('apt-KEF', 'Keflavik International Airport', 'Reykjavík', 'Iceland', 'KEF', 'XKEF', 52.0, 63.9850006, -22.6056004, 'aca-BIRD', 0),
    ('apt-TRD', 'Trondheim, Vaernes Airport', 'Trondheim', 'Norway', 'TRD', 'XTRD', 17.0, 63.4578018, 10.9239998, 'aca-ENOR', 0),
    ('apt-AMS', 'Amsterdam Schiphol Airport', 'Haarlemmermeer', 'Netherlands', 'AMS', 'XAMS', -3.0, 52.3086014, 4.7638898, 'aca-EHAA', 0),
    ('apt-ZRH', 'Zurich Airport', 'Zurich', 'Switzerland', 'ZRH', 'XZRH', 432.0, 47.4646988, 8.5491695, 'aca-LSAS', 0),
    ('apt-MXP', 'Malpensa International Airport', 'Milano', 'Italy', 'MXP', 'XMXP', 234.0, 45.6306, 8.7281103, 'aca-LIBB', 0),
    ('apt-FCO', 'Leonardo Da Vinci (Fiumicino) International Airport', 'Rome', 'Italy', 'FCO', 'XFCO', 5.0, 41.8044444, 12.2508333, 'aca-LIBB', 0),
    ('apt-JFK', 'John F Kennedy International Airport', 'New York', 'USA', 'JFK', 'XJFK', 4.0, 40.639801, -73.7789002, 'aca-KUSA', 0),
    ('apt-ATL', 'Hartsfield Jackson Atlanta International Airport', 'Atlanta', 'USA', 'ATL', 'XATL', 313.0, 33.6366997, -84.4281006, 'aca-KUSA', 0),
    ('apt-IAD', 'Washington Dulles International Airport', 'Washington DC', 'USA', 'IAD', 'XIAD', 95.0, 38.9445, -77.4558029, 'aca-KUSA', 0),
    ('apt-ORD', 'Chicago O''Hare International Airport', 'Chicago', 'USA', 'ORD', 'XORD', 204.0, 41.9785996, -87.9048004, 'aca-KUSA', 0),
    ('apt-LAX', 'Los Angeles International Airport', 'Los Angeles', 'USA', 'LAX', 'XLAX', 38.0, 33.9425011, -118.4079971, 'aca-KUSA', 0),
    ('apt-DFW', 'Dallas Fort Worth International Airport', 'Dallas', 'USA', 'DFW', 'XDFW', 185.0, 32.896801, -97.038002, 'aca-KUSA', 0),
    ('apt-DEN', 'Denver International Airport', 'Denver', 'USA', 'DEN', 'XDEN', 1655.0, 39.8616982, -104.6729965, 'aca-KUSA', 0),
    ('apt-SFO', 'San Francisco International Airport', 'San Francisco', 'USA', 'SFO', 'XSFO', 3.96, 37.6189995, -122.375, 'aca-KUSA', 0),
    ('apt-LAS', 'McCarran International Airport', 'Las Vegas', 'USA', 'LAS', 'XLAS', 665.0, 36.080101, -115.1520004, 'aca-KUSA', 0),
    ('apt-MIA', 'Miami International', 'Miami', 'USA', 'MIA', 'XMIA', 3.0, 25.7931995, -80.2906036, 'aca-KUSA', 0),
    ('apt-EWR', 'Newark Liberty International Airport', 'Newark', 'USA', 'EWR', 'XEWR', 5.0, 40.6925011, -74.1687012, 'aca-KUSA', 0),
    ('apt-HNL', 'Honolulu International Airport', 'Honolulu', 'USA', 'HNL', 'XHNL', 4.0, 21.3187008, -157.9219971, 'aca-PHZH', 0),
    ('apt-BOS', 'General Edward Lawrence Logan International Airport', 'Boston', 'USA', 'BOS', 'XBOS', 6.1, 42.3642998, -71.0052033, 'aca-KUSA', 0),
    ('apt-MCI', 'Kansas City International Airport', 'Kansas City', 'USA', 'MCI', 'XMCI', 313.0, 39.2975998, -94.7138977, 'aca-KUSA', 0),
    ('apt-YYC', 'Calgary International Airport', 'Calgary', 'Canada', 'YYC', 'XYYC', 1099.0, 51.1138992, -114.0199966, 'aca-CZEG', 0),
    ('apt-YUL', 'Montréal–Pierre Elliott Trudeau International Airport', 'Montreal', 'Canada', 'YUL', 'XYUL', 36.0, 45.4706001, -73.740799, 'aca-CZEG', 0),
    ('apt-MEX', 'Licenciado Benito Juarez International Airport', 'Ciudad de Mexico', 'Mexico', 'MEX', 'XMEX', 2238.0, 19.4363003, -99.0720978, 'aca-MMFR', 0),
    ('apt-BOG', 'El Dorado International Airport', 'Bogota', 'Colombia', 'BOG', 'XBOG', 2548.0, 4.70159, -74.1469, 'aca-SKED', 0),
    ('apt-GIG', 'Rio de Janeiro – Antonio Carlos Jobim International Airport', 'Rio de Janeiro', 'Brazil', 'GIG', 'XGIG', 9.0, -22.8099995, -43.2505569, 'aca-SBAZ', 0),
    ('apt-REC', 'Guararapes - Gilberto Freyre International Airport', 'Recife', 'Brazil', 'REC', 'XREC', 10.0, -8.1264896, -34.9235992, 'aca-SBAZ', 0),
    ('apt-SVO', 'Sheremetyevo International Airport', 'Moscow', 'Russia', 'SVO', 'XSVO', 192.0, 55.972599, 37.4146004, 'aca-UUWV', 0),
    ('apt-ALA', 'Almaty International Airport', 'Alamaty', 'Kazakhstan', 'ALA', 'XALA', 681.0, 43.3521004, 77.0404968, 'aca-UUWV', 0),
    ('apt-JNB', 'OR Tambo International Airport', 'Johannesburg', 'South Africa', 'JNB', 'XJNB', 1694.0, -26.1392002, 28.2460003, 'aca-FAJA', 0),
    ('apt-FIH', 'Ndjili International Airport', 'Kinshasa', 'Democratic Republic of the Congo', 'FIH', 'XFIH', 313.0, -4.3857498, 15.4446001, 'aca-FZZA', 0),
    ('apt-MPM', 'Maputo International Airport', 'Maputo', 'Mozambique', 'MPM', 'XMPM', 44.0, -25.9207993, 32.5726013, 'aca-FAJA', 0),
    ('apt-HND', 'Tokyo International Airport', 'Tokyo', 'Japan', 'HND', 'XHND', 6.0, 35.5522995, 139.7799988, 'aca-RJJJ', 0),
    ('apt-HKG', 'Hong Kong International Airport', 'Hong Kong', 'Hong Kong', 'HKG', 'XHKG', 9.0, 22.308321, 113.919649, 'aca-ZBAA', 0),
    ('apt-PEK', 'Beijing Capital International Airport', 'Beijing', 'China', 'PEK', 'XPEK', 35.0, 40.080101, 116.5849991, 'aca-ZBAA', 0),
    ('apt-CTU', 'Chengdu Shuangliu International Airport', 'Chengdu', 'China', 'CTU', 'XCTU', 495.0, 30.5785007, 103.9469986, 'aca-ZBAA', 0),
    ('apt-LXA', 'Lhasa Gonggar Airport', 'Lhasa', 'China', 'LXA', 'XLXA', 3570.0, 29.2978001, 90.9119034, 'aca-ZBAA', 0),
    ('apt-SIN', 'Singapore Changi International Airport', 'Singapore', 'Singapore', 'SIN', 'XSIN', 7.0, 1.35019, 103.9940033, 'aca-WSJC', 0),
    ('apt-SYD', 'Sydney Kingsford Smith International Airport', 'Sydney', 'Australia', 'SYD', 'XSYD', 6.0, -33.9460983, 151.177002, 'aca-YBBB', 0),
    ('apt-ASP', 'Alice Springs Airport', 'Alice Springs', 'Australia', 'ASP', 'XASP', 545.0, -23.8066998, 133.9019928, 'aca-YBBB', 0);

-- ============================================================
-- ENGINE CATALOG
-- ============================================================

INSERT INTO EngineModel (engine_model_id, name, type, power, efficiency, thrustSeaLevelStopped, thrustSeaLevelCruise, thrustSpecificFuelConsumption, maker_name, maker_country, fuel_type, density, specificEnergy, version)
VALUES
    ('eng-001', 'CFM56-7B',    'TURBOFAN',  121.0, 0.38, 121000.0,  98000.0, 0.62, 'CFM International',     'France', 'JET_A', 0.80, 43.0, 0),
    ('eng-002', 'CFM56-7B27',  'TURBOFAN',  121.0, 0.38, 121000.0,  98000.0, 0.62, 'CFM International',     'France', 'JET_A', 0.80, 43.0, 0),
    ('eng-003', 'PW127M',      'TURBOPROP',   2.05, 0.35,  20500.0,  16200.0, 0.58, 'Pratt & Whitney Canada','Canada', 'JET_A', 0.80, 43.0, 0),
    ('eng-004', 'PW127F',      'TURBOPROP',   2.05, 0.35,  20500.0,  16200.0, 0.58, 'Pratt & Whitney Canada','Canada', 'JET_A', 0.80, 43.0, 0),
    ('eng-005', 'GE90-115B',   'TURBOFAN',  513.0, 0.40, 513000.0, 430000.0, 0.60, 'General Electric',      'USA',    'JET_A', 0.80, 43.0, 0),
    ('eng-006', 'GE90-110B',   'TURBOFAN',  513.0, 0.40, 513000.0, 430000.0, 0.60, 'General Electric',      'USA',    'JET_A', 0.80, 43.0, 0),
    ('eng-007', 'GE 90B',      'TURBOFAN',  513.0, 0.40, 513000.0, 430000.0, 0.60, 'General Electric',      'USA',    'JET_A', 0.80, 43.0, 0),
    ('eng-008', 'RR Trent 970','TURBOFAN',  340.0, 0.39, 340000.0, 285000.0, 0.61, 'Rolls-Royce',           'UK',     'JET_A', 0.80, 43.0, 0);

-- ============================================================
-- AIRCRAFT MODELS (TestSet00_AircraftModels.xml)
-- ============================================================

INSERT INTO AircraftModel (aircraft_model_id, type, emptyWeight, maximumTakeOffWeight, maximumZeroFuelWeight, maximumFuelCapacity, serviceCeiling, cruiseSpeed, wingArea, wingSpan, dragCoefficient, liftCoefficient, numberOfEngines, modelName, name, country, version)
VALUES
    ('am-777',  'PASSENGER', 19549.8311, 347451.7554,  79877.6163, 145149.5584, 262.55, 247.8,  440.0, 60.93, 0.017, 1.2, 2, '777-200ER', 'Boeing', 'Unknown', 0),
    ('am-A380', 'PASSENGER', 19504.4719, 260815.6128,  48534.3836, 276800.0,    262.55, 250.75, 845.0, 79.75, 0.020, 1.2, 4, 'A380',      'Airbus', 'Unknown', 0);

INSERT INTO aircraftmodel_enginemodel (aircraft_model_id, engine_model_id)
VALUES
    ('am-777',  'eng-007'),
    ('am-A380', 'eng-008');

-- ============================================================
-- FLEET — AIRCRAFT
-- ============================================================

INSERT INTO Aircraft (aircraft_id, registrationNumber, registrationCountry, operationStatus, flightCrewCount, aircraftModel_aircraft_model_id, airTransportCompany_air_transport_company_id, engineModel_engine_model_id, economyClassSeats, businessClassSeats, firstClassSeats, version)
VALUES
    ('ac-001', 'CS-TUA', 'Portugal', 'IN_SERVICE', 2, 'am-777',  'atc-001', 'eng-007', 264, 8,  48, 0),
    ('ac-002', 'CS-TUB', 'Portugal', 'IN_SERVICE', 2, 'am-777',  'atc-001', 'eng-007', 264, 8,  48, 0),
    ('ac-003', 'CS-TKQ', 'Portugal', 'IN_SERVICE', 2, 'am-A380', 'atc-001', 'eng-008', 399, 14, 80, 0);

-- ============================================================
-- CUSTOMER COLLABORATORS
-- ============================================================

INSERT INTO CustomerCollaborator (customer_collaborator_id, user_id, customerType, air_transport_company_id, air_control_area_id, position, active, version)
VALUES
    ('cc-001', 'atcc1@tap.pt',     'AIR_TRANSPORT_COMPANY', 'atc-001', NULL,      'Operations Manager', TRUE, 0),
    ('cc-002', 'atcc1@ryanair.ie', 'AIR_TRANSPORT_COMPANY', 'atc-002', NULL,      'Fleet Manager',      TRUE, 0),
    ('cc-003', 'fco1@aisafe.pt',   'AIR_CONTROL_AREA',       NULL,     'aca-LPPC','Area Supervisor',    TRUE, 0);

-- ============================================================
-- NODES
-- ============================================================

INSERT INTO Node (node_id, designation, latitude, longitude, version)
VALUES
    ('node-001', 'NORTH-1', 40.1234, -8.1234, 0),
    ('node-002', 'NORTH-2', 40.2234, -8.2234, 0),
    ('node-003', 'EAST-1',  39.9234, -7.9234, 0),
    ('node-004', 'WEST-1',  41.1234, -9.2234, 0);

-- ============================================================
-- FLIGHT PLANNING
-- ============================================================

INSERT INTO FlightRoute (flight_route_id, designation, activeFrom, activeUntil, originAirport_airport_id, destinationAirport_airport_id, company_air_transport_company_id, version)
VALUES
    ('fr-001', 'TP123', '2026-05-01', NULL,         'apt-LIS', 'apt-OPO', 'atc-001', 0),
    ('fr-002', 'BA45',  '2026-05-01', NULL,         'apt-LHR', 'apt-LIS', 'atc-006', 0),
    ('fr-003', 'LH999', '2026-05-01', NULL,         'apt-FRA', 'apt-LIS', 'atc-004', 0),
    ('fr-004', 'AF77',  '2026-05-01', '2026-05-20', 'apt-CDG', 'apt-LIS', 'atc-005', 0);

INSERT INTO Flight (flight_id, flightDesignator, type, scheduledDepartureTime, scheduledArrivalTime, aircraft_aircraft_id, version)
VALUES
    ('fl-001', 'TP1234', 'REGULAR', '2026-05-12 08:00:00', '2026-05-12 10:15:00', 'ac-001', 0),
    ('fl-002', 'BA450',  'REGULAR', '2026-05-12 09:30:00', '2026-05-12 12:10:00', 'ac-002', 0),
    ('fl-003', 'LH9991', 'CHARTER', '2026-05-12 13:00:00', '2026-05-12 16:20:00', 'ac-003', 0);

INSERT INTO Pilot (pilot_id, user_id, status, version)
VALUES
    ('pilot-001', 'pilot@tap.com', 'ACTIVE', 0),
    ('pilot-002', 'pilot1@tap.com', 'ACTIVE', 0),
    ('pilot-003', 'pilot2@ryanair.ie', 'ACTIVE', 0),
    ('pilot-004', 'pilot3@lufthansa.de', 'ACTIVE', 0);

INSERT INTO PilotCompanyAssignment (pilot_user_id, company_id)
VALUES
    ('pilot@tap.com', 'atc-001'),
    ('pilot1@tap.com', 'atc-001'),
    ('pilot2@ryanair.ie', 'atc-002'),
    ('pilot3@lufthansa.de', 'atc-004');

INSERT INTO Pilot_AircraftModel (Pilot_pilot_id, certifiedAircraftModels_aircraft_model_id)
VALUES
    ('pilot-001', 'am-777'),
    ('pilot-001', 'am-A380'),
    ('pilot-002', 'am-777'),
    ('pilot-002', 'am-A380'),
    ('pilot-003', 'am-777'),
    ('pilot-003', 'am-A380'),
    ('pilot-004', 'am-777'),
    ('pilot-004', 'am-A380');


SELECT setval('T_ROLESET_SEQ',        50, true);
SELECT setval('T_ROLEASSIGNMENT_SEQ', 50, true);
SELECT setval('FlightLeg_SEQ',        50, true);
SELECT setval('Segment_SEQ',          50, true);
SELECT setval('AltitudeSlot_SEQ',     50, true);
