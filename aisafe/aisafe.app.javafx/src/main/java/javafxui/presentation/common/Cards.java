package javafxui.presentation.common;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Small factory of styled nodes used to assemble data cards consistently
 * across the modules. Pairs with {@link CardListCell}.
 */
public final class Cards {

    private Cards() {
    }

    /** A card container holding the given rows. */
    public static VBox card(final Node... rows) {
        final VBox c = new VBox(6, rows);
        c.getStyleClass().add("data-card");
        return c;
    }

    /** Bold primary line of a card (e.g. the entity name). */
    public static Label title(final String text) {
        final Label l = new Label(text == null ? "" : text);
        l.getStyleClass().add("card-title");
        return l;
    }

    /** Muted secondary line of a card (specs, details). */
    public static Label meta(final String text) {
        final Label l = new Label(text == null ? "" : text);
        l.getStyleClass().add("card-meta");
        l.setWrapText(true);
        return l;
    }

    /** A pill/chip. Pass extra style classes (e.g. "chip-danger") to recolor. */
    public static Label chip(final String text, final String... extraClasses) {
        final Label l = new Label(text == null ? "" : text);
        l.getStyleClass().add("chip");
        l.getStyleClass().addAll(extraClasses);
        return l;
    }

    /** A horizontal, vertically-centred row. */
    public static HBox row(final double spacing, final Node... nodes) {
        final HBox h = new HBox(spacing, nodes);
        h.setAlignment(Pos.CENTER_LEFT);
        return h;
    }

    /** A spacer that pushes following nodes to the right. */
    public static Region grow() {
        final Region r = new Region();
        HBox.setHgrow(r, Priority.ALWAYS);
        return r;
    }
}
