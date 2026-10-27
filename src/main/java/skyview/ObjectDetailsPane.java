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
        if (star == null) {
            name.setText("Select a star");
            identifiers.setText("");
            coordinates.setText("");
            brightness.setText("");
            spectrum.setText("");
            composition.setText("");
            return;
        }

        name.setText(star.getDisplayName());
        identifiers.setText("HYG: " + star.getHygId()
                + " | HIP: " + optionalValue(star.getHipId())
                + " | HD: " + optionalValue(star.getHdId()));
        coordinates.setText("J2000 RA: " + star.getRightAscensionHours() + " hours"
                + " | Dec: " + star.getDeclinationDegrees() + " degrees");
        brightness.setText("Apparent magnitude: " + star.getApparentMagnitude());
        spectrum.setText("Spectral type: " + spectralType(star.getSpectralType())
                + " | Color index (B-V): " + optionalValue(star.getColorIndex()));

        if (abundances.isEmpty()) {
            composition.setText("No composition measurements");
        } else {
            StringBuilder details = new StringBuilder("Composition:");
            for (ElementAbundance abundance : abundances) {
                String measuredValue;
                if (abundance.getMedianAbundanceDex() == null) {
                    measuredValue = "unknown";
                } else {
                    measuredValue = abundance.getMedianAbundanceDex() + " dex";
                }

                details.append("\n")
                        .append(optionalValue(abundance.getElement()))
                        .append(": ").append(measuredValue)
                        .append(" (solar normalization: ")
                        .append(optionalValue(abundance.getSolarNormalization()))
                        .append(")");
            }
            composition.setText(details.toString());
        }
    }

    /** Shows an unavailable optional value as unknown, preserving numeric zero. */
    private String optionalValue(Object value) {
        if (value == null) {
            return "unknown";
        } else {
            return value.toString();
        }
    }

    /** A blank spectral type is also unavailable. */
    private String spectralType(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        } else {
            return value;
        }
    }
}

