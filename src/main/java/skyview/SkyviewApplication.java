package skyview;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import org.skyview.goes.CloudCell;

/** Coordinates the independent source demonstrations */
public class SkyviewApplication {
    private static final double MAGNITUDE_LIMIT = 6.5;
    private static final String MOON_ID = "301";
    private static final Duration SAMPLE_INTERVAL = Duration.ofMinutes(5);
    private static final Duration SAMPLE_WINDOW = Duration.ofMinutes(15);
    private static final String COMPOSITION_STAR = "HIP32970";
    private static final String COMPOSITION_ELEMENT = "ca";
    
    
    // approximate demo location
    private static final ObserverLocation DEMO_OBSERVER = new ObserverLocation(37.14, -80.55, 600.0);

    // 2 real catalog rows for development, use data/hygdata_v41.csv for the full catalog
    private static final Path CATALOG_FILE = Path.of("data/hyg-sample.csv");
    private final HygCatalog starCatalog = new HygCatalog();
    private final HorizonsClient horizonsClient = new HorizonsClient();
    private final HypatiaClient hypatiaClient = new HypatiaClient();
    private final GoesClient goesClient = new GoesClient();

    /**
     * Calls each enabled source; uncomment your section while implementing it.
     *
     * @throws InterruptedException if an enabled request is interrupted
     */
    public void retrieveData() throws InterruptedException {
        System.out.printf("%nRetrieval started: %s (UTC)%n", Instant.now());
        System.out.printf("Radford observer: %.2f deg latitude, %.2f deg longitude, %.0f m elevation%n",
                DEMO_OBSERVER.getLatitudeDegrees(), DEMO_OBSERVER.getLongitudeDegrees(), DEMO_OBSERVER.getElevationMeters());

        // retrieveStars();
        retrieveSolarSystem();
        // retrieveComposition();
        retrieveCloudMask();
        retrieveCloudTopHeight();
        System.out.println("\nRetrieval finished. Press Enter to fetch again.");
    }

    /**
     * Displays available cloud-top geopotential height near Radford.
     * @throws InterruptedException if acquisition is interrupted
     */
    private void retrieveCloudTopHeight() throws InterruptedException {
        System.out.println("\nNOAA GOES-19: downloading the latest CONUS cloud-top heights...");
        try {
            var heights = goesClient.fetchLatestCloudTopHeight();
            var cell = heights.getCellAt(DEMO_OBSERVER.getLatitudeDegrees(), DEMO_OBSERVER.getLongitudeDegrees());
            System.out.printf("Height scan %s to %s UTC, grid %s x %s%n",
                    heights.getScanStart(), heights.getScanEnd(), heights.getColumnCount(), heights.getRowCount());
            System.out.printf("Scan age: %d min; height DQF: %s%n",
                    Duration.between(heights.getScanEnd(), Instant.now()).toMinutes(), cell.getQualityCode());

            if (cell.getHeightMeters() == null) {
                System.out.println("Cloud-top height near Radford: unavailable (no good-quality retrieval).");
            } else {
                System.out.printf("Cloud-top geopotential height near Radford: %.0f m above sea level.%n", cell.getHeightMeters());
            }
        } catch (IOException | IllegalArgumentException exception) {
            System.out.println("GOES height: " + exception.getMessage());
        }
    }

    /**
     * Displays geographic cloud conditions and actual scan times for the demo location.
     * @throws InterruptedException if acquisition is interrupted
     */
    private void retrieveCloudMask() throws InterruptedException {
        System.out.println("\nNOAA GOES-19: downloading the latest CONUS cloud mask...");
        try {
            var mask = goesClient.fetchLatestCloudMask();
            CloudCell cell = mask.getCellAt(DEMO_OBSERVER.getLatitudeDegrees(), DEMO_OBSERVER.getLongitudeDegrees());
            System.out.printf("GOES-19: scan %s to %s UTC, grid %s x %s%n",
                    mask.getScanStart(), mask.getScanEnd(), mask.getColumnCount(), mask.getRowCount());
            System.out.printf("Scan age: %d min.%n", Duration.between(mask.getScanEnd(), Instant.now()).toMinutes());
            System.out.printf("Radford geographic cloud state: %s; DQF: %s%n", cell.getState(), cell.getQualityCode());
            if (cell.getCloudProbability() == null) {
                System.out.println("Cloud probability: unknown");
            } else {
                System.out.printf("Cloud probability: %.3f (0 to 1)%n", cell.getCloudProbability());
            }
        } catch (IOException | IllegalArgumentException exception) {
            System.out.println("GOES: " + exception.getMessage());
        }
    }

    /** Displays the stars returned by the local catalog reader. */
    private void retrieveStars() {
        try {
            for (Star star : starCatalog.loadStars(CATALOG_FILE, MAGNITUDE_LIMIT)) {
                System.out.printf("%s: RA %s h, Dec %s deg, magnitude %s%n",
                        star.getDisplayName(), star.getRightAscensionHours(),
                        star.getDeclinationDegrees(), star.getApparentMagnitude());
            }
        } catch (IOException | IllegalArgumentException exception) {
            System.out.println("HYG: " + exception.getMessage());
        }
    }

    /**
     * Displays calculated Moon positions for a short current time window.
     *
     * @throws InterruptedException if the HTTP request is interrupted
     */
    private void retrieveSolarSystem() throws InterruptedException {
        Instant start = Instant.now();
        System.out.printf("%nJPL Horizons: requesting Moon positions for the next %d minutes...%n", SAMPLE_WINDOW.toMinutes());
        try {
            var samples = horizonsClient.fetchPositions(MOON_ID, DEMO_OBSERVER, start, start.plus(SAMPLE_WINDOW), SAMPLE_INTERVAL);
            System.out.printf("Received %d calculated positions. Azimuth: north = 0 deg, east = 90 deg.%n", samples.size());

            for (PositionSample sample : samples) {
                String horizon = "above horizon";
                if (sample.getAltitudeDegrees() < 0.0) {
                    horizon = "below horizon";
                }

                System.out.printf("Moon at %s (UTC): azimuth %.2f deg, altitude %+.2f deg (%s)%n",
                        sample.getTime(), sample.getAzimuthDegrees(), sample.getAltitudeDegrees(), horizon);
            }
        } catch (IOException | IllegalArgumentException exception) {
            System.out.println("Horizons: " + exception.getMessage());
        }
    }

    /**
     * Displays available calcium measurements for one known Hypatia match.
     *
     * @throws InterruptedException if the HTTP request is interrupted
     */
    private void retrieveComposition() throws InterruptedException {
        try {
            var measurements = hypatiaClient.fetchAbundances(COMPOSITION_STAR, COMPOSITION_ELEMENT);

            if (measurements.isEmpty()) {
                System.out.println("Hypatia: no matching abundance measurements.");
            }

            for (ElementAbundance measurement : measurements) {
                String value;
                if (measurement.getMedianAbundanceDex() == null) {
                    value = "unknown";
                } else {
                    value = measurement.getMedianAbundanceDex() + " dex";
                }
                System.out.printf("%s: %s %s, solar reference %s%n",
                        measurement.getStarName(), measurement.getElement(),
                        value, measurement.getSolarNormalization());
            }
            
        } catch (IOException | IllegalArgumentException exception) {
            System.out.println("Hypatia: " + exception.getMessage());
        }
    }
}
