package inmemory;

import airtransportcompany.domain.AirTransportCompany;
import airtransportcompany.repositories.AirTransportCompanyRepository;
import eapli.framework.infrastructure.repositories.impl.inmemory.InMemoryDomainRepository;
import shared.domain.AirTransportCompanyId;

import java.util.ArrayList;
import java.util.List;

public class inMemoryAirTransportCompanyRepository
    extends InMemoryDomainRepository<AirTransportCompany, AirTransportCompanyId>
        implements AirTransportCompanyRepository {

    static {
        InMemoryInitializer.init();
    }

    @Override
    public List<AirTransportCompany> findByName(final String name) {
        final List<AirTransportCompany> result = new ArrayList<>();
        match(a -> a.name().equalsIgnoreCase(name)).forEach(result::add);
        return result;
    }

    @Override
    public List<AirTransportCompany> findByIataCode(final String iataCode) {
        final List<AirTransportCompany> result = new ArrayList<>();
        match(a -> a.iataCode().equalsIgnoreCase(iataCode)).forEach(result::add);
        return result;
    }

    @Override
    public List<AirTransportCompany> findByIcaoCode(final String icaoCode) {
        final List<AirTransportCompany> result = new ArrayList<>();
        match(a -> a.icaoCode().equalsIgnoreCase(icaoCode)).forEach(result::add);
        return result;
    }
}