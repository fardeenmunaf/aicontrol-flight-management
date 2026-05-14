package backoffice.presentation.aircraftmodel;

import aircraftmodel.application.RemoveEngineModelFromAircraftController;
import aircraftmodel.domain.AircraftModel;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;
import enginemodel.domain.EngineModel;
import infrastructure.persistence.PersistenceContext;

import java.util.List;

public class RemoveEngineModelFromAircraftUI extends AbstractUI {

    private final RemoveEngineModelFromAircraftController controller =
            new RemoveEngineModelFromAircraftController(
                    PersistenceContext.repositories().aircraftModels(),
                    PersistenceContext.repositories().engineModels(),
                    PersistenceContext.repositories().aircraft());

    @Override
    protected boolean doShow() {
        final List<AircraftModel> models = controller.listAircraftModelsWithMultipleEngines();

        if (models.isEmpty()) {
            System.out.println("No aircraft models available for engine removal (all have only one certified engine).");
            return false;
        }

        System.out.println("Available aircraft models:");
        for (int i = 0; i < models.size(); i++) {
            final AircraftModel model = models.get(i);
            System.out.printf("%d - %s by %s (%d certified engine(s))%n",
                    i + 1,
                    model.modelName(),
                    model.maker().name(),
                    model.supportedEngineModels().size());
        }

        final int modelChoice = readInt("Select aircraft model number");
        if (modelChoice < 1 || modelChoice > models.size()) {
            System.out.println("Error: Invalid aircraft model option");
            return false;
        }

        final AircraftModel selectedModel = models.get(modelChoice - 1);
        final List<EngineModel> engines = controller.listRemovableEngineModels(selectedModel);

        System.out.println("Certified engine models:");
        for (int i = 0; i < engines.size(); i++) {
            System.out.printf("%d - %s%n", i + 1, engines.get(i).name());
        }

        final int engineChoice = readInt("Select engine model number to remove");
        if (engineChoice < 1 || engineChoice > engines.size()) {
            System.out.println("Error: Invalid engine model option");
            return false;
        }

        final EngineModel selectedEngine = engines.get(engineChoice - 1);

        try {
            controller.removeEngineModel(selectedModel, selectedEngine.identity());
            System.out.printf("Engine model '%s' successfully removed from aircraft model '%s'.%n",
                    selectedEngine.name(), selectedModel.modelName());
        } catch (final IllegalStateException e) {
            System.out.println("ERROR: " + e.getMessage());
        }

        return false;
    }

    @Override
    public String headline() {
        return "Remove Engine Model from Aircraft Model";
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