package airtransport.service;

import aircraft.domain.Aircraft;
import aircraft.domain.AircraftOperationStatus;
import aircraft.domain.CabinConfiguration;
import aircraftmodel.domain.AircraftModel;
import airport.domain.Airport;
import airtransportcompany.domain.AirTransportCompany;
import enginemodel.domain.EngineModel;
import flight.dsl.mapping.FlightPlanCreationService;
import flight.dsl.mapping.ValidateFlightPlanService;
import flightPlan.application.FlightPlanValidationResult;
import flightPlan.application.ValidateFlightPlanController;
import flightPlan.domain.AltitudeSlot;
import flightPlan.domain.FlightLeg;
import flightPlan.domain.FlightPlan;
import flightPlan.domain.FlightPlanStatus;
import flightPlan.domain.Segment;
import flightRoute.domain.FlightRoute;
import flightRoute.domain.RouteDesignation;
import infrastructure.persistence.PersistenceContext;
import jpa.JpaPilotCompanyAssignmentRepository;
import jpa.PersistenceSettings;
import node.domain.Node;
import pilot.domain.Pilot;
import pilot.domain.PilotCompanyAssignment;
import shared.domain.AircraftId;
import shared.domain.AircraftModelId;
import shared.domain.AirTransportCompanyId;
import shared.domain.EngineModelId;
import shared.domain.FlightPlanId;
import shared.domain.FlightRouteId;
import shared.domain.UserId;
import weatherData.domain.WeatherData;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;

public class LocalAirTransportService implements AirTransportService {

    private final AirTransportCompanyId companyId;
    private final String companyIcaoCode;

    public LocalAirTransportService(final AirTransportCompanyId companyId, final String companyIcaoCode) {
        this.companyId = companyId;
        this.companyIcaoCode = companyIcaoCode;
    }

    @Override
    public String listAircraftModels() {
        final StringJoiner sb = new StringJoiner("\n");
        PersistenceContext.repositories().aircraftModels().findAll().forEach(m -> {
            final String engines = m.supportedEngineModels().stream()
                    .map(e -> e.identity().toString() + ":" + e.name())
                    .collect(Collectors.joining("|"));
            sb.add(m.aircraftModelId().toString() + ";"
                    + m.modelName() + ";"
                    + m.maker().name() + ";"
                    + m.numberOfEngines() + ";"
                    + m.maximumZeroFuelWeight() + ";"
                    + m.emptyWeight() + ";"
                    + engines);
        });
        return sb.toString();
    }

    @Override
    public String listFleet() {
        final StringJoiner sb = new StringJoiner("\n");
        PersistenceContext.repositories().aircraft()
                .findByAirTransportCompanyId(companyId)
                .forEach(a -> {
                    final CabinConfiguration cc = a.cabinConfiguration();
                    sb.add(a.registrationNumber() + ";"
                            + a.aircraftModel().modelName() + ";"
                            + a.aircraftModel().maker().name() + ";"
                            + a.operationStatus() + ";"
                            + cc.economyClassSeats() + ";"
                            + cc.businessClassSeats() + ";"
                            + cc.firstClassSeats() + ";"
                            + a.registrationCountry() + ";"
                            + a.identity().toString());
                });
        return sb.toString();
    }

