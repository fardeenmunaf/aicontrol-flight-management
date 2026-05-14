package backoffice.presentation.airtransportcompany;

import eapli.framework.presentation.console.AbstractListUI;
import eapli.framework.visitor.Visitor;

import airtransportcompany.application.ListAirTransportCompaniesController;
import airtransportcompany.domain.AirTransportCompany;
import infrastructure.persistence.PersistenceContext;

public class ListAirTransportCompaniesUI extends AbstractListUI<AirTransportCompany> {

    private final ListAirTransportCompaniesController controller =
            new ListAirTransportCompaniesController(
                    PersistenceContext.repositories().airTransportCompanies());

    @Override
    protected Iterable<AirTransportCompany> elements() {
        return controller.allCompanies();
    }

    @Override
    protected Visitor<AirTransportCompany> elementPrinter() {
        return company -> System.out.printf("%-30s | %-8s | %-8s%n",
                company.name(),
                company.iataCode(),
                company.icaoCode());
    }

    @Override
    protected String elementName() {
        return "Air Transport Company";
    }

    @Override
    protected String listHeader() {
        return String.format("%-30s | %-8s | %-8s", "COMPANY NAME", "IATA", "ICAO");
    }

    @Override
    protected String emptyMessage() {
        return "No Air Transport Companies found.";
    }

    @Override
    public String headline() {
        return "List All Air Transport Companies";
    }
}

