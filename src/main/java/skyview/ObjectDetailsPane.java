package skyview;

import java.util.List;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/** Displays supplied star data without network requests. */
public class ObjectDetailsPane extends VBox {
    private static final double SPACING = 8;

    private final Label name = new Label("Select a star");
    private final Label identifiers = new Label();
    private final Label coordinates = new Label();
    private final Label brightness = new Label();
    private final Label spectrum = new Label();
    private final Label composition = new Label();

    /** Creates the labels for the details task. */
    public ObjectDetailsPane() {
        super(SPACING);

        getChildren().addAll(name, identifiers, coordinates, brightness, spectrum, composition);

        for (javafx.scene.Node child : getChildren()) {
            ((Label) child).setWrapText(true);
        }
    }

    /**
     * Displays a star and its supplied abundance results.
     * A null star clears the panel. An empty list means no composition measurements.
     * @param star selected star, or null
     * @param abundances non null results for this star
     */
    public void showStar(Star star, List<ElementAbundance> abundances) {
        // TODO Seth: populate every label, including missing values and cleared selection.
        name.setText("Select a star");
        if (star != null) {
            name.setText(star.getDisplayName());
        }

        composition.setText("Details task pending");
    }
}
