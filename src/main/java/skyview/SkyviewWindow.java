package skyview;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/** Connects real catalogs and live source requests to the Skyview window */
public class SkyviewWindow extends Application {
    private static final int WIDTH = 1300;
    private static final int HEIGHT = 820;
    private static final int PANEL_WIDTH = 330;
    private static final int SPACING = 12;
    private static final int BACKGROUND_THREADS = 3;
    private static final int CLOUD_IMAGE_SIZE = 256;
    private static final int STATUS_ROWS = 3;
    private static final int INFO_ROWS = 12;
    private static final int POSITION_ROWS = 16;
    private static final Duration POSITION_WINDOW = Duration.ofMinutes(15);
    private static final Duration POSITION_INTERVAL = Duration.ofMinutes(5);
    private static final ObserverLocation OBSERVER = new ObserverLocation(37.14, -80.55, 600.0);
    private static final Path HYG_FILE = Path.of("data/hygdata_v41.csv");
    private static final Path NGC_FILE = Path.of("data/openngc.csv");
    private static final Path NGC_ADDENDUM = Path.of("data/openngc-addendum.csv");

    private final ExecutorService background = Executors.newFixedThreadPool(BACKGROUND_THREADS);
    private final ObjectDetailsPane details = new ObjectDetailsPane();
    private final StarListPane starList = new StarListPane(this);
    private final ListView<DeepSkyObject> deepSkyList = new ListView<>();
    private final TextField deepSkySearch = new TextField();
    private final TextField element = new TextField("Ca");
    private final TextArea result = new TextArea();
    private final TextArea objectInfo = new TextArea();
    private final TextArea positionsText = new TextArea();
    private final Label starStatus = new Label("Loading HYG catalog");
    private final Label deepSkyStatus = new Label("Loading OpenNGC catalogs");
    private final Label positionStatus = new Label();
    private final Label cloudStatus = new Label();
    private final Label cloudFrame = new Label("No cloud image received");
    private final ImageView cloudImage = new ImageView();

    private StarStore catalog = new StarStore(List.of());
    private List<DeepSkyObject> deepSkyObjects = List.of();
    private PositionStore positionStore = new PositionStore(List.of());
    private Star selectedStar;
    private Task<List<ElementAbundance>> compositionTask;
    private Task<List<PositionSample>> positionTask;
    private Task<CloudTile> cloudTask;

    /**
     * Opens the window and starts real source retrieval
     * @param stage application window
     */
    @Override
    public void start(Stage stage) {
        stage.setTitle("Skyview");
        stage.setScene(new Scene(createLayout(), WIDTH, HEIGHT));
        stage.show();

        loadStars();
        loadDeepSky();
        refreshPositions();
        refreshClouds();
    }

    // creates the main source and object panels
    private BorderPane createLayout() {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(SPACING));
        root.setTop(new Label("Skyview | Radford | Latitude 37.14 | Longitude -80.55 | Elevation 600 m"));

        TabPane browser = new TabPane(new Tab("Stars", new VBox(SPACING, starStatus, starList)),
                new Tab("Deep sky", createDeepSkyBrowser()));
        browser.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        browser.setPrefWidth(PANEL_WIDTH);
        VBox.setVgrow(starList, Priority.ALWAYS);
        root.setLeft(browser);

        root.setCenter(createSourcePanels());
        root.setRight(createObjectPanel());

        result.setEditable(false);
        result.setPrefRowCount(STATUS_ROWS);
        root.setBottom(result);

