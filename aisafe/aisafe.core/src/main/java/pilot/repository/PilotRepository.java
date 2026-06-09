package pilot.repository;

import eapli.framework.domain.repositories.DomainRepository;
import pilot.domain.Pilot;
import pilot.domain.PilotStatus;
import aircraftmodel.domain.AircraftModel;
import shared.domain.PilotId;
import shared.domain.UserId;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Pilot aggregate.
 * Defines domain-specific persistence operations for Pilot entities.
 *
 * @author LAPR4 Team
 */
public interface PilotRepository extends DomainRepository<PilotId, Pilot> {

    /**
     * Returns all pilots matching a given status.
     * Useful for finding all active pilots available for flight plan assignment.
     *
     * @param status the pilot status to filter by
     * @return list of pilots with the given status, never null
     */
    List<Pilot> findByStatus(PilotStatus status);

    /**
     * Returns all pilots certified for a given aircraft model.
     * Used during flight plan creation to find eligible pilots
     * for a specific aircraft type.
     *
     * @param aircraftModel the aircraft model to check certification against
     * @return list of certified pilots, never null
     */
    List<Pilot> findCertifiedFor(AircraftModel aircraftModel);

    Optional<Pilot> findByUserId(UserId userId);
}