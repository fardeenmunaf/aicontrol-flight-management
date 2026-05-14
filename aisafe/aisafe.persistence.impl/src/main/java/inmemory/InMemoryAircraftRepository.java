package inmemory;

import aircraft.domain.Aircraft;
import aircraft.domain.CabinConfiguration;
import aircraft.repositories.AircraftRepository;
import eapli.framework.infrastructure.repositories.impl.inmemory.InMemoryDomainRepository;
import shared.domain.AircraftId;
import shared.domain.AirTransportCompanyId;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class InMemoryAircraftRepository
        extends InMemoryDomainRepository<Aircraft, AircraftId>
        implements AircraftRepository {

    static {
        InMemoryInitializer.init();
    }

    @Override
    public List<Aircraft> findByCabinConfiguration(final CabinConfiguration cabinConfiguration) {
        if (cabinConfiguration == null) {
            return List.of();
        }
        final List<Aircraft> result = new ArrayList<>();
        match(a -> a.cabinConfiguration().equals(cabinConfiguration)).forEach(result::add);
        return result;
    }

    @Override
    public List<Aircraft> findByOperationStatus(final String operationStatus) {
        if (operationStatus == null || operationStatus.isBlank()) {
            return List.of();
        }
        final List<Aircraft> result = new ArrayList<>();
        match(a -> a.operationStatus() != null && a.operationStatus().name().equalsIgnoreCase(operationStatus)).forEach(result::add);
        return result;
    }

    @Override
    public List<Aircraft> findByRegistrationCountry(final String registrationCountry) {
        if (registrationCountry == null || registrationCountry.isBlank()) {
            return List.of();
        }
        final List<Aircraft> result = new ArrayList<>();
        match(a -> a.registrationCountry().equalsIgnoreCase(registrationCountry)).forEach(result::add);
        return result;
    }

    /**
     * Find aircraft by registration number.
     *
     * @param registrationNumber the registration number to search for
     * @return optional containing the aircraft if found
     */
    public Optional<Aircraft> findByRegistrationNumber(final String registrationNumber) {
        if (registrationNumber == null || registrationNumber.isBlank()) {
            return Optional.empty();
        }
        final List<Aircraft> result = new ArrayList<>();
        match(a -> a.registrationNumber().equalsIgnoreCase(registrationNumber)).forEach(result::add);
        if (result.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(result.get(0));
    }

    @Override
    public List<Aircraft> findByAirTransportCompanyId(final AirTransportCompanyId companyId) {
        if (companyId == null) {
            return List.of();
        }
        final List<Aircraft> result = new ArrayList<>();
        match(a -> a.airTransportCompany() != null && a.airTransportCompany().identity().equals(companyId)).forEach(result::add);
        return result;
    }
}
