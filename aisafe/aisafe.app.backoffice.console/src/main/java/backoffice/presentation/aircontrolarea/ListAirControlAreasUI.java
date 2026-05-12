package backoffice.presentation.aircontrolarea;

import aircontrolarea.application.ListAirControlAreasController;
import aircontrolarea.domain.AirControlArea;
import eapli.framework.presentation.console.AbstractListUI;
import eapli.framework.visitor.Visitor;
import infrastructure.persistence.PersistenceContext;

public class ListAirControlAreasUI extends AbstractListUI<AirControlArea> {

    private final ListAirControlAreasController controller =
            new ListAirControlAreasController(
                    PersistenceContext.repositories().airControlAreas());

    @Override
    protected Iterable<AirControlArea> elements() {
        return controller.allAirControlAreas();
    }

    @Override
    protected Visitor<AirControlArea> elementPrinter() {
        return area -> System.out.printf("%-10s | %-10.2f | %-50s%n",
                area.areaCode(),
                area.minimumRequiredArrivalFuel(),
                area.geographicBoundaries());
    }

    @Override
    protected String elementName() {
        return "Air Control Area";
    }

    @Override
    protected String listHeader() {
        return String.format("%-10s | %-10s | %-50s", "AREA CODE", "MIN FUEL", "GEOGRAPHIC BOUNDARIES");
    }

    @Override
    protected String emptyMessage() {
        return "No Air Control Areas found.";
    }

    @Override
    public String headline() {
        return "List All Air Control Areas";
    }
}