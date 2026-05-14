package bootstrap;

import airtransportcompany.domain.AirTransportCompany;
import airtransportcompany.repositories.AirTransportCompanyRepository;

public class AirTransportCompanyBootstrap {

    private final AirTransportCompanyRepository repository;

    public AirTransportCompanyBootstrap(final AirTransportCompanyRepository repository) {
        this.repository = repository;
    }

    public void execute() {
        registerIfAbsent("TAP Air Portugal", "TP", "TAP");
        registerIfAbsent("RyanAir", "FR", "RYR");
        registerIfAbsent("EasyJet", "U2", "EZY");
        registerIfAbsent("Lufthansa", "LH", "DLH");
        registerIfAbsent("Air France", "AF", "AFR");
        registerIfAbsent("British Airways", "BA", "BAW");
        registerIfAbsent("KLM Royal Dutch Airlines", "KL", "KLM");
        registerIfAbsent("Iberia", "IB", "IBE");
        registerIfAbsent("Alitalia", "AZ", "AZA");
        registerIfAbsent("Swiss International Airlines", "LX", "SWR");
    }

    private void registerIfAbsent(final String name,
                                   final String iataCode,
                                   final String icaoCode) {
        // Check if company already exists by IATA code
        if (!repository.findByIataCode(iataCode).isEmpty()) {
            return;
        }

        // Check if company already exists by ICAO code
        if (!repository.findByIcaoCode(icaoCode).isEmpty()) {
            return;
        }

        final var company = new AirTransportCompany(name, iataCode, icaoCode);
        repository.save(company);
    }
}

