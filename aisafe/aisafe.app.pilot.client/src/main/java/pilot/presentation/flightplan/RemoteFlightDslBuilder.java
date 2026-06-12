package pilot.presentation.flightplan;

import eapli.framework.io.util.Console;

import java.util.ArrayList;
import java.util.List;

/**
 * Mirrors ConsoleFlightDslBuilder but works with string arrays received over TCP.
 *
 * Fleet row:  [0]=registration [1]=model [2]=maker [3]=status
 * Route row:  [0]=designation [1]=fromIata [2]=toIata [3]=activeFrom
 *             [4]=fromLat [5]=fromLon [6]=fromElev
 *             [7]=toLat   [8]=toLon   [9]=toElev  [10]=companyIcao
 */
class RemoteFlightDslBuilder {

    private final String pilotEmail;
    private final boolean promptEverything;
    private final List<String[]> fleet;
    private final List<String[]> routes;

    RemoteFlightDslBuilder(final String pilotEmail, final boolean promptEverything,
                           final List<String[]> fleet, final List<String[]> routes) {
        this.pilotEmail = pilotEmail;
        this.promptEverything = promptEverything;
        this.fleet = fleet;
        this.routes = routes;
    }

    String build() {
        final String type = reqType();

        System.out.println("\n  Available aircraft:");
        for (int i = 0; i < fleet.size(); i++) {
            final String[] a = fleet.get(i);
            System.out.printf("  %d. %-20s (%s)%n", i + 1,
                    a.length > 1 ? a[1] : "?", a.length > 0 ? a[0] : "?");
        }
        final int aircraftIdx = pickIndex("Select aircraft", fleet.size()) - 1;
        final String aircraftReg = fleet.get(aircraftIdx)[0];

        final int numLegs = (int) numWithDef("Number of legs", 1);
        if (numLegs < 1) throw new IllegalArgumentException("Flight must have at least one leg.");

        final List<LegInfo> legs = new ArrayList<>();
        for (int i = 1; i <= numLegs; i++) {
            System.out.println("  --- Leg " + i + " ---");
            System.out.println("\n  Available routes:");
            for (int j = 0; j < routes.size(); j++) {
                final String[] r = routes.get(j);
                System.out.printf("  %d. %-20s %s -> %s%n", j + 1,
                        r[0], r.length > 1 ? r[1] : "?", r.length > 2 ? r[2] : "?");
            }
            final int routeIdx = pickIndex("Select route for leg " + i, routes.size()) - 1;
            final String[] r = routes.get(routeIdx);

            final LegInfo leg = new LegInfo();
            leg.designation = r[0];
            leg.fromIata    = r[1];
            leg.toIata      = r[2];
            leg.fromLat     = parseDouble(r, 4, 38.7);
            leg.fromLon     = parseDouble(r, 5, -9.1);
            leg.fromElev    = parseDouble(r, 6, 0);
            leg.toLat       = parseDouble(r, 7, 41.2);
            leg.toLon       = parseDouble(r, 8, -8.6);
            leg.toElev      = parseDouble(r, 9, 0);
            leg.companyIcao = r.length > 10 ? r[10] : "AIS";

            leg.depDate  = reqWithDef("Leg departure date (yyyy-MM-dd)", "2026-06-12");
            leg.depTime  = reqWithDef("Leg departure time (HH:mm)", "09:00");
            leg.arrDate  = reqWithDef("Leg arrival date (yyyy-MM-dd)", "2026-06-12");
            leg.arrTime  = reqWithDef("Leg arrival time (HH:mm)", "11:00");
            leg.fuel     = reqWithDef("Fuel quantity (kg)", "42000");
            leg.cruiseAlt = numWithDef("Cruise altitude (m)", 10000);
            leg.b = pointLatLon("Climb end / cruise start point", leg.cruiseAlt, 40.0, -8.0);
            leg.c = pointLatLon("Cruise end / descent start point", leg.cruiseAlt, 41.0, -7.0);
            legs.add(leg);
        }

        final LegInfo first = legs.get(0);
        final LegInfo last  = legs.get(legs.size() - 1);

        String numbers = first.designation.replaceAll("[^0-9]", "");
        if (numbers.length() > 3) numbers = numbers.substring(0, 3);
        else if (numbers.isEmpty()) numbers = "000";

        String suffix = reqWithDef("Flight suffix letter (A-Z)", "A").toUpperCase();
        if (suffix.length() > 1) suffix = suffix.substring(0, 1);
        if (!suffix.matches("[A-Z]")) suffix = "A";

        final String designator = first.companyIcao + numbers + suffix;

        final StringBuilder sb = new StringBuilder();
        sb.append("flight ").append(designator).append(" {\n");
        sb.append("  type: ").append(type).append(";\n");
        sb.append("  aircraft: ").append(aircraftReg).append(";\n");
        sb.append("  pilot: ").append(pilotEmail).append(";\n");
        sb.append("  route: ").append(first.designation)
                .append(" from ").append(first.fromIata)
                .append(" to ").append(last.toIata).append(";\n");

        for (final LegInfo leg : legs) {
            final double[] a = {leg.fromLat, leg.fromLon, leg.fromElev};
            final double[] d = {leg.toLat,   leg.toLon,   leg.toElev};
            sb.append("  leg {\n");
            sb.append("    departure { airport: ").append(leg.fromIata)
                    .append("; date: ").append(leg.depDate)
                    .append("; time: ").append(leg.depTime).append("; }\n");
            sb.append("    arrival { airport: ").append(leg.toIata)
                    .append("; date: ").append(leg.arrDate)
                    .append("; time: ").append(leg.arrTime).append("; }\n");
            sb.append("    fuel { quantity: ").append(leg.fuel).append(" kg; }\n");
            sb.append(profileBlock(leg.fromElev, leg.cruiseAlt, leg.toElev));
            sb.append(segmentBlock("climb",   a, leg.b));
            sb.append(segmentBlock("cruise",  leg.b, leg.c));
            sb.append(segmentBlock("descend", leg.c, d));
            sb.append("  }\n");
        }
        sb.append("}\n");
        return sb.toString();
    }

