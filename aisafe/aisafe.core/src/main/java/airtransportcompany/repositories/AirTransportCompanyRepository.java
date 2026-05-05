package airtransportcompany.repositories;

import eapli.framework.domain.repositories.DomainRepository;
import airtransportcompany.domain.AirTransportCompany;
import shared.domain.AirTransportCompanyId;

import java.util.List;

/**
 * Repository interface for AirTransportCompany aggregate.
 * Defines domain-specific persistence operations for AirTransportCompany entities.
 */
public interface AirTransportCompanyRepository extends DomainRepository<AirTransportCompanyId, AirTransportCompany> {

    /**
     * Returns all companies matching a given name.
     * Useful for searching companies by their commercial name.
     *
     * @param name the company name to filter by
     * @return list of companies with the given name, never null
     */
    List<AirTransportCompany> findByName(String name);

    /**
     * Returns all companies matching a given IATA code.
     * Useful for regulatory or operational lookups.
     *
     * @param iataCode the IATA code to filter by
     * @return list of companies with the given IATA code, never null
     */
    List<AirTransportCompany> findByIataCode(String iataCode);

    /**
     * Returns all companies matching a given ICAO code.
     * Useful for regulatory or operational lookups.
     *
     * @param icaoCode the ICAO code to filter by
     * @return list of companies with the given ICAO code, never null
     */
    List<AirTransportCompany> findByIcaoCode(String icaoCode);
}