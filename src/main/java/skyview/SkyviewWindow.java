package skyview;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/** Connects the four components using local examples. */
public class SkyviewWindow extends Application {
    private static final int WIDTH = 1100;
    private static final int HEIGHT = 740;
    private static final int PANEL_WIDTH = 260;
    private static final int SPACING = 12;
    private static final double MAGNITUDE_LIMIT = 6.5;

    private final PreviewData examples = new PreviewData();
    private final ObjectDetailsPane details = new ObjectDetailsPane();
    private final StarListPane starList = new StarListPane(this);
    private final TextArea result = new TextArea("Select a component example below");
    private StarStore catalog = new StarStore(List.of());

    /**
     * Opens the window with the local HYG sample.
     * @param stage application window
     */
    @Override
    public void start(Stage stage) {
        final BorderPane root = new BorderPane();
        root.setPadding(new Insets(SPACING));
        root.setTop(new Label("Skyview | Component preview | Example data only"));

        starList.setPrefWidth(PANEL_WIDTH);
        details.setPrefWidth(PANEL_WIDTH);
        root.setLeft(starList);
        root.setRight(details);
        root.setBottom(createExamples());

        loadCatalog();
        root.setCenter(createCatalogExamples());

        stage.setTitle("Skyview component preview");
        stage.setScene(new Scene(root, WIDTH, HEIGHT));
        stage.show();
    }

    /** @return star buttons that work before the list task is complete */
    private VBox createCatalogExamples() {
        final VBox preview = new VBox(SPACING, new Label("Sky view"),
                new Label("3D rendering will be added here"), new Label("Direct star examples"));
        preview.setPadding(new Insets(SPACING));

        for (Star star : catalog.getStars()) {
            final Button button = new Button(star.getDisplayName());
            button.setOnAction(event -> selectStar(star));
            preview.getChildren().add(button);
        }

        return preview;
    }

    /** Loads the existing two star file for the catalog and list tasks. */
    private void loadCatalog() {
        try {
            catalog = new StarStore(new HygCatalog().loadStars(Path.of("data/hyg-sample.csv"), MAGNITUDE_LIMIT));
            starList.showStars(catalog.getStars());
        } catch (IOException exception) {
            result.setText("Cannot load the HYG sample\n" + exception.getMessage());
        }
    }

    /** @return controls that exercise each component independently */
    private VBox createExamples() {
        final Button listExample = new Button("Show HYG list");
        listExample.setOnAction(event -> starList.showStars(catalog.getStars()));

        final Button searchExample = new Button("Find Sirius in store");
        searchExample.setOnAction(event -> findExampleStar());

        final Button complete = new Button("Complete star");
        complete.setOnAction(event -> details.showStar(examples.getCompleteStar(), examples.getAbundances()));

        final Button missing = new Button("Missing fields");
        missing.setOnAction(event -> details.showStar(examples.getMissingStar(), List.of()));

        final Button noComposition = new Button("No composition");
        noComposition.setOnAction(event -> details.showStar(examples.getCompleteStar(), List.of()));

        final Button clear = new Button("Clear selection");
        clear.setOnAction(event -> details.showStar(null, List.of()));

        final Button positions = new Button("Find Moon at 0006 UTC");
        positions.setOnAction(event -> findExamplePosition(false));

        final Button empty = new Button("Empty position store");
        empty.setOnAction(event -> findExamplePosition(true));

        result.setEditable(false);
        result.setPrefRowCount(3);

        return new VBox(SPACING, new Label("Independent component examples"),
                new HBox(SPACING, listExample, searchExample, positions, empty),
                new HBox(SPACING, complete, missing, noComposition, clear), result);
    }

    /** Reports a list selection without fetching composition. */
    void selectStar(Star star) {
        details.showStar(star, List.of());

        result.setText("Selection cleared");

        if (star != null) {
            result.setText("Selected " + star.getDisplayName());
        }
    }

    /** Applies a filter after both component tasks are complete. */
    void filterStars(String query, double limit) {
        result.setText("Filter requested\nName: " + query + "\nMaximum magnitude: " + limit);

        try {
            starList.showStars(catalog.findStars(query, limit));
        } catch (UnsupportedOperationException exception) {
            result.appendText("\n" + exception.getMessage());
        }
    }

    /** Exercises the catalog task without the list component. */
    private void findExampleStar() {
        try {
            final StringBuilder names = new StringBuilder("Store search results\n");

            for (Star star : catalog.findStars(" siR ", MAGNITUDE_LIMIT)) {
                names.append(star.getDisplayName()).append('\n');
            }

            result.setText(names.toString());
        } catch (UnsupportedOperationException exception) {
            result.setText(exception.getMessage());
        }
    }

    /** Exercises the position task without any other component. */
    private void findExamplePosition(boolean empty) {
        List<PositionSample> samples = examples.getPositions();

        if (empty) {
            samples = List.of();
        }

        try {
            final PositionSample sample = new PositionStore(samples).findNearest(
                    "301", Instant.parse("2026-10-01T00:06:00Z"));

            result.setText("No stored sample");

            if (sample != null) {
                result.setText("Stored sample UTC\n" + sample.getTime());
            }
        } catch (UnsupportedOperationException exception) {
            result.setText(exception.getMessage());
        }
    }
}
