package flight.dsl.validation;

import flight.dsl.ast.ArrivalAst;
import flight.dsl.ast.ClimbEntryAst;
import flight.dsl.ast.DescendEntryAst;
import flight.dsl.ast.FlightPlanAst;
import flight.dsl.ast.LegAst;
import flight.dsl.ast.ProfileAst;
import flight.dsl.ast.SegmentAst;
import flight.dsl.model.FlightDslDocument;
import flight.dsl.model.SourceLocation;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FlightSemanticValidator {

    public List<SemanticError> validate(final FlightDslDocument document) {
        final List<SemanticError> errors = new ArrayList<>();

        if (document.flightPlans().isEmpty()) {
            errors.add(new SemanticError(SemanticErrorType.STRUCTURAL, 1, 0, "At least one flight block is required"));
            return errors;
        }

        if (document.flightPlans().size() != 1) {
            errors.add(new SemanticError(
                    SemanticErrorType.STRUCTURAL,
                    1,
                    0,
                    "Exactly one flight block is allowed in a DSL file"
            ));
        }

        for (final FlightPlanAst flight : document.flightPlans()) {
            validateFlight(flight, document, errors);
        }

        return errors;
    }

    private void validateFlight(final FlightPlanAst flight, final FlightDslDocument document, final List<SemanticError> errors) {
        if (flight.legs().isEmpty()) {
            errors.add(error(document, flight, SemanticErrorType.STRUCTURAL, "A flight must contain at least one leg"));
            return;
        }

        final Set<String> visitedAirports = new HashSet<>();
        String previousArrival = null;
        LocalDateTime previousArrivalTime = null;

        for (int i = 0; i < flight.legs().size(); i++) {
            final LegAst leg = flight.legs().get(i);

            validateLeg(leg, document, errors, i + 1);

            final String departureAirport = leg.departure().airportCode();
            final String arrivalAirport = leg.arrival().airportCode();

            if (i == 0 && !visitedAirports.add(departureAirport)) {
                errors.add(error(document, leg.departure(), SemanticErrorType.SEMANTIC, "Departure airport is repeated in flight"));
            }
            if (!visitedAirports.add(arrivalAirport)) {
                if (previousArrival == null || !previousArrival.equals(arrivalAirport)) {
                    errors.add(error(document, leg.arrival(), SemanticErrorType.SEMANTIC, "Airport appears more than once in the same flight: " + arrivalAirport));
                }
            }

            if (previousArrival != null && !previousArrival.equals(departureAirport)) {
                errors.add(error(document, leg.departure(), SemanticErrorType.SEMANTIC,
                        "Arrival airport of leg " + (i) + " must match departure airport of leg " + (i + 1)));
            }

            final LocalDateTime departureDateTime = parseDateTime(leg.departure().date(), leg.departure().time(), document, leg.departure(), errors);
            final LocalDateTime arrivalDateTime = parseDateTime(leg.arrival().date(), leg.arrival().time(), document, leg.arrival(), errors);

            if (departureDateTime != null && arrivalDateTime != null && !arrivalDateTime.isAfter(departureDateTime)) {
                errors.add(error(document, leg.arrival(), SemanticErrorType.SEMANTIC,
                        "Arrival date/time must be after departure date/time"));
            }

            if (previousArrivalTime != null && departureDateTime != null && !departureDateTime.isAfter(previousArrivalTime)) {
                errors.add(error(document, leg.departure(), SemanticErrorType.SEMANTIC,
                        "Departure of leg " + (i + 1) + " must be after arrival of leg " + i));
            }

            previousArrival = arrivalAirport;
            previousArrivalTime = arrivalDateTime;
        }

        final String firstDeparture = flight.legs().get(0).departure().airportCode();
        final String lastArrival = flight.legs().get(flight.legs().size() - 1).arrival().airportCode();
        if (firstDeparture == null || lastArrival == null || !firstDeparture.equals(lastArrival) && flight.legs().size() == 1) {
            // single-leg routes naturally depend on same origin/destination only if expected by the file; keep this check minimal
        }

        validateFlightType(flight, document, errors);
    }

    private void validateFlightType(final FlightPlanAst flight, final FlightDslDocument document, final List<SemanticError> errors) {
        if (flight.type() == null) {
            errors.add(error(document, flight, SemanticErrorType.SEMANTIC, "Flight type is required"));
        }
    }

    private void validateLeg(final LegAst leg, final FlightDslDocument document, final List<SemanticError> errors, final int legNumber) {
        if (leg.segments().isEmpty()) {
            errors.add(error(document, leg, SemanticErrorType.STRUCTURAL, "Leg " + legNumber + " must contain at least one segment"));
        }

        validateFuel(leg, document, errors, legNumber);
        validateProfile(leg.profile(), document, errors, legNumber);

        for (final SegmentAst segment : leg.segments()) {
            validateSegment(segment, document, errors, legNumber);
        }
    }

    private void validateFuel(final LegAst leg, final FlightDslDocument document, final List<SemanticError> errors, final int legNumber) {
        if (leg.fuel().quantity() <= 0) {
            errors.add(error(document, leg.fuel(), SemanticErrorType.SEMANTIC,
                    "Fuel quantity in leg " + legNumber + " must be strictly positive"));
        }
    }

    private void validateProfile(final ProfileAst profile, final FlightDslDocument document, final List<SemanticError> errors, final int legNumber) {
        if (profile == null) {
            return;
        }

        if (profile.climbEntries().isEmpty() && profile.descendEntries().isEmpty()) {
            errors.add(new SemanticError(SemanticErrorType.STRUCTURAL, 1, 0, "Profile in leg " + legNumber + " cannot be empty"));
        }

        for (final ClimbEntryAst climb : profile.climbEntries()) {
            if (climb.altitude() < 0 || climb.speed() <= 0) {
                errors.add(error(document, climb, SemanticErrorType.SEMANTIC,
                        "Climb profile values must be valid and positive"));
            }
        }

        for (final DescendEntryAst descend : profile.descendEntries()) {
            if (descend.altitude() < 0 || descend.speed() <= 0 || descend.rate() >= 0) {
                errors.add(error(document, descend, SemanticErrorType.SEMANTIC,
                        "Descend profile values must be valid and rate must be negative"));
            }
        }
    }

    private void validateSegment(final SegmentAst segment, final FlightDslDocument document, final List<SemanticError> errors, final int legNumber) {
        if (segment.from().latitude().equals(segment.to().latitude())
                && segment.from().longitude().equals(segment.to().longitude())
                && segment.from().altitude().equals(segment.to().altitude())) {
            errors.add(error(document, segment, SemanticErrorType.SEMANTIC,
                    "Segment start and end coordinates must differ in leg " + legNumber));
        }

        if (segment.from().latitude() < -90 || segment.from().latitude() > 90 || segment.to().latitude() < -90 || segment.to().latitude() > 90) {
            errors.add(error(document, segment, SemanticErrorType.SEMANTIC, "Latitude must be between -90 and 90"));
        }
        if (segment.from().longitude() < -180 || segment.from().longitude() > 180 || segment.to().longitude() < -180 || segment.to().longitude() > 180) {
            errors.add(error(document, segment, SemanticErrorType.SEMANTIC, "Longitude must be between -180 and 180"));
        }
    }

    private LocalDateTime parseDateTime(final String date,
                                        final String time,
                                        final FlightDslDocument document,
                                        final Object node,
                                        final List<SemanticError> errors) {
        try {
            return LocalDateTime.of(LocalDate.parse(date), LocalTime.parse(time));
        } catch (final Exception ex) {
            errors.add(error(document, node, SemanticErrorType.SEMANTIC, "Invalid date/time value: " + date + " " + time));
            return null;
        }
    }

    private SemanticError error(final FlightDslDocument document, final Object node, final SemanticErrorType type, final String message) {
        final SourceLocation location = document.locationRegistry().locationOf(node).orElse(new SourceLocation(1, 0));
        return new SemanticError(type, location.line(), location.column(), message);
    }
}

