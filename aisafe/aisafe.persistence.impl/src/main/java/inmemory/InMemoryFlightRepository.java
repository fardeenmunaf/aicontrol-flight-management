package inmemory;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import eapli.framework.infrastructure.repositories.impl.inmemory.InMemoryDomainRepository;
import flight.domain.Flight;
import flight.domain.FlightStatus;
import flight.repository.FlightRepository;

/**
 * In-memory implementation of FlightRepository.
 * Used for testing and development environments.
 *
 * @author LAPR4 Team
 */
public class InMemoryFlightRepository extends InMemoryDomainRepository<Flight, Long> implements FlightRepository {

    static {
        InMemoryInitializer.init();
    }

    @Override
    public List<Flight> findActiveFlightsLandingInAirport(final String iataCode) {
        final Instant now = Instant.now();
        final Iterable<Flight> flights = match(f -> f.scheduledDepartureTime().isBefore(now)
                && f.scheduledArrivalTime().isAfter(now));
        return StreamSupport.stream(flights.spliterator(), false).collect(Collectors.toList());
    }

    @Override
    public List<Flight> findByStatus(final FlightStatus status) {
        final Instant now = Instant.now();
        switch (status) {
            case SCHEDULED:
                return StreamSupport.stream(match(f -> f.scheduledDepartureTime().isAfter(now)).spliterator(), false).collect(Collectors.toList());
            case AIRBORNE:
                return StreamSupport.stream(match(f -> f.scheduledDepartureTime().isBefore(now) && f.scheduledArrivalTime().isAfter(now)).spliterator(), false).collect(Collectors.toList());
            case COMPLETED:
                return StreamSupport.stream(match(f -> f.scheduledArrivalTime().isBefore(now)).spliterator(), false).collect(Collectors.toList());
            default:
                return List.of();
        }
    }

    @Override
    public Optional<Flight> findByFlightDesignator(final String flightDesignator) {
        return matchOne(f -> f.flightDesignator().equals(flightDesignator));
    }
}