package backoffice.presentation.aircraftmodel;

import aircraftmodel.dto.AircraftModelDTO;
import aircraftmodel.application.RegisterAircraftModelController;
import aircraftmodel.domain.AircraftType;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;
import infrastructure.persistence.PersistenceContext;

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

            final Double emptyWeight = readDouble("Empty weight");
            final Double maximumTakeOffWeight = readDouble("Maximum take-off weight");
            final Double maximumZeroFuelWeight = readDouble("Maximum zero fuel weight");
            final Double maximumFuelCapacity = readDouble("Maximum fuel capacity");
            final Double serviceCeiling = readDouble("Service ceiling");
            final Double cruiseSpeed = readDouble("Cruise speed");
            final Double wingArea = readDouble("Wing area");
            final Double wingSpan = readDouble("Wing span");
            final Double dragCoefficient = readDouble("Drag coefficient");
            final Double liftCoefficient = readDouble("Lift coefficient");

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
        System.out.println("Enter at least one certified engine model name.");
        System.out.println("Type one per line. Leave blank to finish.");

        final List<String> names = new ArrayList<>();
        while (true) {
            final String value = Console.readLine("Engine model name").trim();
            if (value.isEmpty()) {
                break;
            }
            names.add(value);
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