    private String profileBlock(final double depAlt, final double cruiseAlt, final double arrAlt) {
        final StringBuilder sb = new StringBuilder("    profile {\n");
        if (promptEverything) {
            System.out.println("  Enter profile lines.");
            final int climbs = (int) numWithDef("Number of climb entries", 2);
            for (int i = 0; i < climbs; i++) {
                final String altDef = i == 0 ? fmt(depAlt) : fmt(cruiseAlt);
                final String spdDef = i == 0 ? "210" : "300";
                sb.append("      climb [ altitude: ")
                        .append(reqWithDef("  climb " + (i + 1) + " altitude (m)", altDef))
                        .append(" m, speed: ")
                        .append(reqWithDef("  climb " + (i + 1) + " speed (kts)", spdDef))
                        .append(" kts ];\n");
            }
            sb.append("      cruise [ speed: ").append(reqWithDef("  cruise speed (kts)", "460")).append(" kts ];\n");
            final int descs = (int) numWithDef("Number of descend entries", 2);
            for (int i = 0; i < descs; i++) {
                final String altDef  = i == 0 ? fmt(cruiseAlt) : fmt(arrAlt);
                final String spdDef  = i == 0 ? "300" : "150";
                final String rateDef = i == 0 ? "-10" : "-5";
                sb.append("      descend [ altitude: ")
                        .append(reqWithDef("  descend " + (i + 1) + " altitude (m)", altDef))
                        .append(" m, speed: ")
                        .append(reqWithDef("  descend " + (i + 1) + " speed (kts)", spdDef))
                        .append(" kts, rate: ")
                        .append(reqWithDef("  descend " + (i + 1) + " rate (m/s, negative)", rateDef))
                        .append(" m/s ];\n");
            }
        } else {
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
            final double maxAlt = Math.max(from[2], to[2]);
            final String maxAltDef = fmt(maxAlt <= 0 ? 1 : maxAlt);
            System.out.println("  " + mode + " segment altitude slot + wind:");
            slot = "altslot [ min: " + reqWithDef("  slot min (m)", "0")
                    + " m, max: " + reqWithDef("  slot max (m)", maxAltDef)
                    + " m, width: " + reqWithDef("  slot width (m)", "50") + " m ];";
            wind = "wind { direction: " + reqWithDef("  wind direction (deg 0-359)", "270")
                    + "; speed: " + reqWithDef("  wind speed (m/s)", "10") + " m/s; }";
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

    private double[] pointLatLon(final String label, final double altitude, final double defLat, final double defLon) {
        System.out.println("  " + label + " (altitude fixed at cruise = " + fmt(altitude) + " m):");
        return new double[]{numWithDef("  latitude", defLat), numWithDef("  longitude", defLon), altitude};
    }

    private static int pickIndex(final String label, final int max) {
        while (true) {
            try {
                final String raw = Console.readLine("  " + label + " (1-" + max + ")");
                final int v = Integer.parseInt(raw == null ? "" : raw.trim());
                if (v >= 1 && v <= max) return v;
            } catch (final NumberFormatException ignored) {}
            System.out.println("  Enter a number between 1 and " + max + ".");
        }
    }

    private static String reqType() {
        while (true) {
            final String raw = Console.readLine("  Type (regular/charter) [regular]");
            if (raw == null || raw.trim().isEmpty()) return "regular";
            final String val = raw.trim().toLowerCase();
            if ("regular".equals(val) || "charter".equals(val)) return val;
            System.out.println("  Invalid type.");
        }
    }

    private static String reqWithDef(final String label, final String defValue) {
        while (true) {
            final String raw = Console.readLine("  " + label + " [" + defValue + "]");
            if (raw == null || raw.trim().isEmpty()) return defValue;
            return raw.trim();
        }
    }

    private static double numWithDef(final String label, final double defValue) {
        while (true) {
            try {
                final String raw = Console.readLine("  " + label + " [" + fmt(defValue) + "]");
                if (raw == null || raw.trim().isEmpty()) return defValue;
                return Double.parseDouble(raw.trim());
            } catch (final NumberFormatException e) {
                System.out.println("  Enter a number.");
            }
        }
    }

    private static double parseDouble(final String[] arr, final int idx, final double fallback) {
        try {
            return idx < arr.length ? Double.parseDouble(arr[idx]) : fallback;
        } catch (final NumberFormatException e) {
            return fallback;
        }
    }

    private static String fmt(final double v) {
        return v == Math.rint(v) ? Long.toString((long) v) : Double.toString(v);
    }

    private static class LegInfo {
        String designation, fromIata, toIata, depDate, depTime, arrDate, arrTime, fuel, companyIcao;
        double fromLat, fromLon, fromElev, toLat, toLon, toElev, cruiseAlt;
        double[] b, c;
    }
}
