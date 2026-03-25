# Glossary

**Terms, Expressions and Acronyms (TEA) must be organized alphabetically.**

| **_TEA_** (EN) | **_TEA_** (PT) | **_Description_** (EN) |
|:--|:--|:--|
| **Air Control Area** | **Área de Controlo Aéreo** | Defined region of airspace with a unique code, geographic boundaries, and a minimum required arrival fuel. Manages one or more airports. |
| **Aircraft** | **Aeronave** | Individual aircraft identified by registration number and country, with an operational status and crew count. Belongs to a specific aircraft model. |
| **Aircraft Model** | **Modelo de Aeronave** | Technical specification of a motorised commercial aircraft type, including weight limits, fuel capacity, service ceiling, cruise speed, and aerodynamic coefficients. |
| **Aircraft Position** | **Posição da Aeronave** | Recorded position of an aircraft at a specific timestamp during simulation, including altitude and an associated velocity vector. |
| **Airport** | **Aeroporto** | Physical location for aircraft departure and arrival, identified by name, town, country, IATA code, ICAO code, coordinates, and elevation. |
| **Air Transport Company** | **Empresa de Transporte Aéreo** | Organisation identified by name, IATA code, and ICAO code that employs users, owns aircraft, and operates flight routes. |
| **Altitude Slot** | **Slot de Altitude** | Available altitude window within a segment, defined by minimum altitude, maximum altitude, and width. |
| **Cabin Configuration** | **Configuração de Cabine** | Value object defining the seat distribution of an aircraft across first class, business class, and economy class. |
| **Coordinates** | **Coordenadas** | Value object representing a geographic position through latitude and longitude. Associated with airports and navigation nodes. |
| **Engine Model** | **Modelo de Motor** | Technical specification of an engine including power, fuel type, efficiency, thrust at sea level, and thrust specific fuel consumption. |
| **Flight** | **Voo** | Concrete scheduled trip with a unique designator, type, and scheduled departure and arrival times. Instantiated from a flight route. |
| **Flight Designator** | **Designador de Voo** | Unique flight identifier in the format _xxnnnna_, combining airline code, numeric flight number, and an optional operational suffix. |
| **Flight Leg** | **Perna de Voo** | Single non-stop journey between two airports within a flight plan, containing departure, arrival, route, segments, and fuel information. |
| **Flight Plan** | **Plano de Voo** | Execution blueprint for a flight specifying departure date and time, fuel quantity, passenger and cargo load, allocated aircraft, and assigned pilot. |
| **Flight Route** | **Rota de Voo** | Named route operated by an air transport company, valid between two dates, from which individual flights are instantiated. |
| **Fuel Properties** | **Propriedades do Combustível** | Value object describing the physical characteristics of a fuel type, including density and specific energy. |
| **Node** | **Nó** | Navigation point in the airspace network with a designation and geographic coordinates, used as start or end of a flight segment. |
| **Pilot** | **Piloto** | Specialised user certified to fly one or more aircraft models. Inherits system access credentials from User. |
| **Role** | **Papel** | Function or permission profile assigned to a user in the system (e.g. Admin, Pilot, Flight Control Operator). |
| **Safety Violation** | **Violação de Segurança** | Event detected during simulation where aircraft proximity or behaviour breaches defined safety thresholds, recorded with timestamp and severity. |
| **Segment** | **Segmento** | Linear path connecting two nodes, characterised by altitude slots, slot widths, and wind direction and speed. |
| **Service Ceiling** | **Teto de Serviço** | Maximum altitude at which an aircraft can sustain level flight under standard atmospheric conditions. |
| **Simulation** | **Simulação** | Controlled execution of an airspace model over a defined geographic area and time period, tracking flights and detecting safety violations. |
| **Simulation Report** | **Relatório de Simulação** | Document produced at the end of a simulation summarising total flights, safety violations, and overall validation result. |
| **User** | **Utilizador** | Person registered in the system with a valid email, name, and phone number, requiring authentication to access any functionality. |
| **Velocity Vector** | **Vetor de Velocidade** | Value object capturing the instantaneous movement state of an aircraft during simulation, defined by speed and direction. |
| **Weather Data** | **Dados Meteorológicos** | Meteorological information including date, wind speed, and wind direction, incorporated into a simulation to influence flight behaviour. |
