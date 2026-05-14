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
            if (modelName == null || modelName.trim().isEmpty()) {
                throw new IllegalArgumentException("Model name cannot be empty");
            }
            
            final String makerName = Console.readLine("Manufacturer name");
            if (makerName == null || makerName.trim().isEmpty()) {
                throw new IllegalArgumentException("Manufacturer name cannot be empty");
            }
            
            final String makerCountry = Console.readLine("Manufacturer country");
            if (makerCountry == null || makerCountry.trim().isEmpty()) {
                throw new IllegalArgumentException("Manufacturer country cannot be empty");
            }
            
            final AircraftType type = readAircraftType();
            final int numberOfEngines = readInt("Number of engines (0 if none)");

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

            final List<String> engineModelNames = readEngineModelNames(numberOfEngines);

            final AircraftModelDTO dto = new AircraftModelDTO(
                    modelName.trim(),
                    makerName.trim(),
                    makerCountry.trim(),
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
                    numberOfEngines,
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

    private List<String> readEngineModelNames(final int numberOfEngines) {

        if (numberOfEngines < 0) {
            throw new IllegalArgumentException("Number of engines cannot be negative");
        }

        if (numberOfEngines == 0) {
            System.out.println("No engine models will be associated with this aircraft model.");
            return new ArrayList<>();
        }

        final List<EngineModel> available = new ArrayList<>();
        PersistenceContext.repositories().engineModels().findAll().forEach(available::add);

        if (available.isEmpty()) {
            throw new IllegalArgumentException("No engine models available in the system. Please register engine models first.");
        }

        System.out.println("Available engine models:");
        for (int i = 0; i < available.size(); i++) {
            final EngineModel em = available.get(i);
            System.out.printf("%d - %s%n", i + 1, em.name());
        }

        System.out.printf("Select exactly %d engine model(s) by number.%n", numberOfEngines);
        System.out.printf("You can select up to %d engine(s), or skip by pressing 0.%n", numberOfEngines);
        System.out.println("0 - Finish selection");

        final List<String> names = new ArrayList<>();
        while (names.size() < numberOfEngines) {
            final String input = Console.readLine("Engine model number");
            if (input == null) {
                throw new IllegalArgumentException("Engine model selection was interrupted");
            }
            final String raw = input.trim();
            if ("0".equals(raw)) {
                System.out.println("Selection finished.");
                break;
            }
            final int choice;
            try {
                choice = Integer.parseInt(raw);
            } catch (final NumberFormatException ex) {
                System.out.println("Invalid number: " + raw + " (type only the list number, e.g. 1)");
                continue;
            }
            if (choice < 1 || choice > available.size()) {
                System.out.println("Choice out of range: " + choice);
                continue;
            }
            final EngineModel selected = available.get(choice - 1);
            System.out.println("Selected: " + selected.name());
            names.add(selected.name());
            available.remove(choice - 1);
            System.out.printf("Selected %d/%d engine(s)%n", names.size(), numberOfEngines);
        }

        return names;
    }

    private int readInt(final String prompt) {
        final String raw = Console.readLine(prompt);
        if (raw == null) {
            throw new IllegalArgumentException("Input was interrupted or no value provided");
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (final NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid number: " + raw);
        }
    }

    private Double readDouble(final String prompt) {
        final String raw = Console.readLine(prompt);
        if (raw == null) {
            throw new IllegalArgumentException("Input was interrupted or no value provided");
        }
        try {
            return Double.parseDouble(raw.trim());
        } catch (final NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid number: " + raw);
        }
    }
}
