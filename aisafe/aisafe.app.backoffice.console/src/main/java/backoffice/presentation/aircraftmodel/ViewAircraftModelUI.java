package backoffice.presentation.aircraftmodel;

import eapli.framework.presentation.console.AbstractUI;
import aircraftmodel.dto.AircraftModelDTO;
import aircraftmodel.application.ViewAircraftModelController;
import infrastructure.persistence.PersistenceContext;


import java.util.List;


public class ViewAircraftModelUI extends AbstractUI{

    private final ViewAircraftModelController controller = new ViewAircraftModelController(PersistenceContext.repositories().aircraftModels());



    @Override
    protected boolean doShow() {
        List<AircraftModelDTO> aircraftModels = controller.listAircraftModels();
        if (aircraftModels.isEmpty()) {
            System.out.println("No aircraft models found.");
        } else {
            System.out.println("Aircraft Models:");
            for (AircraftModelDTO dto : aircraftModels) {
                System.out.println(dto);
            }
        }
        return true;
    }

    @Override
    public String headline() {
        return "View Aircraft Models";
    }
}
