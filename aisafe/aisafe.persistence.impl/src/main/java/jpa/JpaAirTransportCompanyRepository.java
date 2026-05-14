package jpa;

import airtransportcompany.domain.AirTransportCompany;
import airtransportcompany.repositories.AirTransportCompanyRepository;
import eapli.framework.domain.repositories.TransactionalContext;
import shared.domain.AirTransportCompanyId;

import java.util.List;
import java.util.Map;

public class JpaAirTransportCompanyRepository
        extends BaseJpaRepositoryBase<AirTransportCompany, AirTransportCompanyId, AirTransportCompanyId>
        implements AirTransportCompanyRepository {

    public JpaAirTransportCompanyRepository(final TransactionalContext autoTx) {
        super(autoTx, "airTransportCompanyId");
    }

    public JpaAirTransportCompanyRepository(final String persistenceUnitName) {
        super("airTransportCompanyId");
    }

    @Override
    public List<AirTransportCompany> findByName(final String name) {
        return match("LOWER(e.name) = LOWER(:name)", Map.of("name", name));
    }

    @Override
    public List<AirTransportCompany> findByIataCode(final String iataCode) {
        return match("LOWER(e.iataCode) = LOWER(:iataCode)", Map.of("iataCode", iataCode));
    }

    @Override
    public List<AirTransportCompany> findByIcaoCode(final String icaoCode) {
        return match("LOWER(e.icaoCode) = LOWER(:icaoCode)", Map.of("icaoCode", icaoCode));
    }
}