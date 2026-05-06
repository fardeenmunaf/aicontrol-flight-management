package backoffice.presentation.aircraftmodel;

import aircraftmodel.dto.AircraftModelDTO;
import aircraftmodel.application.RegisterAircraftModelController;
import aircraftmodel.domain.AircraftType;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;
import infrastructure.persistence.PersistenceContext;

import enginemodel.domain.EngineModel;
import java.util.ArrayList;
import java.util.List;

public class RegisterAircraftModelUI extends AbstractUI {

    private final RegisterAircraftModelController controller =
            new RegisterAircraftModelController(
                    PersistenceContext.repositories().aircraftModels(),
                    PersistenceContext.repositories().engineModels());

    @Override
    protected boolean doShow() {
        try {
            final String modelName = Console.readLine("Model name");
            final String makerName = Console.readLine("Manufacturer name");
            final String makerCountry = Console.readLine("Manufacturer country");
            final AircraftType type = readAircraftType();

            final Double emptyWeight = readDouble("Empty weight (kg)");
            final Double maximumTakeOffWeight = readDouble("Maximum take-off weight (kg)");
            final Double maximumZeroFuelWeight = readDouble("Maximum zero fuel weight (kg)");
            final Double maximumFuelCapacity = readDouble("Maximum fuel capacity (kg)");
            final Double serviceCeiling = readDouble("Service ceiling (ft)");
            final Double cruiseSpeed = readDouble("Cruise speed (km/h)");
            final Double wingArea = readDouble("Wing area (m^2)");
            final Double wingSpan = readDouble("Wing span (m)");
            final Double dragCoefficient = readDouble("Drag coefficient (Cd)");
            final Double liftCoefficient = readDouble("Lift coefficient (Cl)");

            final List<String> engineModelNames = readEngineModelNames();

            final AircraftModelDTO dto = new AircraftModelDTO(
                    modelName,
                    makerName,
                    makerCountry,
                    type,
                    emptyWeight,
                    maximumTakeOffWeight,
                    maximumZeroFuelWeight,
                    maximumFuelCapacity,
                    serviceCeiling,
                    cruiseSpeed,
                    wingArea,
                    wingSpan,
                    dragCoefficient,
                    liftCoefficient,
                    engineModelNames
            );

            controller.registerAircraftModel(dto);
            System.out.println("Aircraft model registered successfully.");
            return true;
        } catch (final IllegalArgumentException ex) {
            System.out.println("Error: " + ex.getMessage());
            return false;
        }
    }

    @Override
    public String headline() {
        return "Register Aircraft Model";
    }

    private AircraftType readAircraftType() {
        final AircraftType[] values = AircraftType.values();
        System.out.println("Aircraft types:");
        for (int i = 0; i < values.length; i++) {
            System.out.printf("%d - %s%n", i + 1, values[i]);
        }

        final int choice = readInt("Select aircraft type");
        if (choice < 1 || choice > values.length) {
            throw new IllegalArgumentException("Invalid aircraft type option");
        }
        return values[choice - 1];
    }

    private List<String> readEngineModelNames() {

        final List<EngineModel> available = new ArrayList<>();
        PersistenceContext.repositories().engineModels().findAll().forEach(available::add);

        if (available.isEmpty()) {
            throw new IllegalArgumentException("No engine models available in the system. Please register engine models first.");
        }

        System.out.println("Available engine models:");
        for (int i = 0; i < available.size(); i++) {
            final EngineModel em = available.get(i);
            // assume EngineModel has a name() method
            System.out.printf("%d - %s%n", i + 1, em.name());
        }

        System.out.println("Select at least one certified engine model by number.");
        System.out.println("Type one number per line. Leave blank to finish selection.");

        final List<String> names = new ArrayList<>();
        while (true) {
            final String raw = Console.readLine("Engine model number").trim();
            if (raw.isEmpty()) {
                break;
            }
            final int choice;
            try {
                choice = Integer.parseInt(raw);
            } catch (final NumberFormatException ex) {
                System.out.println("Invalid number: " + raw);
                continue;
            }
            if (choice < 1 || choice > available.size()) {
                System.out.println("Choice out of range: " + choice);
                continue;
            }
            final EngineModel selected = available.get(choice - 1);
            names.add(selected.name());
        }

        if (names.isEmpty()) {
            throw new IllegalArgumentException("At least one certified engine model is required");
        }

        return names;
    }

    private int readInt(final String prompt) {
        final String raw = Console.readLine(prompt);
        try {
            return Integer.parseInt(raw.trim());
        } catch (final NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid number: " + raw);
        }
    }

    private Double readDouble(final String prompt) {
        final String raw = Console.readLine(prompt);
        try {
            return Double.parseDouble(raw.trim());
        } catch (final NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid number: " + raw);
        }
    }
}
