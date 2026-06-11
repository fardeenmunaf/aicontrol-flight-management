package flight.dsl.mapping;

import aircraft.domain.Aircraft;
import aircraft.repositories.AircraftRepository;
import aircraftmodel.domain.AircraftModel;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import enginemodel.domain.EngineModel;
import flight.dsl.ast.*;
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
            QuantityDTO fuel = new QuantityDTO(
                    legAst.fuel().quantity(),
                    legAst.fuel().unit() != null ? legAst.fuel().unit() : "kg"
            );

            QuantityDTO payload = new QuantityDTO(
                    13000.0, // Default payload for now as it's not in AST
                    "kg"
            );

            ProfileDTO profile = buildProfile(legAst.profile());

            List<SimulationSegmentDTO> segmentDTOs = new ArrayList<>();
            for (SegmentAst segAst : legAst.segments()) {
                CoordinateDTO start = toCoordinate(segAst.from());
                CoordinateDTO end = toCoordinate(segAst.to());
                AltitudeSlotDTO altSlot = buildAltSlot(segAst);
                WindDTO wind = buildWind(segAst);

                segmentDTOs.add(new SimulationSegmentDTO(
                        segAst.mode() != null ? segAst.mode().name().toLowerCase() : "cruise",
                        start,
                        end,
                        altSlot,
                        wind
                ));
            }

            legDTOs.add(new SimulationLegDTO(
                    legAst.departure().airportCode(),
                    legAst.departure().date(),
                    legAst.departure().time(),
                    legAst.arrival().airportCode(),
                    legAst.arrival().date(),
                    legAst.arrival().time(),
                    fuel,
                    payload,
                    profile,
                    segmentDTOs
            ));
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
        String json = objectMapper.writeValueAsString(outList);
        java.nio.file.Files.writeString(outFile.toPath(), json, java.nio.charset.StandardCharsets.UTF_8);
    }

    private CoordinateDTO toCoordinate(CoordinateAst c) {
        String unit = c.altitudeUnit() != null ? c.altitudeUnit() : "m";
        return new CoordinateDTO(c.latitude(), c.longitude(), new MeasureDTO(c.altitude(), unit));
    }

    private ProfileDTO buildProfile(ProfileAst profile) {
        if (profile == null) {
            return null;
        }

        List<ClimbEntryDTO> climbs = new ArrayList<>();
        if (profile.climbEntries() != null) {
            for (ClimbEntryAst c : profile.climbEntries()) {
                climbs.add(new ClimbEntryDTO(
                        new MeasureDTO(c.altitude(), c.altitudeUnit() != null ? c.altitudeUnit() : "m"),
                        new MeasureDTO(c.speed(), c.speedUnit() != null ? c.speedUnit() : "kts")
                ));
            }
        }

        CruiseEntryDTO cruise = null;
        if (profile.cruiseEntries() != null && !profile.cruiseEntries().isEmpty()) {
            CruiseEntryAst c = profile.cruiseEntries().get(0);
            cruise = new CruiseEntryDTO(
                    new MeasureDTO(c.speed(), c.speedUnit() != null ? c.speedUnit() : "kts")
            );
        }

        List<DescendEntryDTO> descends = new ArrayList<>();
        if (profile.descendEntries() != null) {
            for (DescendEntryAst d : profile.descendEntries()) {
                descends.add(new DescendEntryDTO(
                        new MeasureDTO(d.altitude(), d.altitudeUnit() != null ? d.altitudeUnit() : "m"),
                        new MeasureDTO(d.speed(), d.speedUnit() != null ? d.speedUnit() : "kts"),
                        new MeasureDTO(d.rate(), "m/s")
                ));
            }
        }

        return new ProfileDTO(climbs, cruise, descends);
    }

    private AltitudeSlotDTO buildAltSlot(SegmentAst segAst) {
        if (segAst.altitudeSlots() == null || segAst.altitudeSlots().isEmpty()) {
            return null;
        }
        AltitudeSlotAst slot = segAst.altitudeSlots().get(0);
        return new AltitudeSlotDTO(
                new MeasureDTO(slot.min(), slot.minUnit() != null ? slot.minUnit() : "m"),
                new MeasureDTO(slot.max(), slot.maxUnit() != null ? slot.maxUnit() : "m"),
                new MeasureDTO(slot.width(), slot.widthUnit() != null ? slot.widthUnit() : "m")
        );
    }

    private WindDTO buildWind(SegmentAst segAst) {
        WindAst wind = segAst.wind();
        if (wind == null) {
            return null;
        }
        return new WindDTO(wind.direction(), new MeasureDTO(wind.speed(), "m/s"));
    }
}
