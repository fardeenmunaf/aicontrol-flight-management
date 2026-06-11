package airtransport.presentation.flightplan;

import eapli.framework.io.util.Console;

/**
 * Gathers console input and assembles Flight DSL text for a single-leg, three-segment plan.
 * The resulting text is handed to FlightPlanCreationService, which performs all DSL validation.
 */
class ConsoleFlightDslBuilder {

    private final String pilotEmail;
    private final boolean promptEverything;
    private final java.util.List<aircraft.domain.Aircraft> eligibleAircraft;
    private final java.util.List<flightRoute.domain.FlightRoute> companyRoutes;
    private final String companyIcaoCode;

    ConsoleFlightDslBuilder(final String pilotEmail, final boolean promptEverything, 
                            final java.util.List<aircraft.domain.Aircraft> eligibleAircraft, 
                            final java.util.List<flightRoute.domain.FlightRoute> companyRoutes,
                            final String companyIcaoCode) {
        this.pilotEmail = pilotEmail;
        this.promptEverything = promptEverything;
        this.eligibleAircraft = eligibleAircraft;
        this.companyRoutes = companyRoutes;
        this.companyIcaoCode = companyIcaoCode;
    }

    String build() {
        final String type = reqType();
        
        final eapli.framework.presentation.console.SelectWidget<aircraft.domain.Aircraft> selector = 
                new eapli.framework.presentation.console.SelectWidget<>(
                        "  Select Aircraft", eligibleAircraft,
                        new eapli.framework.visitor.Visitor<aircraft.domain.Aircraft>() {
                            @Override
                            public void visit(aircraft.domain.Aircraft a) {
                                System.out.printf("%-10s (%s)", a.aircraftModel().modelName(), a.registrationNumber());
                            }
                        });
        selector.show();
        final aircraft.domain.Aircraft selectedAircraft = selector.selectedElement();
        if (selectedAircraft == null) {
            throw new IllegalArgumentException("Aircraft selection is required.");
        }
        final String aircraft = selectedAircraft.registrationNumber();

        final int numLegs = (int) numWithDef("Number of legs", 1);
        if (numLegs < 1) {
            throw new IllegalArgumentException("Flight must have at least one leg.");
        }

        java.util.List<LegInfo> legs = new java.util.ArrayList<>();
        for (int i = 1; i <= numLegs; i++) {
            System.out.println("  --- Leg " + i + " ---");
            final eapli.framework.presentation.console.SelectWidget<flightRoute.domain.FlightRoute> routeSelector = 
                    new eapli.framework.presentation.console.SelectWidget<>(
                            "  Select Route for Leg " + i, companyRoutes,
                            r -> System.out.printf("%s: %s -> %s", r.designation().toString(), r.originAirport().iataCode(), r.destinationAirport().iataCode()));
            routeSelector.show();
            final flightRoute.domain.FlightRoute selectedRoute = routeSelector.selectedElement();
            if (selectedRoute == null) {
                throw new IllegalArgumentException("Route selection is required.");
            }

            LegInfo leg = new LegInfo();
            leg.route = selectedRoute;
            leg.depDate = reqWithDef("Leg departure date (yyyy-MM-dd)", "2026-06-12");
            leg.depTime = reqWithDef("Leg departure time (HH:mm)", "09:00");
            leg.arrDate = reqWithDef("Leg arrival date (yyyy-MM-dd)", "2026-06-12");
            leg.arrTime = reqWithDef("Leg arrival time (HH:mm)", "11:00");
            leg.fuel = reqWithDef("Fuel quantity (kg)", "4200");
            
            leg.a = new double[] {
                selectedRoute.originAirport().coordinates().latitude(),
                selectedRoute.originAirport().coordinates().longitude(),
                selectedRoute.originAirport().elevation()
            };
            
            leg.cruiseAlt = numWithDef("Cruise altitude (m)", 10000);
            leg.b = pointLatLon("Climb end / cruise start point", leg.cruiseAlt, 40.0, -8.0);
            leg.c = pointLatLon("Cruise end / descent start point", leg.cruiseAlt, 41.0, -7.0);
            
            leg.d = new double[] {
                selectedRoute.destinationAirport().coordinates().latitude(),
                selectedRoute.destinationAirport().coordinates().longitude(),
                selectedRoute.destinationAirport().elevation()
            };
            
            legs.add(leg);
        }

        final flightRoute.domain.FlightRoute firstRoute = legs.get(0).route;
        final flightRoute.domain.FlightRoute lastRoute = legs.get(legs.size() - 1).route;

        final String routeRef = firstRoute.designation().toString();
        
        String numbers = routeRef.replaceAll("[^0-9]", "");
        if (numbers.length() > 3) {
            numbers = numbers.substring(0, 3);
        } else if (numbers.isEmpty()) {
            numbers = "000";
        }
        
        String suffix = reqWithDef("Flight suffix letter (A-Z)", "A").toUpperCase();
        if (suffix.length() > 1) suffix = suffix.substring(0, 1);
        if (!suffix.matches("[A-Z]")) suffix = "A";

        final String designator = companyIcaoCode + numbers + suffix;
        
        final String origin = firstRoute.originAirport().iataCode();
        final String destination = lastRoute.destinationAirport().iataCode();

        final StringBuilder sb = new StringBuilder();
        sb.append("flight ").append(designator).append(" {\n");
        sb.append("  type: ").append(type).append(";\n");
        sb.append("  aircraft: ").append(aircraft).append(";\n");
        sb.append("  pilot: ").append(pilotEmail).append(";\n");
        sb.append("  route: ").append(routeRef).append(" from ").append(origin)
                .append(" to ").append(destination).append(";\n");

        for (LegInfo leg : legs) {
            sb.append("  leg {\n");
            sb.append("    departure { airport: ").append(leg.route.originAirport().iataCode()).append("; date: ").append(leg.depDate)
                    .append("; time: ").append(leg.depTime).append("; }\n");
            sb.append("    arrival { airport: ").append(leg.route.destinationAirport().iataCode()).append("; date: ").append(leg.arrDate)
                    .append("; time: ").append(leg.arrTime).append("; }\n");
            sb.append("    fuel { quantity: ").append(leg.fuel).append(" kg; }\n");
            sb.append(profileBlock(leg.a[2], leg.cruiseAlt, leg.d[2]));
            sb.append(segmentBlock("climb", leg.a, leg.b));
            sb.append(segmentBlock("cruise", leg.b, leg.c));
            sb.append(segmentBlock("descend", leg.c, leg.d));
            sb.append("  }\n");
        }
        sb.append("}\n");
        return sb.toString();
    }

