package jpa;

import eapli.framework.domain.repositories.TransactionalContext;
import flight.domain.Flight;
import flight.repository.FlightRepository;
import flight.domain.FlightStatus;
import shared.domain.FlightId;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class JpaFlightRepository extends BaseJpaRepositoryBase<Flight, FlightId, FlightId> implements FlightRepository {

    public JpaFlightRepository(final TransactionalContext autoTx) {
        super(autoTx, "flightId");
    }

    public JpaFlightRepository(final String persistenceUnitName) {
        super("flightId");
    }

    @Override
    public List<Flight> findActiveFlightsLandingInAirport(final String iataCode) 
    {
        final Map<String, Object> params = new HashMap<>();
        params.put("now", Instant.now());
        params.put("iataCode", iataCode);
        return match("e.scheduledDepartureTime <= :now AND e.scheduledArrivalTime >= :now " + "AND EXISTS (SELECT fp FROM FlightPlan fp JOIN fp.flightLegs fl " + "WHERE fp.flight = e AND fl.arrivalAirport.iataCode = :iataCode)", params);
    }

    @Override
    public List<Flight> findByStatus(final FlightStatus status) {
        final Map<String, Object> params = new HashMap<>();
        params.put("now", Instant.now());
        switch (status) {
            case SCHEDULED:
                return match("e.scheduledDepartureTime > :now", params);
            case AIRBORNE:
                return match("e.scheduledDepartureTime <= :now AND e.scheduledArrivalTime >= :now", params);
            case COMPLETED:
                return match("e.scheduledArrivalTime < :now", params);
            default:
                return Collections.emptyList();
        }
    }

    @Override
    public Optional<Flight> findByFlightDesignator(final String flightDesignator) {
        final Map<String, Object> params = new HashMap<>();
        params.put("flightDesignator", flightDesignator);
        return matchOne("e.flightDesignator = :flightDesignator", params);
    }
    
}
