package backoffice.presentation.enginemodel;

import eapli.framework.presentation.console.AbstractListUI;
import eapli.framework.visitor.Visitor;

import enginemodel.application.ListEngineModelsController;
import enginemodel.domain.EngineModel;
import infrastructure.persistence.PersistenceContext;

public class ListEngineModelsUI extends AbstractListUI<EngineModel> {

    private final ListEngineModelsController controller =
            new ListEngineModelsController(PersistenceContext.repositories().engineModels());

    @Override
    protected Iterable<EngineModel> elements() {
        return controller.listEngineModels();
    }

    @Override
    protected Visitor<EngineModel> elementPrinter() {
        return engineModel -> System.out.printf("%-15s | %-12s | Maker: %s (%s) | Thrust SL: %.1f N\n",
                engineModel.name(),
                engineModel.type(),
                engineModel.maker().name(),
                engineModel.maker().country(),
                engineModel.thrustSeaLevelStopped());
    }

    @Override
    protected String elementName() {
        return "Engine Model";
    }

    @Override
    protected String listHeader() {
        return String.format("%-15s | %-12s | %-25s | %s", "NAME", "TYPE", "MANUFACTURER", "POWER");
    }

    @Override
    protected String emptyMessage() {
        return "No Engine Models found.";
    }

    @Override
    public String headline() {
        return "List Engine Models";
    }
}
