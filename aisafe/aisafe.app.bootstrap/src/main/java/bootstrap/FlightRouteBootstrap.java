package bootstrap;

import airport.domain.Airport;
import airport.repositories.AirportRepository;
import airtransportcompany.domain.AirTransportCompany;
import airtransportcompany.repositories.AirTransportCompanyRepository;
import flightRoute.domain.FlightRoute;
import flightRoute.domain.RouteDesignation;
import flightRoute.repository.FlightRouteRepository;
import shared.domain.FlightRouteId;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class FlightRouteBootstrap {

    private final FlightRouteRepository flightRouteRepository;
    private final AirportRepository airportRepository;
    private final AirTransportCompanyRepository companyRepository;

    public FlightRouteBootstrap(final FlightRouteRepository flightRouteRepository,
                                 final AirportRepository airportRepository,
                                 final AirTransportCompanyRepository companyRepository) {
        this.flightRouteRepository = flightRouteRepository;
        this.airportRepository = airportRepository;
        this.companyRepository = companyRepository;
    }

    public void execute() {
        // TAP Air Portugal (TP)
        registerIfAbsent("TP01", "TP", "LIS", "OPO");
        registerIfAbsent("TP02", "TP", "LIS", "MAD");
        registerIfAbsent("TP03", "TP", "LIS", "CDG");
        registerIfAbsent("TP04", "TP", "LIS", "LHR");
        registerIfAbsent("TP05", "TP", "OPO", "FRA");

        // RyanAir (FR)
        registerIfAbsent("FR01", "FR", "OPO", "MAD");
        registerIfAbsent("FR02", "FR", "LIS", "LHR");
        registerIfAbsent("FR03", "FR", "OPO", "PMI");

        // Lufthansa (LH)
        registerIfAbsent("LH01", "LH", "FRA", "LIS");
        registerIfAbsent("LH02", "LH", "FRA", "OPO");

        // British Airways (BA)
        registerIfAbsent("BA01", "BA", "LHR", "LIS");
        registerIfAbsent("BA02", "BA", "LHR", "OPO");

        // Air France (AF)
        registerIfAbsent("AF01", "AF", "CDG", "LIS");
        registerIfAbsent("AF02", "AF", "CDG", "OPO");
    }

    private void registerIfAbsent(final String designation, final String companyIata,
                                   final String originIata, final String destIata) {
        final RouteDesignation rd;
        try {
            rd = new RouteDesignation(designation);
        } catch (final IllegalArgumentException e) {
            System.out.println("Invalid route designation '" + designation + "': " + e.getMessage());
            return;
        }

        if (flightRouteRepository.findByDesignation(rd).isPresent()) {
            return;
        }

        final List<AirTransportCompany> companies = companyRepository.findByIataCode(companyIata);
        if (companies.isEmpty()) {
            System.out.println("Company with IATA '" + companyIata + "' not found. Skipping route " + designation);
            return;
        }

        final Optional<Airport> origin = airportRepository.findByIataCode(originIata);
        if (origin.isEmpty()) {
            System.out.println("Airport '" + originIata + "' not found. Skipping route " + designation);
            return;
        }

        final Optional<Airport> destination = airportRepository.findByIataCode(destIata);
        if (destination.isEmpty()) {
            System.out.println("Airport '" + destIata + "' not found. Skipping route " + designation);
            return;
        }

        final FlightRoute route = new FlightRoute(
                FlightRouteId.newId(), rd, LocalDate.now(),
                origin.get(), destination.get(), companies.get(0));
        flightRouteRepository.save(route);
        System.out.println("Flight route bootstrapped: " + designation
                + " [" + originIata + " -> " + destIata + "] (" + companyIata + ")");
    }
}
