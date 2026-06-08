package airtransport.presentation.aircraft;

import aircraft.application.RegisterAircraftController;
import airtransportcompany.domain.AirTransportCompany;
import customercollaborator.domain.CustomerCollaborator;
import customercollaborator.domain.CustomerType;
import infrastructure.persistence.PersistenceContext;
import eapli.framework.presentation.console.AbstractUI;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import aircraft.domain.AircraftOperationStatus;
import aircraftmodel.domain.AircraftModel;
import enginemodel.domain.EngineModel;
import eapli.framework.io.util.Console;
import shared.domain.UserId;

import java.util.List;

public class RegisterAircraftUI extends AbstractUI {

    private final RegisterAircraftController controller = new RegisterAircraftController(
            PersistenceContext.repositories().aircraft(),
            PersistenceContext.repositories().aircraftModels(),
            PersistenceContext.repositories().customerCollaborators(),
            PersistenceContext.repositories().airTransportCompanies(),
            PersistenceContext.repositories().engineModels());

    @Override
    protected boolean doShow() {
        try {
            final var session = AuthzRegistry.authorizationService().session().orElseThrow(IllegalStateException::new);
            final var userId = UserId.valueOf(session.authenticatedUser().identity().toString());
            final CustomerCollaborator collaborator = PersistenceContext.repositories().customerCollaborators()
                    .findByUserId(userId)
                    .orElseThrow(() -> new IllegalStateException("Current user is not a customer collaborator"));

            if (collaborator.customerType() != CustomerType.AIR_TRANSPORT_COMPANY) {
                System.out.println("You are not assigned to an air transport company.");
                return true;
            }

            final AirTransportCompany company = PersistenceContext.repositories().airTransportCompanies()
                    .ofIdentity(collaborator.airTransportCompanyId())
                    .orElseThrow(() -> new IllegalStateException("Air transport company not found"));

            System.out.println("Registering aircraft for: " + company.name());
            System.out.println();
            
            final java.util.List<AircraftModel> models = new java.util.ArrayList<>();
            PersistenceContext.repositories().aircraftModels().findAll().forEach(models::add);

            if (models.isEmpty()) {
                System.out.println("No aircraft models available. Please create aircraft models first.");
                return true;
            }

            System.out.println("Available aircraft models:");
            for (int i = 0; i < models.size(); i++) {
                final AircraftModel m = models.get(i);
                System.out.printf("%d) %s (%s) - Engines: %d\n", i + 1, m.modelName(), m.maker().name(), m.numberOfEngines());
            }

            final int modelChoice = readInt("Select aircraft model number");
            if (modelChoice < 1 || modelChoice > models.size()) {
                throw new IllegalArgumentException("Invalid aircraft model option");
            }

            final AircraftModel selectedModel = models.get(modelChoice - 1);
            final String aircraftModelId = selectedModel.aircraftModelId().toString();

            final List<EngineModel> supportedEngines = new java.util.ArrayList<>(selectedModel.supportedEngineModels());
            if (supportedEngines.isEmpty()) {
                System.out.println("No certified engines found for this model. Contact admin.");
                return true;
            }

            System.out.println("Certified engines for " + selectedModel.modelName() + ":");
            for (int i = 0; i < supportedEngines.size(); i++) {
                System.out.printf("%d) %s\n", i + 1, supportedEngines.get(i).name());
            }

            final int engineChoice = readInt("Select installed engine number");
            if (engineChoice < 1 || engineChoice > supportedEngines.size()) {
                throw new IllegalArgumentException("Invalid engine option");
            }
            final EngineModel selectedEngine = supportedEngines.get(engineChoice - 1);
            final String engineModelId = selectedEngine.identity().toString();

            final double payloadBudgetKg = selectedModel.maximumZeroFuelWeight() - selectedModel.emptyWeight() - 500.0;
            final int estimatedMaxPeople = (int) Math.floor(Math.max(0.0, payloadBudgetKg) / 100.0);

            System.out.printf("Estimated max onboard people for %s: %d\n", selectedModel.modelName(), estimatedMaxPeople);
            System.out.println("(calculated with MZFW, empty weight, 500kg safety margin, 100kg/person)");

            final String registrationNumber = readLine("Registration number");
            final String registrationCountry = readLine("Registration country");
            final AircraftOperationStatus operationStatus = chooseOperationStatus();
            final int flightCrew = readInt("Flight crew count");

            final int estimatedMaxSeats = Math.max(0, estimatedMaxPeople - flightCrew);
            System.out.printf("Estimated max passenger seats with crew %d: %d\n", flightCrew, estimatedMaxSeats);

            final int econ = readInt("Economy seats");
            final int bus = readInt("Business seats");
            final int first = readInt("First class seats");

            final int totalSeats = econ + bus + first;
            if (totalSeats > estimatedMaxSeats) {
                throw new IllegalArgumentException(
                        "Total seats (" + totalSeats + ") exceed estimated seat limit (" + estimatedMaxSeats + ") for this model and crew"
                );
            }

            controller.registerAircraft(registrationNumber, registrationCountry, operationStatus, flightCrew, aircraftModelId, engineModelId, econ, bus, first);
            System.out.println("Aircraft registered successfully for " + company.name() + ".");
        } catch (final Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
        return true;
    }

    @Override
    public String headline() {
        return "Register Aircraft";
    }

    private String readLine(final String prompt) {
        final String raw = Console.readLine(prompt);
        if (raw == null) {
            throw new IllegalArgumentException("Input was interrupted or no value provided");
        }
        return raw.trim();
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

    private AircraftOperationStatus chooseOperationStatus() {
        final AircraftOperationStatus[] statuses = AircraftOperationStatus.values();
        System.out.println("Available operation statuses:");
        for (int i = 0; i < statuses.length; i++) {
            System.out.printf("%d) %s\n", i + 1, statuses[i].name());
        }

        final int choice = readInt("Select operation status number");
        if (choice < 1 || choice > statuses.length) {
            throw new IllegalArgumentException("Invalid operation status option");
        }

        return statuses[choice - 1];
    }
}
