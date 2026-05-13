package jpa;

import aircraft.domain.Aircraft;
import aircraft.domain.CabinConfiguration;
import aircraft.repositories.AircraftRepository;
import eapli.framework.domain.repositories.TransactionalContext;
import shared.domain.AircraftId;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class JpaAircraftRepository
        extends BaseJpaRepositoryBase<Aircraft, AircraftId, AircraftId>
        implements AircraftRepository {

    public JpaAircraftRepository(final TransactionalContext autoTx) {
        super(autoTx, "aircraftId");
    }

    public JpaAircraftRepository(final String persistenceUnitName) {
        super("aircraftId");
    }

    @Override
    public List<Aircraft> findByCabinConfiguration(final CabinConfiguration cabinConfiguration) {
        if (cabinConfiguration == null) {
            return List.of();
        }
        return match("e.cabinConfiguration = :config",
                Map.of("config", cabinConfiguration));
    }

    @Override
    public List<Aircraft> findByOperationStatus(final String operationStatus) {
        if (operationStatus == null || operationStatus.isBlank()) {
            return List.of();
        }
        return match("LOWER(e.operationStatus) = LOWER(:status)",
                Map.of("status", operationStatus));
    }

    @Override
    public List<Aircraft> findByRegistrationCountry(final String registrationCountry) {
        if (registrationCountry == null || registrationCountry.isBlank()) {
            return List.of();
        }
        return match("LOWER(e.registrationCountry) = LOWER(:country)",
                Map.of("country", registrationCountry));
    }

    /**
     * Find aircraft by registration number.
     * Registration number is stored in aircraftId.
     *
     * @param registrationNumber the registration number to search for
     * @return optional containing the aircraft if found
     */
    public Optional<Aircraft> findByRegistrationNumber(final String registrationNumber) {
        if (registrationNumber == null || registrationNumber.isBlank()) {
            return Optional.empty();
        }
        final var result = match("e.aircraftId = :regNum",
                Map.of("regNum", AircraftId.valueOf(registrationNumber)));
        return result.stream().findFirst();
    }
}
