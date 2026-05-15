package backoffice.presentation.airport;

import airport.application.ListAirportsController;
import airport.domain.Airport;
import eapli.framework.presentation.console.AbstractListUI;
import eapli.framework.visitor.Visitor;
import infrastructure.persistence.PersistenceContext;

public class ListAirportsUI extends AbstractListUI<Airport> {

    private final ListAirportsController controller =
            new ListAirportsController(PersistenceContext.repositories().airports());

    @Override
    protected Iterable<Airport> elements() {
        return controller.listAirports();
    }

    @Override
    protected Visitor<Airport> elementPrinter() {
        return airport -> System.out.printf("%-4s | %-4s | %-40s | %-20s | %s\n",
                airport.iataCode(),
                airport.icaoCode(),
                airport.name(),
                airport.town(),
                airport.country());
    }

    @Override
    protected String elementName() {
        return "Airport";
    }

    @Override
    protected String listHeader() {
        return String.format("%-4s | %-4s | %-40s | %-20s | %s", "IATA", "ICAO", "NAME", "TOWN", "COUNTRY");
    }

    @Override
    protected String emptyMessage() {
        return "No Airports found.";
    }

    @Override
    public String headline() {
        return "List Airports";
    }
}
