package flight.dsl.mapping;

import aircraft.domain.Aircraft;
import aircraft.repositories.AircraftRepository;
import aircraftmodel.domain.AircraftModel;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import enginemodel.domain.EngineModel;
import flight.dsl.ast.FlightPlanAst;
import flight.dsl.ast.LegAst;
import flight.dsl.ast.SegmentAst;
import flightPlan.dto.export.SimulationFlightPlanDTO;
import flightPlan.dto.export.SimulationFlightPlanDTO.*;
import shared.domain.AircraftId;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ExportSimulationFlightPlanService {

    private final AircraftRepository aircraftRepository;
    private final ObjectMapper objectMapper;
    private final String outputDirectory;

    public ExportSimulationFlightPlanService(AircraftRepository aircraftRepository, String outputDirectory) {
        this.aircraftRepository = aircraftRepository;
        
        // If outputDirectory starts with "aisafe/", it might be wrong when running from the aisafe folder.
        // We ensure it points to the correct place.
        if (outputDirectory.startsWith("aisafe/")) {
            outputDirectory = outputDirectory.substring(7);
        }
        this.outputDirectory = outputDirectory;
        
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .enable(SerializationFeature.INDENT_OUTPUT);
        
        File dir = new File(this.outputDirectory);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    public void export(FlightPlanAst ast) throws IOException {
        Optional<Aircraft> optAircraft = aircraftRepository.findByRegistrationNumber(ast.aircraftId());
        if (optAircraft.isEmpty()) {
            try {
                optAircraft = aircraftRepository.ofIdentity(AircraftId.valueOf(ast.aircraftId()));
            } catch (Exception e) {
                // Ignore UUID parsing errors etc.
            }
        }
        
        if (optAircraft.isEmpty()) {
            throw new IllegalArgumentException("Aircraft not found for ID or Registration Number: " + ast.aircraftId());
        }
        
        Aircraft aircraft = optAircraft.get();
        AircraftModel aircraftModel = aircraft.aircraftModel();
        EngineModel engineModel = aircraft.engineModel();

        RouteReferenceDTO route = new RouteReferenceDTO(
                ast.routeReference(),
                ast.routeOrigin(),
                ast.routeDestination()
        );

        AircraftSpecsDTO aircraftSpecs = new AircraftSpecsDTO(
                aircraftModel.emptyWeight(),
                aircraftModel.wingArea(),
                10.24, // aspectRatio default
                0.85,  // oswaldEfficiency default
                0.0265, // cd0 default
                aircraftModel.numberOfEngines()
        );

        EngineSpecsDTO engineSpecs = new EngineSpecsDTO(
                engineModel.thrustSeaLevelStopped(),
                engineModel.thrustSeaLevelCruise(),
                0.9, // thrustLapseRate default
                engineModel.thrustSpecificFuelConsumption()
        );

        List<SimulationLegDTO> legDTOs = new ArrayList<>();
        for (LegAst legAst : ast.legs()) {
            AirportTimeDTO departure = new AirportTimeDTO(
                    legAst.departure().airportCode(),
                    legAst.departure().date(),
                    legAst.departure().time()
            );

            AirportTimeDTO arrival = new AirportTimeDTO(
                    legAst.arrival().airportCode(),
                    legAst.arrival().date(),
                    legAst.arrival().time()
            );

            QuantityUnitDTO fuel = new QuantityUnitDTO(
                    legAst.fuel().quantity(),
                    legAst.fuel().unit() != null ? legAst.fuel().unit() : "kg"
            );

            QuantityUnitDTO payload = new QuantityUnitDTO(
                    13000.0, // Default payload for now as it's not in AST
                    "kg"
            );

            List<SimulationSegmentDTO> segmentDTOs = new ArrayList<>();
            for (SegmentAst segAst : legAst.segments()) {
                CoordinateDTO from = new CoordinateDTO(
                        segAst.from().latitude(),
                        segAst.from().longitude(),
                        segAst.from().altitude(),
                        segAst.from().altitudeUnit() != null ? segAst.from().altitudeUnit() : "m"
                );

                CoordinateDTO to = new CoordinateDTO(
                        segAst.to().latitude(),
                        segAst.to().longitude(),
                        segAst.to().altitude(),
                        segAst.to().altitudeUnit() != null ? segAst.to().altitudeUnit() : "m"
                );

                segmentDTOs.add(new SimulationSegmentDTO(
                        segAst.mode() != null ? segAst.mode().name().toLowerCase() : "cruise",
                        from,
                        to
                ));
            }

            legDTOs.add(new SimulationLegDTO(departure, arrival, fuel, payload, segmentDTOs));
        }

        SimulationFlightPlanDTO dto = new SimulationFlightPlanDTO(
                ast.flightDesignator(),
                ast.type().name().toLowerCase(),
                route,
                aircraftSpecs,
                engineSpecs,
                legDTOs
        );

        // Export as an array with a single item to match TB0001.json
        List<SimulationFlightPlanDTO> outList = List.of(dto);
        File outFile = new File(outputDirectory, ast.flightDesignator() + ".json");
        objectMapper.writeValue(outFile, outList);
    }
}