    private static class LegInfo {
        flightRoute.domain.FlightRoute route;
        String depDate;
        String depTime;
        String arrDate;
        String arrTime;
        String fuel;
        double[] a;
        double cruiseAlt;
        double[] b;
        double[] c;
        double[] d;
    }

    private String profileBlock(final double depAlt, final double cruiseAlt, final double arrAlt) {
        final StringBuilder sb = new StringBuilder("    profile {\n");
        if (promptEverything) {
            System.out.println("  Enter profile lines. Climb altitudes must ascend, descend must descend.");
            final int climbs = (int) num("Number of climb entries");
            for (int i = 0; i < climbs; i++) {
                sb.append("      climb [ altitude: ").append(req("  climb " + (i + 1) + " altitude (m)"))
                        .append(" m, speed: ").append(req("  climb " + (i + 1) + " speed (kts)")).append(" kts ];\n");
            }
            sb.append("      cruise [ speed: ").append(req("  cruise speed (kts)")).append(" kts ];\n");
            final int descs = (int) num("Number of descend entries");
            for (int i = 0; i < descs; i++) {
                sb.append("      descend [ altitude: ").append(req("  descend " + (i + 1) + " altitude (m)"))
                        .append(" m, speed: ").append(req("  descend " + (i + 1) + " speed (kts)"))
                        .append(" kts, rate: ").append(req("  descend " + (i + 1) + " rate (m/s, negative)")).append(" m/s ];\n");
            }
        } else {
            // Template: a minimal valid ascending climb / cruise / descending descend.
            sb.append("      climb [ altitude: ").append(fmt(depAlt)).append(" m, speed: 210 kts ];\n");
            sb.append("      climb [ altitude: ").append(fmt(cruiseAlt)).append(" m, speed: 300 kts ];\n");
            sb.append("      cruise [ speed: 460 kts ];\n");
            sb.append("      descend [ altitude: ").append(fmt(cruiseAlt)).append(" m, speed: 300 kts, rate: -10 m/s ];\n");
            sb.append("      descend [ altitude: ").append(fmt(arrAlt)).append(" m, speed: 150 kts, rate: -5 m/s ];\n");
        }
        return sb.append("    }\n").toString();
    }