    @Override
    public String registerAircraft(final String reg, final String country, final String status,
                                   final int crew, final String modelId, final String engineId,
                                   final int econ, final int bus, final int first) {
        try {
            if (PersistenceContext.repositories().aircraft().findByRegistrationNumber(reg).isPresent()) {
                return "ERROR: Aircraft with registration '" + reg + "' already exists";
            }

            final AircraftModel model = PersistenceContext.repositories().aircraftModels()
                    .ofIdentity(AircraftModelId.valueOf(modelId))
                    .orElseThrow(() -> new IllegalArgumentException("Aircraft model not found"));

            final EngineModel engine = PersistenceContext.repositories().engineModels()
                    .ofIdentity(EngineModelId.valueOf(engineId))
                    .orElseThrow(() -> new IllegalArgumentException("Engine model not found"));

            final AirTransportCompany company = PersistenceContext.repositories().airTransportCompanies()
                    .ofIdentity(companyId)
                    .orElseThrow(() -> new IllegalStateException("Company not found"));

            final double payloadBudgetKg = model.maximumZeroFuelWeight() - model.emptyWeight() - 500.0;
            final int estimatedMaxPeople = (int) Math.floor(Math.max(0.0, payloadBudgetKg) / 100.0);
            if ((econ + bus + first + crew) > estimatedMaxPeople) {
                return "ERROR: Total people exceed estimated limit (" + estimatedMaxPeople + ") for this model";
            }

            final AircraftOperationStatus opStatus = AircraftOperationStatus.valueOf(status);
            final CabinConfiguration cabin = new CabinConfiguration(econ, first, bus);
            final Aircraft aircraft = new Aircraft(
                    reg.trim(), country.trim(), opStatus, crew, model, engine, cabin, company);
            PersistenceContext.repositories().aircraft().save(aircraft);
            return "Aircraft '" + reg + "' registered successfully.";
        } catch (final Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }

    @Override
    public String decommissionAircraft(final String aircraftId) {
        try {
            final Aircraft aircraft = PersistenceContext.repositories().aircraft()
                    .ofIdentity(AircraftId.valueOf(aircraftId))
                    .orElseThrow(() -> new IllegalArgumentException("Aircraft not found"));

            if (!aircraft.airTransportCompany().identity().equals(companyId)) {
                return "ERROR: Aircraft does not belong to your company";
            }
            if (aircraft.operationStatus() == AircraftOperationStatus.DECOMISSIONED) {
                return "ERROR: Aircraft is already decommissioned";
            }

            aircraft.decommission();
            PersistenceContext.repositories().aircraft().save(aircraft);
            return "Aircraft decommissioned successfully.";
        } catch (final Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }

    @Override
    public String listAirports() {
        final StringJoiner sb = new StringJoiner("\n");
        PersistenceContext.repositories().airports().findAll().forEach(a ->
                sb.add(a.iataCode() + ";" + a.name() + ";" + a.town() + ";" + a.country()));
        return sb.toString();
    }

    @Override
    public String listFlightRoutes() {
        final StringJoiner sb = new StringJoiner("\n");
        PersistenceContext.repositories().flightRoutes().findByCompanyId(companyId).forEach(r ->
                sb.add(r.identity().toString() + ";"
                        + r.designation().toString() + ";"
                        + r.originAirport().iataCode() + ";"
                        + r.destinationAirport().iataCode() + ";"
                        + r.activeFrom() + ";"
                        + r.originAirport().coordinates().latitude() + ";"
                        + r.originAirport().coordinates().longitude() + ";"
                        + r.originAirport().elevation() + ";"
                        + r.destinationAirport().coordinates().latitude() + ";"
                        + r.destinationAirport().coordinates().longitude() + ";"
                        + r.destinationAirport().elevation() + ";"
                        + companyIcaoCode + ";"
                        + r.isActive()));
        return sb.toString();
    }

    @Override
    public String createFlightRoute(final String designation, final String originIata,
                                    final String destIata, final String activeFromStr) {
        try {
            final AirTransportCompany company = PersistenceContext.repositories().airTransportCompanies()
                    .ofIdentity(companyId)
                    .orElseThrow(() -> new IllegalStateException("Company not found"));

            final RouteDesignation rd = new RouteDesignation(designation);
            if (PersistenceContext.repositories().flightRoutes().findByDesignation(rd).isPresent()) {
                return "ERROR: Route with designation '" + designation + "' already exists";
            }

            final String originCode = originIata.trim().toUpperCase();
            final String destCode = destIata.trim().toUpperCase();
            if (originCode.equals(destCode)) {
                return "ERROR: Origin and destination must be different";
            }

            final Airport origin = PersistenceContext.repositories().airports().findByIataCode(originCode)
                    .orElseThrow(() -> new IllegalArgumentException("Origin airport not found: " + originCode));
            final Airport destination = PersistenceContext.repositories().airports().findByIataCode(destCode)
                    .orElseThrow(() -> new IllegalArgumentException("Destination airport not found: " + destCode));

            final LocalDate activeFrom = activeFromStr == null || activeFromStr.isBlank()
                    ? LocalDate.now()
                    : LocalDate.parse(activeFromStr.trim());

            final FlightRoute route = new FlightRoute(
                    FlightRouteId.newId(), rd, activeFrom, origin, destination, company);
            PersistenceContext.repositories().flightRoutes().save(route);
            return "Route " + designation + " (" + originCode + " -> " + destCode + ") created.";
        } catch (final Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }

    @Override
    public String deleteFlightRoute(final String routeId, final String deactivationDateStr) {
        try {
            final FlightRoute route = PersistenceContext.repositories().flightRoutes()
                    .ofIdentity(FlightRouteId.valueOf(routeId))
                    .orElseThrow(() -> new IllegalArgumentException("Route not found"));

            if (!route.company().identity().equals(companyId)) {
                return "ERROR: Route does not belong to your company";
            }

            final LocalDate date = LocalDate.parse(deactivationDateStr.trim());
            final Instant cutoff = date.atStartOfDay(ZoneOffset.UTC).toInstant();
            final Iterable<FlightPlan> planned = PersistenceContext.repositories().flightPlans()
                    .findByRouteIdAndDepartureAfter(route.designation().value(), cutoff);
            if (planned.iterator().hasNext()) {
                return "ERROR: Cannot deactivate route: planned flights exist after " + date;
            }

            route.deactivateFrom(date);
            PersistenceContext.repositories().flightRoutes().save(route);
            return "Route " + route.designation() + " deactivated from " + date + ".";
        } catch (final Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }

    @Override
    public String listFlightPlans() {
        final Set<String> companyRegs = companyAircraftRegistrations();
        final StringJoiner sb = new StringJoiner("\n");
        PersistenceContext.repositories().flightPlans().findAll().forEach(p -> {
            if (companyRegs.contains(p.aircraftId())) {
                sb.add(p.identity().toString() + ";"
                        + p.flightDesignator() + ";"
                        + p.status() + ";"
                        + p.routeOrigin() + ";"
                        + p.routeDestination() + ";"
                        + p.departureTime());
            }
        });
        return sb.toString();
    }

    @Override
    public String createFlightPlan(final String dslText) {
        try {
            final FlightPlanCreationService service = new FlightPlanCreationService(
                    PersistenceContext.repositories().flightPlans(),
                    PersistenceContext.repositories().airports(),
                    PersistenceContext.repositories().nodes(),
                    PersistenceContext.repositories().aircraft());
            final flight.dsl.mapping.CreationResult result = service.createFromText(dslText);
            final StringBuilder sb = new StringBuilder();
            if (result.hasErrors()) {
                sb.append("Errors:\n");
                result.errors().forEach(e -> sb.append("  - ").append(e).append("\n"));
            }
            if (!result.created().isEmpty()) {
                sb.append("Created ").append(result.created().size()).append(" flight plan(s):\n");
                result.created().forEach(p -> sb.append("  + ").append(p.flightDesignator()).append("\n"));
            } else if (!result.hasErrors()) {
                sb.append("No flight plans were created.");
            }
            return sb.toString().trim();
        } catch (final Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }

    @Override
    public String validateFlightPlan(final String planId) {
        try {
            final FlightPlan plan = findCompanyPlan(planId);
            if (plan == null) return "ERROR: Flight plan not found or not your company's";
            if (plan.status() != FlightPlanStatus.DRAFT) {
                return "ERROR: Only DRAFT plans can be validated (current status: " + plan.status() + ")";
            }

            final ValidateFlightPlanService service = new ValidateFlightPlanService(
                    new AtccValidateController(plan),
                    PersistenceContext.repositories().airports());

            final FlightPlanValidationResult result = service.validate(plan.identity());
            return formatValidationResult(result, plan.flightDesignator());
        } catch (final Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }

    @Override
    public String addWeatherData(final String planId) {
        try {
            final FlightPlan plan = findCompanyPlan(planId);
            if (plan == null) return "ERROR: Flight plan not found or not your company's";

            boolean testWasVoided = false;
            int added = 0;

            for (final WeatherData wd : PersistenceContext.repositories().weatherData().findAll()) {
                final Instant wdStart = LocalDateTime.of(wd.date(), wd.startHour()).toInstant(ZoneOffset.UTC);
                final Instant wdEnd   = LocalDateTime.of(wd.date(), wd.endHour()).toInstant(ZoneOffset.UTC);

                for (final FlightLeg leg : plan.legs()) {
                    if (!leg.departureTime().isBefore(wdEnd) || !leg.arrivalTime().isAfter(wdStart)) continue;
                    for (final Segment seg : leg.segments()) {
                        if (!altitudeOverlaps(seg, wd)) continue;
                        final Optional<Node> start = PersistenceContext.repositories().nodes().ofIdentity(seg.startNodeId());
                        final Optional<Node> end   = PersistenceContext.repositories().nodes().ofIdentity(seg.endNodeId());
                        final boolean inBox =
                                start.map(n -> inBoundingBox(n.coordinates().latitude(), n.coordinates().longitude(), wd)).orElse(false)
                             || end.map(n   -> inBoundingBox(n.coordinates().latitude(), n.coordinates().longitude(), wd)).orElse(false);
                        if (inBox) {
                            final boolean voided = plan.addWeatherDataToSegment(seg, wd.identity());
                            testWasVoided = testWasVoided || voided;
                            added++;
                        }
                    }
                }
            }

            if (added > 0) {
                PersistenceContext.repositories().flightPlans().save(plan);
            }

            if (added == 0) return "No weather data matched this plan's route/altitude/time.";
            final String extra = testWasVoided ? " (previous test result voided — re-validation required)" : "";
            return added + " weather record(s) added." + extra;
        } catch (final Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }

    @Override
    public String listCompanyPilots() {
        final Set<String> myPilotIds = assignments().findByCompanyId(companyId).stream()
                .map(a -> a.pilotUserId().toString()).collect(Collectors.toSet());
        final StringJoiner sb = new StringJoiner("\n");
        PersistenceContext.repositories().pilots().findAll().forEach(p -> {
            if (myPilotIds.contains(p.userId().toString())) {
                final String models = p.certifiedAircraftModels().stream()
                        .map(m -> m.modelName()).sorted().collect(Collectors.joining(","));
                sb.add(p.userId().toString() + ";" + p.status().name() + ";" + models);
            }
        });
        return sb.toString();
    }

    @Override
    public String listAvailablePilots() {
        final Set<String> assigned = new HashSet<>();
        assignments().findAll().forEach(a -> assigned.add(a.pilotUserId().toString()));
        final StringJoiner sb = new StringJoiner("\n");
        PersistenceContext.repositories().pilots().findAll().forEach(p -> {
            if (!assigned.contains(p.userId().toString())) {
                final String models = p.certifiedAircraftModels().stream()
                        .map(m -> m.modelName()).sorted().collect(Collectors.joining(","));
                sb.add(p.userId().toString() + ";" + p.status().name() + ";" + models);
            }
        });
        return sb.toString();
    }

    @Override
    public String addPilotToCompany(final String userId) {
        try {
            final Pilot pilot = PersistenceContext.repositories().pilots()
                    .findByUserId(UserId.valueOf(userId))
                    .orElseThrow(() -> new IllegalArgumentException("Pilot not found: " + userId));
            assignments().save(new PilotCompanyAssignment(pilot.userId(), companyId));
            return "Pilot " + userId + " added to company roster.";
        } catch (final Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }

    @Override
    public String removePilotFromCompany(final String userId) {
        try {
            final JpaPilotCompanyAssignmentRepository repo = assignments();
            repo.ofIdentity(UserId.valueOf(userId)).ifPresentOrElse(
                    repo::delete,
                    () -> { throw new IllegalArgumentException("Pilot not assigned to company"); });
            return "Pilot " + userId + " removed from company roster.";
        } catch (final Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }

    @Override
    public String activatePilot(final String userId) {
        try {
            final Pilot pilot = findCompanyPilot(userId);
            if (pilot == null) return "ERROR: Pilot not found in your company roster";
            pilot.activate();
            PersistenceContext.repositories().pilots().save(pilot);
            return "Pilot " + userId + " activated.";
        } catch (final Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }

    @Override
    public String deactivatePilot(final String userId) {
        try {
            final Pilot pilot = findCompanyPilot(userId);
            if (pilot == null) return "ERROR: Pilot not found in your company roster";

            final boolean hasPlans = PersistenceContext.repositories().flightPlans()
                    .findByPilotRef(userId).iterator().hasNext();
            if (hasPlans) return "ERROR: Cannot deactivate: pilot has flight plans assigned";

            pilot.deactivate();
            PersistenceContext.repositories().pilots().save(pilot);
            return "Pilot " + userId + " deactivated.";
        } catch (final Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }

    // --- Extra methods for ConsoleFlightDslBuilder (local mode only) ---

    public List<Aircraft> getCompanyAircraft() {
        return PersistenceContext.repositories().aircraft().findByAirTransportCompanyId(companyId);
    }

    public List<FlightRoute> getCompanyRoutes() {
        final List<FlightRoute> routes = new ArrayList<>();
        PersistenceContext.repositories().flightRoutes().findByCompanyId(companyId).forEach(routes::add);
        return routes;
    }

    public String getCompanyIcaoCode() {
        return companyIcaoCode;
    }

    // --- Private helpers ---

    private JpaPilotCompanyAssignmentRepository assignments() {
        return new JpaPilotCompanyAssignmentRepository(PersistenceSettings.getPersistenceUnitName());
    }

    private Set<String> companyAircraftRegistrations() {
        return PersistenceContext.repositories().aircraft()
                .findByAirTransportCompanyId(companyId).stream()
                .map(Aircraft::registrationNumber)
                .collect(Collectors.toSet());
    }

    private FlightPlan findCompanyPlan(final String planId) {
        final Set<String> companyRegs = companyAircraftRegistrations();
        return PersistenceContext.repositories().flightPlans()
                .findByFlightPlanId(FlightPlanId.valueOf(planId))
                .filter(p -> companyRegs.contains(p.aircraftId()))
                .orElse(null);
    }

    private Pilot findCompanyPilot(final String userId) {
        final Set<String> myPilotIds = assignments().findByCompanyId(companyId).stream()
                .map(a -> a.pilotUserId().toString()).collect(Collectors.toSet());
        if (!myPilotIds.contains(userId)) return null;
        return PersistenceContext.repositories().pilots()
                .findByUserId(UserId.valueOf(userId)).orElse(null);
    }

    private static boolean altitudeOverlaps(final Segment seg, final WeatherData wd) {
        if (seg.altitudeSlots().isEmpty()) return true;
        final double segMin = seg.altitudeSlots().stream().mapToDouble(AltitudeSlot::minimumAltitude).min().orElse(0);
        final double segMax = seg.altitudeSlots().stream().mapToDouble(AltitudeSlot::maximumAltitude).max().orElse(Double.MAX_VALUE);
        return segMin <= wd.altitudeSup() && segMax >= wd.altitudeInf();
    }

    private static boolean inBoundingBox(final double lat, final double lon, final WeatherData wd) {
        return lat >= Math.min(wd.latitude1(), wd.latitude2())
            && lat <= Math.max(wd.latitude1(), wd.latitude2())
            && lon >= Math.min(wd.longitude1(), wd.longitude2())
            && lon <= Math.max(wd.longitude1(), wd.longitude2());
    }

    private static String formatValidationResult(final FlightPlanValidationResult result, final String designator) {
        final StringBuilder sb = new StringBuilder();
        if (!result.dslErrors().isEmpty()) {
            sb.append("DSL Validation FAILED:\n");
            result.dslErrors().forEach(e -> sb.append("  [DSL] ").append(e).append("\n"));
        } else {
            sb.append("DSL Validation: PASSED\n");
        }
        if (result.physicalCheckRan()) {
            if (!result.physicalFailures().isEmpty()) {
                sb.append("Physical Validation FAILED:\n");
                result.physicalFailures().forEach(f -> sb.append("  [PHYSICAL] ").append(f).append("\n"));
            } else {
                sb.append("Physical Validation: PASSED\n");
            }
        }
        sb.append(result.passed()
                ? "RESULT: PASSED - Flight plan " + designator + " is now TESTED."
                : "RESULT: FAILED - Flight plan " + designator + " remains DRAFT.");
        return sb.toString();
    }

    // Bypass controller: skips role-based auth, uses company-scoped ownership check
    private static final class AtccValidateController extends ValidateFlightPlanController {

        private final FlightPlan cachedPlan;

        AtccValidateController(final FlightPlan plan) {
            super(PersistenceContext.repositories().flightPlans());
            this.cachedPlan = plan;
        }

        @Override
        public FlightPlan findForValidation(final FlightPlanId planId) {
            return cachedPlan;
        }

        @Override
        public FlightPlan markAsTested(final FlightPlan plan) {
            plan.markAsTested();
            return PersistenceContext.repositories().flightPlans().save(plan);
        }
    }
}
