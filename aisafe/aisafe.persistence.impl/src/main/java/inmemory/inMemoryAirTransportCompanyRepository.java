package inmemory;

import airtransportcompany.domain.AirTransportCompany;
import airtransportcompany.repositories.AirTransportCompanyRepository;
import eapli.framework.infrastructure.repositories.impl.inmemory.InMemoryDomainRepository;
import shared.domain.AirTransportCompanyId;

import java.util.Optional;

public class inMemoryAirTransportCompanyRepository
    extends InMemoryDomainRepository<AirTransportCompany, AirTransportCompanyId>
        implements AirTransportCompanyRepository {

    static {
        InMemoryInitializer.init();
    }

    @Override
    public Optional<AirTransportCompany> findByName(final String name) {
        return matchOne(a -> a.name().equalsIgnoreCase(name));
    }

    public Optional<AirTransportCompany> findByIataCode(final String iataCode) {
        return matchOne(a -> a.iataCode().equalsIgnoreCase(iataCode));
    }

    public Optional<AirTransportCompany> findByIcaoCode(final String icaoCode) {
        return matchOne(a -> a.icaoCode().equalsIgnoreCase(icaoCode));
    }
}