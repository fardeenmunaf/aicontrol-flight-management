package backoffice.presentation.aircraftmodel;

import aircraftmodel.application.AddEngineModelToAircraftController;
import aircraftmodel.domain.AircraftModel;
import enginemodel.domain.EngineModel;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;
import infrastructure.persistence.PersistenceContext;

import java.util.List;

public class AddEngineModelToAircraftUI extends AbstractUI {

    private final AddEngineModelToAircraftController controller =
            new AddEngineModelToAircraftController(
                    PersistenceContext.repositories().aircraftModels(),
                    PersistenceContext.repositories().engineModels());

    @Override
    protected boolean doShow() {
        try {
            final List<AircraftModel> aircraftModels = controller.listAircraftModelsWithRemainingSlots();
            if (aircraftModels.isEmpty()) {
                System.out.println("No aircraft models have free engine slots.");
                return false;
            }

            System.out.println("Available aircraft models:");
            for (int i = 0; i < aircraftModels.size(); i++) {
                final AircraftModel model = aircraftModels.get(i);
                final int remaining = model.numberOfEngines() - model.supportedEngineModels().size();
                System.out.printf("%d - %s by %s (%d slot(s) free)%n",
                        i + 1,
                        model.modelName(),
                        model.maker().name(),
                        remaining);
            }

            final int aircraftChoice = readInt("Select aircraft model number");
            if (aircraftChoice < 1 || aircraftChoice > aircraftModels.size()) {
                throw new IllegalArgumentException("Invalid aircraft model option");
            }

            final AircraftModel selectedAircraft = aircraftModels.get(aircraftChoice - 1);
            final List<EngineModel> engines = controller.listAssignableEngineModels(selectedAircraft);
            if (engines.isEmpty()) {
                System.out.println("No engine models can be added to this aircraft model.");
                return false;
            }

            System.out.println("Available engine models:");
            for (int i = 0; i < engines.size(); i++) {
                System.out.printf("%d - %s%n", i + 1, engines.get(i).name());
            }

            final int engineChoice = readInt("Select engine model number");
            if (engineChoice < 1 || engineChoice > engines.size()) {
                throw new IllegalArgumentException("Invalid engine model option");
            }

            final EngineModel selectedEngine = engines.get(engineChoice - 1);
            controller.addEngineModelToAircraft(
                    selectedAircraft.modelName(),
                    selectedAircraft.maker().name(),
                    selectedEngine.name());

            System.out.println("Engine model added successfully to aircraft model.");
            return true;
        } catch (final IllegalArgumentException ex) {
            System.out.println("Error: " + ex.getMessage());
            return false;
        }
    }

    @Override
    public String headline() {
        return "Add Engine Model to Aircraft Model";
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
}

