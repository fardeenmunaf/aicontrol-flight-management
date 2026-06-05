package flight.dsl.validation;

import flight.dsl.ast.AltitudeSlotAst;
import flight.dsl.ast.ClimbEntryAst;
import flight.dsl.ast.CoordinateAst;
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
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class FlightSemanticValidator {

    private static final Pattern FLIGHT_DESIGNATOR_PATTERN =
            Pattern.compile("[A-Z]{2}[0-9]{1,4}[A-Z]?");

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}$");

    private static final double MAX_ALTITUDE_M   =  13_700.0;  // ~45,000 ft — absolute ceiling for commercial aviation
    private static final double MAX_ALTITUDE_FT  =  45_000.0;
    private static final double MIN_ALTITUDE_M   =    -500.0;  // below Dead Sea (~-430 m), the lowest point on Earth
    private static final double MIN_ALTITUDE_FT  =  -1_640.0;
    private static final double MAX_WIND_SPEED   =     25.0;   // m/s — operational wind limit for commercial aviation

    public List<SemanticError> validate(final FlightDslDocument document) {
        final List<SemanticError> errors = new ArrayList<>();

        final Set<String> seenIds = new HashSet<>();
        for (final FlightPlanAst flight : document.flightPlans()) {
            if (!seenIds.add(flight.flightDesignator())) {
                errors.add(error(document, flight, SemanticErrorType.SEMANTIC,
                        "Duplicate flight identifier: " + flight.flightDesignator()));
            }
            if (!FLIGHT_DESIGNATOR_PATTERN.matcher(flight.flightDesignator()).matches()) {
                errors.add(error(document, flight, SemanticErrorType.SEMANTIC,
                        "Invalid flight designator format: " + flight.flightDesignator()
                                + " (expected: 2 uppercase letters + 1-4 digits + optional letter)"));
            }
            if (!EMAIL_PATTERN.matcher(flight.pilotRef()).matches()) {
                errors.add(error(document, flight, SemanticErrorType.SEMANTIC,
                        "Invalid pilot reference in flight " + flight.flightDesignator()
                                + ": '" + flight.pilotRef() + "' is not a valid email address"));
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
        validateDepartureDate(leg, document, errors, legNumber);
        validateFuel(leg, document, errors, legNumber);
        validateProfile(leg.profile(), document, errors, legNumber);

        for (final SegmentAst segment : leg.segments()) {
            validateSegment(segment, document, errors, legNumber);
        }

        validateSegmentContinuity(leg.segments(), document, errors, legNumber);
    }

    private void validateSegmentContinuity(final List<SegmentAst> segments, final FlightDslDocument document,
                                            final List<SemanticError> errors, final int legNumber) {
        for (int i = 0; i < segments.size() - 1; i++) {
            final CoordinateAst end   = segments.get(i).to();
            final CoordinateAst start = segments.get(i + 1).from();
            if (!end.latitude().equals(start.latitude())
                    || !end.longitude().equals(start.longitude())
                    || !end.altitude().equals(start.altitude())) {
                errors.add(error(document, segments.get(i + 1), SemanticErrorType.SEMANTIC,
                        "Segment " + (i + 2) + " start coordinates must match segment " + (i + 1)
                                + " end coordinates in leg " + legNumber
                                + " — aircraft cannot teleport between segments"));
            }
        }
    }

    private void validateDepartureDate(final LegAst leg, final FlightDslDocument document,
                                        final List<SemanticError> errors, final int legNumber) {
        try {
            final LocalDate date = LocalDate.parse(leg.departure().date());
            try {
                final LocalTime time = LocalTime.parse(leg.departure().time());
                final LocalDateTime departureDateTime = LocalDateTime.of(date, time);
                if (departureDateTime.isBefore(LocalDateTime.now())) {
                    errors.add(error(document, leg.departure(), SemanticErrorType.SEMANTIC,
                            "Departure date/time in leg " + legNumber + " must not be in the past: "
                                    + date + " " + time));
                }
            } catch (final DateTimeParseException ignored) {
                // time format error reported separately below
                if (date.isBefore(LocalDate.now())) {
                    errors.add(error(document, leg.departure(), SemanticErrorType.SEMANTIC,
                            "Departure date in leg " + legNumber + " must not be in the past: " + date));
                }
            }
        } catch (final DateTimeParseException e) {
            errors.add(error(document, leg.departure(), SemanticErrorType.SEMANTIC,
                    "Invalid departure date in leg " + legNumber + ": '" + leg.departure().date()
                            + "' is not a valid calendar date"));
        }

        try {
            LocalTime.parse(leg.departure().time());
        } catch (final DateTimeParseException e) {
            errors.add(error(document, leg.departure(), SemanticErrorType.SEMANTIC,
                    "Invalid departure time in leg " + legNumber + ": '" + leg.departure().time()
                            + "' — hours must be 00-23, minutes 00-59"));
        }

        try {
            LocalDate.parse(leg.arrival().date());
        } catch (final DateTimeParseException e) {
            errors.add(error(document, leg.arrival(), SemanticErrorType.SEMANTIC,
                    "Invalid arrival date in leg " + legNumber + ": '" + leg.arrival().date()
                            + "' is not a valid calendar date"));
        }

        try {
            LocalTime.parse(leg.arrival().time());
        } catch (final DateTimeParseException e) {
            errors.add(error(document, leg.arrival(), SemanticErrorType.SEMANTIC,
                    "Invalid arrival time in leg " + legNumber + ": '" + leg.arrival().time()
                            + "' — hours must be 00-23, minutes 00-59"));
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

        double prevClimbAlt = -1;
        for (final ClimbEntryAst climb : profile.climbEntries()) {
            if (climb.altitude() < 0 || climb.speed() <= 0) {
                errors.add(error(document, climb, SemanticErrorType.SEMANTIC,
                        "Climb profile values must be valid and positive in leg " + legNumber));
            }
            if (climb.altitude() < prevClimbAlt) {
                errors.add(error(document, climb, SemanticErrorType.SEMANTIC,
                        "Climb profile entries must be in ascending altitude order in leg " + legNumber
                                + ": " + climb.altitude() + " is less than previous " + prevClimbAlt));
            }
            prevClimbAlt = climb.altitude();
        }

        double prevDescendAlt = Double.MAX_VALUE;
        for (final DescendEntryAst descend : profile.descendEntries()) {
            if (descend.altitude() < 0 || descend.speed() <= 0 || descend.rate() >= 0) {
                errors.add(error(document, descend, SemanticErrorType.SEMANTIC,
                        "Descend profile values must be valid and rate must be negative in leg " + legNumber));
            }
            if (descend.altitude() > prevDescendAlt) {
                errors.add(error(document, descend, SemanticErrorType.SEMANTIC,
                        "Descend profile entries must be in descending altitude order in leg " + legNumber
                                + ": " + descend.altitude() + " is greater than previous " + prevDescendAlt));
            }
            prevDescendAlt = descend.altitude();
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

        validateAltitudeBounds(segment, document, errors, legNumber);
        validateAltitudeDirection(segment, document, errors, legNumber);

        for (final AltitudeSlotAst slot : segment.altitudeSlots()) {
            if (slot.min() < 0 || slot.max() < 0 || slot.width() <= 0) {
                errors.add(error(document, segment, SemanticErrorType.SEMANTIC,
                        "Altitude slot values must be positive and width must be > 0 in leg " + legNumber));
            }
            if (slot.min() >= slot.max()) {
                errors.add(error(document, segment, SemanticErrorType.SEMANTIC,
                        "Altitude slot min must be less than max in leg " + legNumber));
            }
        }

        if (segment.wind().direction() < 0 || segment.wind().direction() >= 360) {
            errors.add(error(document, segment, SemanticErrorType.SEMANTIC,
                    "Wind direction must be between 0 and 359 degrees in leg " + legNumber));
        }
        if (segment.wind().speed() <= 0) {
            errors.add(error(document, segment, SemanticErrorType.SEMANTIC,
                    "Wind speed must be positive in leg " + legNumber));
        }
        if (segment.wind().speed() > MAX_WIND_SPEED) {
            errors.add(error(document, segment, SemanticErrorType.SEMANTIC,
                    "Wind speed exceeds realistic maximum (" + MAX_WIND_SPEED + " m/s) in leg "
                            + legNumber + ": " + segment.wind().speed() + " m/s"));
        }
    }

    private void validateAltitudeBounds(final SegmentAst segment, final FlightDslDocument document,
                                         final List<SemanticError> errors, final int legNumber) {
        final String unit = segment.from().altitudeUnit();
        final double max = "ft".equals(unit) ? MAX_ALTITUDE_FT : MAX_ALTITUDE_M;
        final double min = "ft".equals(unit) ? MIN_ALTITUDE_FT : MIN_ALTITUDE_M;

        for (final CoordinateAst coord : List.of(segment.from(), segment.to())) {
            final double alt = coord.altitude();
            if (alt < min) {
                errors.add(error(document, segment, SemanticErrorType.SEMANTIC,
                        "Altitude below physical minimum (" + min + " " + unit + ") in leg "
                                + legNumber + ": " + alt + " " + unit));
            } else if (alt > max) {
                errors.add(error(document, segment, SemanticErrorType.SEMANTIC,
                        "Altitude exceeds maximum allowed (" + max + " " + unit + ") in leg "
                                + legNumber + ": " + alt + " " + unit));
            }
        }
    }

    private void validateAltitudeDirection(final SegmentAst segment, final FlightDslDocument document,
                                            final List<SemanticError> errors, final int legNumber) {
        final double fromAlt = segment.from().altitude();
        final double toAlt   = segment.to().altitude();

        switch (segment.mode()) {
            case CLIMB:
                if (toAlt <= fromAlt) {
                    errors.add(error(document, segment, SemanticErrorType.SEMANTIC,
                            "Climb segment in leg " + legNumber
                                    + " must have increasing altitude (from " + fromAlt + " to " + toAlt + ")"));
                }
                break;
            case DESCEND:
                if (toAlt >= fromAlt) {
                    errors.add(error(document, segment, SemanticErrorType.SEMANTIC,
                            "Descend segment in leg " + legNumber
                                    + " must have decreasing altitude (from " + fromAlt + " to " + toAlt + ")"));
                }
                break;
            default:
                break;
        }
    }

    private SemanticError error(final FlightDslDocument document, final Object node, final SemanticErrorType type, final String message) {
        final SourceLocation location = document.locationRegistry().locationOf(node).orElse(new SourceLocation(1, 0));
        return new SemanticError(type, location.line(), location.column(), message);
    }
}