    private String segmentBlock(final String mode, final double[] from, final double[] to) {
        final String slot;
        final String wind;
        if (promptEverything) {
            System.out.println("  " + mode + " segment altitude slot + wind:");
            slot = "altslot [ min: " + req("  slot min (m)") + " m, max: " + req("  slot max (m)")
                    + " m, width: " + req("  slot width (m)") + " m ];";
            wind = "wind { direction: " + req("  wind direction (deg 0-359)")
                    + "; speed: " + req("  wind speed (m/s)") + " m/s; }";
        } else {
            final double maxAlt = Math.max(from[2], to[2]);
            slot = "altslot [ min: 0 m, max: " + fmt(maxAlt <= 0 ? 1 : maxAlt) + " m, width: 50 m ];";
            wind = "wind { direction: 270; speed: 10 m/s; }";
        }
        return "    segment {\n"
                + "      from: (" + fmt(from[0]) + ", " + fmt(from[1]) + ", altitude: " + fmt(from[2]) + " m);\n"
                + "      to: (" + fmt(to[0]) + ", " + fmt(to[1]) + ", altitude: " + fmt(to[2]) + " m);\n"
                + "      " + slot + "\n"
                + "      " + wind + "\n"
                + "      mode: " + mode + ";\n"
                + "    }\n";
    }

    private double[] point(final String label) {
        System.out.println("  " + label + ":");
        return new double[] { num("  latitude"), num("  longitude"), num("  altitude (m)") };
    }

    private double[] pointLatLon(final String label, final double altitude, final double defLat, final double defLon) {
        System.out.println("  " + label + " (altitude fixed at cruise = " + fmt(altitude) + " m):");
        return new double[] { numWithDef("  latitude", defLat), numWithDef("  longitude", defLon), altitude };
    }

    private static String req(final String label) {
        while (true) {
            final String raw = Console.readLine("  " + label);
            if (raw != null && !raw.trim().isEmpty()) {
                return raw.trim();
            }
            System.out.println("  Required.");
        }
    }

    private static String reqWithDef(final String label, final String defValue) {
        while (true) {
            final String raw = Console.readLine("  " + label + " [" + defValue + "]");
            if (raw == null || raw.trim().isEmpty()) {
                return defValue;
            }
            return raw.trim();
        }
    }

    private static double num(final String label) {
        while (true) {
            try {
                return Double.parseDouble(req(label));
            } catch (final NumberFormatException e) {
                System.out.println("  Enter a number.");
            }
        }
    }

    private static double numWithDef(final String label, final double defValue) {
        while (true) {
            try {
                final String raw = Console.readLine("  " + label + " [" + fmt(defValue) + "]");
                if (raw == null || raw.trim().isEmpty()) {
                    return defValue;
                }
                return Double.parseDouble(raw.trim());
            } catch (final NumberFormatException e) {
                System.out.println("  Enter a number.");
            }
        }
    }

    private static String reqType() {
        while (true) {
            final String raw = Console.readLine("  Type (regular/charter) [regular]");
            if (raw == null || raw.trim().isEmpty()) {
                return "regular";
            }
            final String val = raw.trim().toLowerCase();
            if ("regular".equals(val) || "charter".equals(val)) {
                return val;
            }
            System.out.println("  Invalid type. Please enter 'regular' or 'charter'.");
        }
    }

    private static String def(final String value, final String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }

    private static String fmt(final double v) {
        if (v == Math.rint(v)) {
            return Long.toString((long) v);
        }
        return Double.toString(v);
    }
}