        return root;
    }

    // creates the deep sky search and selection controls
    private VBox createDeepSkyBrowser() {
        deepSkySearch.setPromptText("Name, Messier, NGC, or IC identifier");
        deepSkySearch.setOnAction(event -> filterDeepSky());
        deepSkyList.setCellFactory(view -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(DeepSkyObject object, boolean empty) {
                super.updateItem(object, empty);
                setText(null);

                if (!empty && object != null) {
                    setText(object.getDisplayName());
                }
            }
        });
        deepSkyList.getSelectionModel().selectedItemProperty().addListener(
                (property, previous, selected) -> selectDeepSky(selected));
        deepSkyList.setPlaceholder(new Label("No deep sky objects to show"));

        Button search = new Button("Search deep sky");
        search.setOnAction(event -> filterDeepSky());
        VBox panel = new VBox(SPACING, deepSkyStatus, deepSkySearch, search, deepSkyList);
        VBox.setVgrow(deepSkyList, Priority.ALWAYS);

        return panel;
    }

    // creates the live position and geographic cloud displays
    private VBox createSourcePanels() {
        Button refreshPositions = new Button("Refresh solar system");
        refreshPositions.setOnAction(event -> refreshPositions());
        positionsText.setEditable(false);
        positionsText.setPrefRowCount(POSITION_ROWS);

        Button refreshClouds = new Button("Refresh clouds");
        refreshClouds.setOnAction(event -> refreshClouds());
        cloudImage.setFitWidth(CLOUD_IMAGE_SIZE);
        cloudImage.setFitHeight(CLOUD_IMAGE_SIZE);
        cloudImage.setPreserveRatio(true);
        cloudFrame.setWrapText(true);
        cloudStatus.setWrapText(true);

        VBox panel = new VBox(SPACING, new Label("Sky rendering is not implemented yet"),
                refreshPositions, positionStatus, positionsText,
                new HBox(SPACING, refreshClouds, new Label("Geographic cloud coverage")),
                cloudStatus, cloudFrame, cloudImage);
        panel.setPadding(new Insets(SPACING));
        VBox.setVgrow(positionsText, Priority.ALWAYS);

        return panel;
    }

    // creates the selected object and composition controls
    private VBox createObjectPanel() {
        Button composition = new Button("Refresh composition");
        composition.setOnAction(event -> requestComposition());
        element.setOnAction(event -> requestComposition());
        Label elementLabel = new Label("Chemical element for stellar composition");
        elementLabel.setLabelFor(element);

        objectInfo.setEditable(false);
        objectInfo.setWrapText(true);
        objectInfo.setPrefRowCount(INFO_ROWS);
        VBox panel = new VBox(SPACING, new Label("Selected object"), details, objectInfo,
                elementLabel, element, composition);
        panel.setPrefWidth(PANEL_WIDTH);
        VBox.setVgrow(objectInfo, Priority.ALWAYS);

        return panel;
    }

    // loads all usable stars without imposing a naked eye brightness limit
    private void loadStars() {
        Task<List<Star>> task = new Task<>() {
            @Override
            protected List<Star> call() throws IOException {
                return new HygCatalog().loadStars(HYG_FILE, Double.MAX_VALUE);
            }

            @Override
            protected void succeeded() {
                catalog = new StarStore(getValue());
                starList.showStars(catalog.getStars());
                starStatus.setText("HYG v4.1 | " + getValue().size() + " stars");
            }

            @Override
            protected void failed() {
                starStatus.setText("HYG unavailable | " + getException().getMessage());
            }
        };

        background.execute(task);
    }

    // loads both complete deep sky files
    private void loadDeepSky() {
        Task<List<DeepSkyObject>> task = new Task<>() {
            @Override
            protected List<DeepSkyObject> call() throws IOException {
                OpenNgcCatalog reader = new OpenNgcCatalog();
                List<DeepSkyObject> objects = reader.loadObjects(NGC_FILE);
                objects.addAll(reader.loadObjects(NGC_ADDENDUM));
                return objects;
            }

            @Override
            protected void succeeded() {
                deepSkyObjects = getValue();
                filterDeepSky();
            }

            @Override
            protected void failed() {
                deepSkyStatus.setText("OpenNGC unavailable | " + getException().getMessage());
            }
        };

        background.execute(task);
    }

    // searches real catalog identifiers and common names
    private void filterDeepSky() {
        String query = deepSkySearch.getText().strip().replace(" ", "").toLowerCase(Locale.ROOT);
        List<DeepSkyObject> matches = new ArrayList<>();

        for (DeepSkyObject object : deepSkyObjects) {
            String names = object.getSearchNames().replace(" ", "").toLowerCase(Locale.ROOT);
            if (names.contains(query)) {
                matches.add(object);
            }
        }

        if (deepSkyList.getSelectionModel().getSelectedItem() != null) {
            objectInfo.clear();
        }

        deepSkyList.getSelectionModel().clearSelection();
        deepSkyList.getItems().setAll(matches);
        deepSkyStatus.setText("OpenNGC | " + matches.size() + " of " + deepSkyObjects.size() + " objects");
    }

    // displays catalog measurements for the selected deep sky object
    private void selectDeepSky(DeepSkyObject object) {
        if (object == null) {
            return;
        }

        starList.stars.getSelectionModel().clearSelection();
        selectedStar = null;
        details.setVisible(false);
        details.setManaged(false);

        objectInfo.setText(object.getDisplayName() + "\nType " + object.objectType
                + "\nMessier " + Objects.toString(object.messierName, "unknown")
                + "\nOther names " + Objects.toString(object.commonNames, "unknown")
                + "\nJ2000 right ascension " + object.rightAscensionHours + " h"
                + "\nJ2000 declination " + object.declinationDegrees + " deg"
                + "\nVisual magnitude " + Objects.toString(object.visualMagnitude, "unknown")
                + "\nMajor diameter " + Objects.toString(object.majorAxisArcminutes, "unknown") + " arcmin"
                + "\nMinor diameter " + Objects.toString(object.minorAxisArcminutes, "unknown") + " arcmin"
                + "\nGalaxy B surface brightness " + Objects.toString(object.surfaceBrightness, "unknown")
                + " mag per square arcsecond");
    }

    /**
     * Selects a real catalog star and starts its composition request
     * @param star selected star, or null when selection clears
     */
    void selectStar(Star star) {
        selectedStar = star;
        details.setVisible(true);
        details.setManaged(true);
        details.showStar(star, List.of());
        objectInfo.clear();

        if (star != null) {
            deepSkyList.getSelectionModel().clearSelection();
            objectInfo.setText(star.getDisplayName() + "\nJ2000 right ascension "
                    + star.getRightAscensionHours() + " h\nJ2000 declination "
                    + star.getDeclinationDegrees() + " deg\nVisual magnitude " + star.getApparentMagnitude());
        }

        requestComposition();
    }

    /**
     * Applies the supplied search to the full retained star catalog
     * @param query name text
     * @param limit finite magnitude limit, or the largest finite double to include all stars
     */
    void filterStars(String query, double limit) {
        List<Star> matches = catalog.findStars(query, limit);
        starList.showStars(matches);

        result.appendText("Star search returned " + matches.size() + " objects\n");
    }

    // requests the selected element for the actual selected star
    private void requestComposition() {
        if (compositionTask != null) {
            compositionTask.cancel();
        }

        compositionTask = null;

        Star star = selectedStar;
        if (star == null) {
            return;
        }

        String identifier = null;
        if (star.getHipId() != null) {
            identifier = "HIP" + star.getHipId();
        } else if (star.getHdId() != null) {
            identifier = "HD" + star.getHdId();
        }

        if (identifier == null || element.getText().isBlank()) {
            result.appendText("Composition needs a HIP or HD identifier and a chemical element\n");
            return;
        }

        startCompositionRequest(star, identifier, element.getText().strip());
    }

    // keeps composition requests off the application thread
    private void startCompositionRequest(Star star, String identifier, String chemicalElement) {
        result.appendText("Requesting " + chemicalElement + " abundances for " + star.getDisplayName() + "\n");
        compositionTask = new Task<>() {
            @Override
            protected List<ElementAbundance> call() throws IOException, InterruptedException {
                return new HypatiaClient().fetchAbundances(identifier, chemicalElement);
            }

            @Override
            protected void succeeded() {
                if (compositionTask == this) {
                    displayComposition(star, getValue());
                }
            }

            @Override
            protected void failed() {
                if (compositionTask == this) {
                    objectInfo.appendText("\nComposition unavailable\n" + getException().getMessage());
                }
            }
        };

        background.execute(compositionTask);
    }

    // passes live abundances to the details component and shows the returned measurements
    private void displayComposition(Star star, List<ElementAbundance> abundances) {
        details.showStar(star, abundances);
        objectInfo.appendText("\nComposition retrieved at " + Instant.now() + " UTC");

        if (abundances.isEmpty()) {
            objectInfo.appendText("\nNo composition measurements");
        }

        for (ElementAbundance abundance : abundances) {
            String value = "unknown";
            if (abundance.getMedianAbundanceDex() != null) {
                value = abundance.getMedianAbundanceDex() + " dex";
            }

            objectInfo.appendText("\n" + abundance.getElement() + " " + value
                    + " | Solar reference " + abundance.getSolarNormalization());
        }
    }

    // starts one sequential batch of solar system requests
    private void refreshPositions() {
        if (positionTask != null && !positionTask.isDone()) {
            return;
        }

        Instant start = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        positionStatus.setText("Requesting current positions from Horizons");
        positionTask = new Task<>() {
            @Override
            protected List<PositionSample> call() throws IOException, InterruptedException {
                return retrievePositions(start);
            }

            @Override
            protected void succeeded() {
                positionStore = new PositionStore(getValue());
                displayPositions(start);
                positionStatus.setText("Horizons | Requested at " + start + " UTC");
            }

            @Override
            protected void failed() {
                positionStatus.setText("Horizons unavailable | " + getException().getMessage());
            }
        };

        background.execute(positionTask);
    }

    // fetches each target once and retains successful results from other targets
    private List<PositionSample> retrievePositions(Instant start) throws IOException, InterruptedException {
        HorizonsClient reader = new HorizonsClient();
        List<PositionSample> positions = new ArrayList<>();

        for (SolarSystemBody body : SolarSystemBody.values()) {
            try {
                positions.addAll(reader.fetchPositions(body.bodyId, OBSERVER, start,
                        start.plus(POSITION_WINDOW), POSITION_INTERVAL));
            } catch (IOException exception) {
                Platform.runLater(() -> result.appendText(body.displayName + " unavailable | "
                        + exception.getMessage() + "\n"));
            }
        }

        if (positions.isEmpty()) {
            throw new IOException("No solar system positions were returned");
        }

        return positions;
    }

    // displays the actual stored time and direction for each target
    private void displayPositions(Instant time) {
        positionsText.clear();

        for (SolarSystemBody body : SolarSystemBody.values()) {
            PositionSample sample = positionStore.findNearest(body.bodyId, time);
            if (sample == null) {
                positionsText.appendText(body.displayName + " unavailable\n\n");
            } else {
                positionsText.appendText(body.displayName + " | " + sample.getTime() + " UTC\n");
                positionsText.appendText(String.format(Locale.ROOT, "Azimuth %.2f deg | Altitude %.2f deg%n",
                        sample.getAzimuthDegrees(), sample.getAltitudeDegrees()));
                positionsText.appendText("Magnitude " + Objects.toString(sample.getApparentMagnitude(), "unknown")
                        + " | Illuminated percent " + Objects.toString(sample.getIlluminatedPercent(), "unknown") + "\n\n");
            }
        }
    }

    // requests an observed cloud image without using a saved example as fallback
    private void refreshClouds() {
        if (cloudTask != null && !cloudTask.isDone()) {
            return;
        }

        cloudStatus.setText("Requesting the latest available cloud frame");
        cloudTask = new Task<>() {
            @Override
            protected CloudTile call() throws IOException, InterruptedException {
                return new RealEarthClient().fetchLatestCloudMask();
            }

            @Override
            protected void succeeded() {
                CloudTile tile = getValue();
                cloudImage.setImage(SwingFXUtils.toFXImage(tile.getImage(), null));
                cloudFrame.setText("Frame " + tile.getObservationTime() + " UTC | Age "
                        + Duration.between(tile.getObservationTime(), Instant.now()).toMinutes() + " min");
                cloudStatus.setText("Source SSEC RealEarth, UW Madison");
            }

            @Override
            protected void failed() {
                cloudStatus.setText("Cloud refresh failed | " + getException().getMessage());
            }
        };

        background.execute(cloudTask);
    }

    /** Stops background requests when the window closes */
    @Override
    public void stop() {
        background.shutdownNow();
    }
}
