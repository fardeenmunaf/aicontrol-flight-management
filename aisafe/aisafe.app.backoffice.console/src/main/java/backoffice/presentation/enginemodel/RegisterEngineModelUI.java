package backoffice.presentation.enginemodel;

import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

import enginemodel.application.RegisterEngineModelController;
import enginemodel.domain.EngineType;
import enginemodel.domain.FuelType;
import infrastructure.persistence.PersistenceContext;

public class RegisterEngineModelUI extends AbstractUI {

    private final RegisterEngineModelController controller =
            new RegisterEngineModelController(PersistenceContext.repositories().engineModels());

    @Override
    protected boolean doShow() {
        try {
            final String name = Console.readLine("Engine model name");
            final EngineType type = readEngineType();
            final Double power = readDouble("Power");
            final Double efficiency = readDouble("Efficiency (TSFC)");
            final Double thrustSeaLevelStopped = readDouble("Thrust sea level stopped");
            final Double thrustSeaLevelCruise = readDouble("Thrust sea level cruise");
            final Double thrustSpecificFuelConsumption = readDouble("Thrust specific fuel consumption (TSFC)");
            final String makerName = Console.readLine("Manufacturer name");
            final String makerCountry = Console.readLine("Manufacturer country");
            final FuelType fuelType = readFuelType();
            final Double fuelDensity = readDouble("Fuel density");
            final Double fuelSpecificEnergy = readDouble("Fuel specific energy");

            controller.registerEngineModel(
                    name,
                    type,
                    power,
                    efficiency,
                    thrustSeaLevelStopped,
                    thrustSeaLevelCruise,
                    thrustSpecificFuelConsumption,
                    makerName,
                    makerCountry,
                    fuelType,
                    fuelDensity,
                    fuelSpecificEnergy
            );

            System.out.println("Engine model registered successfully.");
            return true;
        } catch (final IllegalArgumentException ex) {
            System.out.println("Error: " + ex.getMessage());
            return false;
        }
    }

    @Override
    public String headline() {
        return "Register Engine Model";
    }

    private EngineType readEngineType() {
        final EngineType[] values = EngineType.values();
        System.out.println("Engine types:");
        for (int i = 0; i < values.length; i++) {
            System.out.printf("%d - %s%n", i + 1, values[i]);
        }
        final int choice = readInt("Select engine type");
        if (choice < 1 || choice > values.length) {
            throw new IllegalArgumentException("Invalid engine type option");
        }
        return values[choice - 1];
    }

    private FuelType readFuelType() {
        final FuelType[] values = FuelType.values();
        System.out.println("Fuel types:");
        for (int i = 0; i < values.length; i++) {
            System.out.printf("%d - %s%n", i + 1, values[i]);
        }
        final int choice = readInt("Select fuel type");
        if (choice < 1 || choice > values.length) {
            throw new IllegalArgumentException("Invalid fuel type option");
        }
        return values[choice - 1];
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
