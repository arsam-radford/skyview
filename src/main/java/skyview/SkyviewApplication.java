package skyview;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;

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

    /**
     * Calls each enabled source; uncomment your section while implementing it.
     *
     * @throws InterruptedException if an enabled request is interrupted
     */
    public void retrieveData() throws InterruptedException {
        System.out.println("Retrieving enabled sections; enable calls in SkyviewApplication.");
        // retrieveStars();
        retrieveSolarSystem();
        // retrieveComposition();
        // GOES acquisition and decoding will be integrated by Arsam separately
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
        try {
            for (PositionSample sample : horizonsClient.fetchPositions(MOON_ID, DEMO_OBSERVER, start, start.plus(SAMPLE_WINDOW), SAMPLE_INTERVAL)) {


                System.out.printf("%s at %s: azimuth %s deg, altitude %s deg%n",
                        sample.getBodyId(), sample.getTime(),
                        sample.getAzimuthDegrees(), sample.getAltitudeDegrees());

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
