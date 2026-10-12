package skyview;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import javax.imageio.ImageIO;

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
    private static final Path CLOUD_IMAGE_FILE = Path.of("build/clouds/radford.png");
    private final RealEarthClient cloudClient = new RealEarthClient();

    /**
     * Retrieves each source independently for the terminal demonstration.
     *
     * @throws InterruptedException if an enabled request is interrupted
     */
    public void retrieveData() throws InterruptedException {
        System.out.printf("%nRetrieval started: %s (UTC)%n", Instant.now());
        System.out.printf("Radford observer: %.2f deg latitude, %.2f deg longitude, %.0f m elevation%n",
                DEMO_OBSERVER.getLatitudeDegrees(), DEMO_OBSERVER.getLongitudeDegrees(), DEMO_OBSERVER.getElevationMeters());

        retrieveStars();
        retrieveSolarSystem();
        retrieveComposition();
        retrieveCloudMask();

        System.out.println("\nRetrieval finished | Press Enter to fetch again");
    }

    /**
     * Saves the latest available geographic cloud texture and shows its actual frame age
     * @throws InterruptedException if acquisition is interrupted
     */
    private void retrieveCloudMask() throws InterruptedException {
        System.out.println("\nSSEC RealEarth: downloading the latest available Radford cloud mask PNG");

        try {
            CloudTile tile = cloudClient.fetchLatestCloudMask();

            Files.createDirectories(CLOUD_IMAGE_FILE.getParent());
            ImageIO.write(tile.getImage(), "png", CLOUD_IMAGE_FILE.toFile());

            System.out.printf("GOES 18 frame: %s UTC | age: %d min%n", tile.getObservationTime(),
                    Duration.between(tile.getObservationTime(), Instant.now()).toMinutes());
            System.out.printf("Tile column %d | row %d | zoom %d | image %d x %d pixels%n",
                    tile.getColumn(), tile.getRow(), tile.getZoom(), tile.getImage().getWidth(), tile.getImage().getHeight());
            System.out.println("Saved geographic cloud texture in build/clouds");
            System.out.println("Colors classify clear or cloudy areas | cloud height is unavailable");
        } catch (IOException | IllegalArgumentException exception) {
            System.out.println("RealEarth: " + exception.getMessage());
        }
    }

    /** Displays the stars returned by the local catalog reader. */
    private void retrieveStars() {
        System.out.println("\nHYG: reading the local development CSV");

        try {
            var stars = starCatalog.loadStars(CATALOG_FILE, MAGNITUDE_LIMIT);

            System.out.printf("Loaded %d stars with apparent magnitude <= %s (J2000 coordinates)%n",
                    stars.size(), MAGNITUDE_LIMIT);

            for (Star star : stars) {
                System.out.printf("%s: RA %s h, Dec %s deg, magnitude %s%n",
                        star.getDisplayName(), star.getRightAscensionHours(),
                        star.getDeclinationDegrees(), star.getApparentMagnitude());

                System.out.printf("  HYG %s | HIP %s | HD %s | spectral type %s | color index %s%n",
                        star.getHygId(), Objects.toString(star.getHipId(), "unknown"),
                        Objects.toString(star.getHdId(), "unknown"), Objects.toString(star.getSpectralType(), "unknown"),
                        Objects.toString(star.getColorIndex(), "unknown"));
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

        System.out.printf("%nJPL Horizons: requesting Moon positions for the next %d minutes%n", SAMPLE_WINDOW.toMinutes());

        try {
            var samples = horizonsClient.fetchPositions(MOON_ID, DEMO_OBSERVER, start, start.plus(SAMPLE_WINDOW), SAMPLE_INTERVAL);

            System.out.printf("Received %d calculated positions | Azimuth: north = 0 deg, east = 90 deg%n", samples.size());

            for (PositionSample sample : samples) {
                String horizon = "above horizon";
                if (sample.getAltitudeDegrees() < 0.0) {
                    horizon = "below horizon";
                }

                System.out.printf("Moon at %s (UTC): azimuth %.2f deg, altitude %+.2f deg (%s)%n",
                        sample.getTime(), sample.getAzimuthDegrees(), sample.getAltitudeDegrees(), horizon);

                System.out.printf("  Body ID %s | apparent magnitude %s | illuminated disk (%%) %s%n",
                        sample.getBodyId(), Objects.toString(sample.getApparentMagnitude(), "unknown"),
                        Objects.toString(sample.getIlluminatedPercent(), "unknown"));
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
        System.out.println("\nHypatia: requesting calcium abundances for " + COMPOSITION_STAR);

        try {
            var measurements = hypatiaClient.fetchAbundances(COMPOSITION_STAR, COMPOSITION_ELEMENT);

            System.out.printf("Abundance results received: %d%n", measurements.size());

            if (measurements.isEmpty()) {
                System.out.println("Hypatia: no matching abundance measurements");
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
