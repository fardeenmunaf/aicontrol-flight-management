package backoffice.presentation.enginemodel;

import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;
import eapli.framework.presentation.console.SelectWidget;

import enginemodel.application.DeleteEngineModelController;
import enginemodel.domain.EngineModel;
import infrastructure.persistence.PersistenceContext;

public class DeleteEngineModelUI extends AbstractUI {

    private final DeleteEngineModelController controller =
            new DeleteEngineModelController(PersistenceContext.repositories().engineModels());

    @Override
    protected boolean doShow() {
        final Iterable<EngineModel> engineModels = controller.activeEngineModels();

        System.out.println(String.format("%-15s | %-12s | %-25s | %s", "NAME", "TYPE", "MANUFACTURER", "POWER"));
        System.out.println("--------------------------------------------------------------------------------");
        for (final EngineModel engineModel : engineModels) {
            System.out.printf("%-15s | %-12s | Maker: %s (%s) | Thrust SL: %.1f N\n",
                engineModel.name(),
                engineModel.type(),
                engineModel.maker().name(),
                engineModel.maker().country(),
                engineModel.thrustSeaLevelStopped());
        }
        System.out.println();

        final SelectWidget<EngineModel> selector = new SelectWidget<>("Select an Engine Model to delete:", engineModels,
                engineModel -> String.format("%s (Maker: %s)", engineModel.name(), engineModel.maker().name()));
        selector.show();
        final EngineModel selected = selector.selectedElement();

        if (selected == null) {
            return false;
        }

        final String confirm = Console.readLine("Are you sure you want to delete this Engine Model? (Y/N)");
        if ("Y".equalsIgnoreCase(confirm)) {
            try {
                controller.deleteEngineModel(selected);
                System.out.println("\nEngine Model deleted successfully.");
            } catch (Exception e) {
                System.out.println("\nError deleting Engine Model: " + e.getMessage());
            }
        } else {
            System.out.println("\nOperation cancelled.");
        }

        return true;
    }

    @Override
    public String headline() {
        return "Delete Engine Model";
    }
}
