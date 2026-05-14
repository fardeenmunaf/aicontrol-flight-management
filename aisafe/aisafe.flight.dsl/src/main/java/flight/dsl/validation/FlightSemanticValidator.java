package flight.dsl.validation;

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

        final Set<String> seenIds = new HashSet<>();
        for (final FlightPlanAst flight : document.flightPlans()) {
            if (!seenIds.add(flight.flightDesignator())) {
                errors.add(error(document, flight, SemanticErrorType.SEMANTIC,
                        "Duplicate flight identifier: " + flight.flightDesignator()));
            }
            validateFlight(flight, document, errors);
        }

        return errors;
    }

    private void validateFlight(final FlightPlanAst flight, final FlightDslDocument document, final List<SemanticError> errors) {
        final Set<String> visitedAirports = new HashSet<>();
        String previousArrival = null;
        LocalDateTime previousArrivalTime = null;

        for (int i = 0; i < flight.legs().size(); i++) {
            final LegAst leg = flight.legs().get(i);

            validateLeg(leg, document, errors, i + 1);

            final String departureAirport = leg.departure().airportCode();
            final String arrivalAirport = leg.arrival().airportCode();

            if (i == 0) {
                visitedAirports.add(departureAirport);
            }
            if (!visitedAirports.add(arrivalAirport)) {
                errors.add(error(document, leg.arrival(), SemanticErrorType.SEMANTIC,
                        "Airport appears more than once in the same flight: " + arrivalAirport));
            }

            if (previousArrival != null && !previousArrival.equals(departureAirport)) {
                errors.add(error(document, leg.departure(), SemanticErrorType.SEMANTIC,
                        "Arrival airport of leg " + i + " must match departure airport of leg " + (i + 1)));
            }

            final LocalDateTime departureDateTime = LocalDateTime.of(
                    LocalDate.parse(leg.departure().date()), LocalTime.parse(leg.departure().time()));
            final LocalDateTime arrivalDateTime = LocalDateTime.of(
                    LocalDate.parse(leg.arrival().date()), LocalTime.parse(leg.arrival().time()));

            if (!arrivalDateTime.isAfter(departureDateTime)) {
                errors.add(error(document, leg.arrival(), SemanticErrorType.SEMANTIC,
                        "Arrival date/time must be after departure date/time in leg " + (i + 1)));
            }

            if (previousArrivalTime != null && !departureDateTime.isAfter(previousArrivalTime)) {
                errors.add(error(document, leg.departure(), SemanticErrorType.SEMANTIC,
                        "Departure of leg " + (i + 1) + " must be after arrival of leg " + i));
            }

            previousArrival = arrivalAirport;
            previousArrivalTime = arrivalDateTime;
        }

        validateRouteCoherence(flight, document, errors);
    }

    private void validateRouteCoherence(final FlightPlanAst flight, final FlightDslDocument document, final List<SemanticError> errors) {
        final String firstDeparture = flight.legs().get(0).departure().airportCode();
        final String lastArrival = flight.legs().get(flight.legs().size() - 1).arrival().airportCode();

        if (!flight.routeOrigin().equals(firstDeparture)) {
            errors.add(error(document, flight.legs().get(0).departure(), SemanticErrorType.SEMANTIC,
                    "Route origin (" + flight.routeOrigin() + ") must match first leg departure airport (" + firstDeparture + ")"));
        }
        if (!flight.routeDestination().equals(lastArrival)) {
            errors.add(error(document, flight.legs().get(flight.legs().size() - 1).arrival(), SemanticErrorType.SEMANTIC,
                    "Route destination (" + flight.routeDestination() + ") must match last leg arrival airport (" + lastArrival + ")"));
        }
    }

    private void validateLeg(final LegAst leg, final FlightDslDocument document, final List<SemanticError> errors, final int legNumber) {
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

        for (final ClimbEntryAst climb : profile.climbEntries()) {
            if (climb.altitude() < 0 || climb.speed() <= 0) {
                errors.add(error(document, climb, SemanticErrorType.SEMANTIC,
                        "Climb profile values must be valid and positive in leg " + legNumber));
            }
        }

        for (final DescendEntryAst descend : profile.descendEntries()) {
            if (descend.altitude() < 0 || descend.speed() <= 0 || descend.rate() >= 0) {
                errors.add(error(document, descend, SemanticErrorType.SEMANTIC,
                        "Descend profile values must be valid and rate must be negative in leg " + legNumber));
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

        if (segment.from().latitude() < -90 || segment.from().latitude() > 90
                || segment.to().latitude() < -90 || segment.to().latitude() > 90) {
            errors.add(error(document, segment, SemanticErrorType.SEMANTIC,
                    "Latitude must be between -90 and 90 in leg " + legNumber));
        }

        if (segment.from().longitude() < -180 || segment.from().longitude() > 180
                || segment.to().longitude() < -180 || segment.to().longitude() > 180) {
            errors.add(error(document, segment, SemanticErrorType.SEMANTIC,
                    "Longitude must be between -180 and 180 in leg " + legNumber));
        }
    }

    private SemanticError error(final FlightDslDocument document, final Object node, final SemanticErrorType type, final String message) {
        final SourceLocation location = document.locationRegistry().locationOf(node).orElse(new SourceLocation(1, 0));
        return new SemanticError(type, location.line(), location.column(), message);
    }
}
