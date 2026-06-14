package javafxui.presentation.common;

import javafx.scene.Node;
import javafx.scene.control.ListCell;

import java.util.function.Function;

/**
 * A {@link ListCell} that renders each item as a card built by the supplied
 * function. Lets a {@code ListView} stand in for the old dense tables while
 * keeping selection, virtualization and item binding intact.
 */
public final class CardListCell<T> extends ListCell<T> {

    private final Function<T, Node> cardBuilder;

    public CardListCell(final Function<T, Node> cardBuilder) {
        this.cardBuilder = cardBuilder;
        getStyleClass().add("card-cell");
    }

    @Override
    protected void updateItem(final T item, final boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
            setText(null);
            setGraphic(null);
        } else {
            setGraphic(cardBuilder.apply(item));
        }
    }
}
