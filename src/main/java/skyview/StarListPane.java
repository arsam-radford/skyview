package skyview;

import java.util.List;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** Displays a supplied star list and reports user input to the application */
public class StarListPane extends VBox {
    private static final double SPACING = 8;

    private final TextField search = new TextField();
    private final TextField magnitude = new TextField();

    // the window clears this selection when a deep sky object is selected
    final ListView<Star> stars = new ListView<>();

    private final Label message = new Label("Loading stars");
    private final SkyviewWindow window;

    /**
     * Creates the controls and the selection hook
     * @param window receives selections and valid filter input
     */
    public StarListPane(SkyviewWindow window) {
        super(SPACING);
        this.window = window;

        final Button apply = new Button("Apply filter");
        apply.setOnAction(event -> applyFilter());
        search.setOnAction(event -> applyFilter());
        magnitude.setOnAction(event -> applyFilter());

        stars.getSelectionModel().selectedItemProperty().addListener(
                (property, previous, selected) -> window.selectStar(selected));
        configureList();

        final Label searchLabel = new Label("Star name");
        searchLabel.setLabelFor(search);

        final Label magnitudeLabel = new Label("Maximum magnitude");
        magnitudeLabel.setLabelFor(magnitude);
        magnitude.setPromptText("Blank shows all catalog stars");

        getChildren().addAll(searchLabel, search, magnitudeLabel, magnitude, apply, message, stars);
        VBox.setVgrow(stars, Priority.ALWAYS);
    }

    // sets the names shown in the star list
    private void configureList() {
        stars.setPlaceholder(new Label("No stars to show"));

        stars.setCellFactory(view -> new ListCell<>() {
            @Override
            protected void updateItem(Star star, boolean empty) {
                super.updateItem(star, empty);
                setText(null);

                if (!empty && star != null) {
                    setText(star.getDisplayName());
                }
            }
        });
    }

    /**
     * Replaces the displayed rows and clears the previous selection
     * @param results non null stars in display order
     */
    public void showStars(List<Star> results) {
        stars.getSelectionModel().clearSelection();
        stars.getItems().setAll(results);
        message.setText("Stars: " + results.size());
    }

    // reports valid filter input or displays an input error
    private void applyFilter() {
        final String magnitudeText = magnitude.getText();
        if (magnitudeText.isBlank()) {
            message.setText("");
            window.filterStars(search.getText(), Double.MAX_VALUE);
            return;
        }

        final double limit;
        try {
            limit = Double.parseDouble(magnitudeText.trim());
        } catch (NumberFormatException exception) {
            message.setText("Invalid magnitude: " + magnitudeText);
            return;
        }
        if (!Double.isFinite(limit)) {
            message.setText("Magnitude must be finite: " + magnitudeText);
            return;
        }

        message.setText("");
        window.filterStars(search.getText(), limit);

    }

}
