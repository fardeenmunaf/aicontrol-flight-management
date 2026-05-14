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
            System.out.println("\n========== Aircraft Models ==========\n");
            for (AircraftModelDTO dto : aircraftModels) {
                printAircraftModel(dto);
            }
            System.out.println("=====================================\n");
        }
        return true;
    }

    private void printAircraftModel(final AircraftModelDTO dto) {
        System.out.println("Model: " + dto.modelName);
        System.out.println("  Manufacturer: " + dto.makerName + " (" + dto.makerCountry + ")");
        System.out.println("  Type: " + dto.type);
        System.out.println("  Number of Engines: " + dto.numberOfEngines);
        System.out.println("  Empty Weight: " + dto.emptyWeight + " kg");
        System.out.println("  Max Take-Off Weight: " + dto.maximumTakeOffWeight + " kg");
        System.out.println("  Max Zero Fuel Weight: " + dto.maximumZeroFuelWeight + " kg");
        System.out.println("  Max Fuel Capacity: " + dto.maximumFuelCapacity + " kg");
        System.out.println("  Service Ceiling: " + dto.serviceCeiling + " ft");
        System.out.println("  Cruise Speed: " + dto.cruiseSpeed + " km/h");
        System.out.println("  Wing Area: " + dto.wingArea + " m²");
        System.out.println("  Wing Span: " + dto.wingSpan + " m");
        System.out.println("  Drag Coefficient: " + dto.dragCoefficient);
        System.out.println("  Lift Coefficient: " + dto.liftCoefficient);
        if (dto.engineModelNames != null && !dto.engineModelNames.isEmpty()) {
            System.out.println("  Certified Engine Models:");
            for (String engineName : dto.engineModelNames) {
                System.out.println("    - " + engineName);
            }
        } else {
            System.out.println("  Certified Engine Models: None");
        }
        System.out.println();
    }

    @Override
    public String headline() {
        return "View Aircraft Models";
    }
}
