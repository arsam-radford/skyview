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
    private final TextField magnitude = new TextField("6.5");
    private final ListView<Star> stars = new ListView<>();
    private final Label message = new Label("Star list task pending");
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

        getChildren().addAll(searchLabel, search, magnitudeLabel, magnitude, apply, message, stars);
        VBox.setVgrow(stars, Priority.ALWAYS);
    }

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
        // TODO Noah: clear selection, replace rows, and display the result count.
        message.setText("Star list task pending");
    }

    /** Reports valid filter input or displays an input error */
    private void applyFilter() {
        // TODO Noah: read the fields, validate the number, and call window.filterStars.
        message.setText("Filter input task pending");
    }
}
