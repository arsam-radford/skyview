package skyview;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.List;
import javax.imageio.ImageIO;

/** Coordinates the independent source demonstrations */
public class SkyviewApplication {
    private static final int OUTPUT_OBJECT_LIMIT = 10;
    private static final Duration SAMPLE_INTERVAL = Duration.ofMinutes(5);
    private static final Duration SAMPLE_WINDOW = Duration.ofMinutes(15);
    private static final String COMPOSITION_STAR = "HIP32970";
    private static final String COMPOSITION_ELEMENT = "ca";


    // approximate demo location
    private static final ObserverLocation DEMO_OBSERVER = new ObserverLocation(37.14, -80.55, 600.0);

    private static final Path CATALOG_FILE = Path.of("data/hygdata_v41.csv");
    private static final Path OPEN_NGC_FILE = Path.of("data/openngc.csv");
    private static final Path OPEN_NGC_ADDENDUM = Path.of("data/openngc-addendum.csv");
    private final HygCatalog starCatalog = new HygCatalog();
    private final HorizonsClient horizonsClient = new HorizonsClient();
    private final HypatiaClient hypatiaClient = new HypatiaClient();
    private static final Path CLOUD_IMAGE_FILE = Path.of("build/clouds/radford.png");
    private final RealEarthClient cloudClient = new RealEarthClient();

    /**
     * Retrieves each source independently for the terminal demonstration
     *
     * @throws InterruptedException if an enabled request is interrupted
     */
    public void retrieveData() throws InterruptedException {
        System.out.printf("%nRetrieval started: %s (UTC)%n", Instant.now());
        System.out.printf("Radford observer: %.2f deg latitude, %.2f deg longitude, %.0f m elevation%n",
                DEMO_OBSERVER.getLatitudeDegrees(), DEMO_OBSERVER.getLongitudeDegrees(), DEMO_OBSERVER.getElevationMeters());

        retrieveStars();
        retrieveDeepSky();
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
        } catch (IOException exception) {
            System.out.println("RealEarth: " + exception.getMessage());
        }
    }

    // displays the stars returned by the local catalog reader
    private void retrieveStars() {
        System.out.println("\nHYG: reading the full catalog");

        try {
            var stars = starCatalog.loadStars(CATALOG_FILE, Double.MAX_VALUE);

            int displayed = Math.min(OUTPUT_OBJECT_LIMIT, stars.size());
            System.out.printf("Loaded %d stars | Showing the first %d records | J2000 coordinates%n",
                    stars.size(), displayed);

            for (int index = 0; index < displayed; index++) {
                Star star = stars.get(index);
                System.out.printf("%s: RA %s h, Dec %s deg, magnitude %s%n",
                        star.getDisplayName(), star.getRightAscensionHours(),
                        star.getDeclinationDegrees(), star.getApparentMagnitude());

                System.out.printf("  HYG %s | HIP %s | HD %s | spectral type %s | color index %s%n",
                        star.getHygId(), Objects.toString(star.getHipId(), "unknown"),
                        Objects.toString(star.getHdId(), "unknown"), Objects.toString(star.getSpectralType(), "unknown"),
                        Objects.toString(star.getColorIndex(), "unknown"));
            }
        } catch (IOException exception) {
            System.out.println("HYG: " + exception.getMessage());
        }
    }

    // displays a short list from the full deep sky catalogs
    private void retrieveDeepSky() {
        System.out.println("\nOpenNGC: reading the full catalogs");

        try {
            OpenNgcCatalog reader = new OpenNgcCatalog();
            List<DeepSkyObject> objects = reader.loadObjects(OPEN_NGC_FILE);
            objects.addAll(reader.loadObjects(OPEN_NGC_ADDENDUM));
            int displayed = Math.min(OUTPUT_OBJECT_LIMIT, objects.size());
            System.out.printf("Loaded %d deep sky objects | Showing the first %d records%n", objects.size(), displayed);

            for (int index = 0; index < displayed; index++) {
                DeepSkyObject object = objects.get(index);
                System.out.printf("%s | Type %s | RA %s h | Dec %s deg | Visual magnitude %s%n",
                        object.getDisplayName(), object.objectType, object.rightAscensionHours,
                        object.declinationDegrees, Objects.toString(object.visualMagnitude, "unknown"));
            }
        } catch (IOException exception) {
            System.out.println("OpenNGC unavailable | " + exception.getMessage());
        }
    }

    // requests every configured solar system target in sequence
    private void retrieveSolarSystem() throws InterruptedException {
        Instant start = Instant.now();

        for (SolarSystemBody body : SolarSystemBody.values()) {
            retrieveBodyPositions(body, start);
        }
    }

    /**
     * Displays calculated positions for one solar system target
     *
     * @throws InterruptedException if the HTTP request is interrupted
     */
    private void retrieveBodyPositions(SolarSystemBody body, Instant start) throws InterruptedException {
        System.out.printf("%nJPL Horizons: requesting %s positions for the next %d minutes%n",
                body.displayName, SAMPLE_WINDOW.toMinutes());

        try {
            var samples = horizonsClient.fetchPositions(body.bodyId, DEMO_OBSERVER, start, start.plus(SAMPLE_WINDOW), SAMPLE_INTERVAL);

            System.out.printf("Received %d calculated positions | Azimuth: north = 0 deg, east = 90 deg%n", samples.size());

            for (PositionSample sample : samples) {
                String horizon = "above horizon";
                if (sample.getAltitudeDegrees() < 0.0) {
                    horizon = "below horizon";
                }

                System.out.printf("%s at %s (UTC): azimuth %.2f deg, altitude %+.2f deg (%s)%n",
                        body.displayName, sample.getTime(), sample.getAzimuthDegrees(), sample.getAltitudeDegrees(), horizon);

                System.out.printf("  Body ID %s | apparent magnitude %s | illuminated disk (%%) %s%n",
                        sample.getBodyId(), Objects.toString(sample.getApparentMagnitude(), "unknown"),
                        Objects.toString(sample.getIlluminatedPercent(), "unknown"));
            }
        } catch (IOException exception) {
            System.out.println(body.displayName + " unavailable | " + exception.getMessage());
        }
    }

    /**
     * Displays available calcium measurements for one known Hypatia match
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
        } catch (IOException exception) {
            System.out.println("Hypatia: " + exception.getMessage());
        }
    }
}
