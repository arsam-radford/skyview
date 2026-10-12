package skyview;

import com.google.gson.JsonParser;
import com.google.gson.JsonParseException;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import javax.imageio.ImageIO;

public class RealEarthClient {
    private static final URI API_ROOT = URI.create("https://clouds.ssec.wisc.edu/api/");
    private static final String PRODUCT = "G18-ACM";
    private static final int RADFORD_TILE_COLUMN = 35;
    private static final int RADFORD_TILE_ROW = 49;
    private static final int TILE_ZOOM = 7;
    private static final int SUCCESS_STATUS = 200;
    private static final Duration CONNECTION_TIMEOUT = Duration.ofSeconds(15);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);
    private static final DateTimeFormatter FRAME_TIME = DateTimeFormatter.ofPattern("uuuuMMdd.HHmmss")
            .withResolverStyle(ResolverStyle.STRICT);

    private final HttpClient httpClient;
    private final URI apiRoot;

    public RealEarthClient() {
        this(HttpClient.newBuilder().connectTimeout(CONNECTION_TIMEOUT).build(), API_ROOT);
    }

    RealEarthClient(HttpClient httpClient, URI apiRoot) {
        this.httpClient = httpClient;
        this.apiRoot = apiRoot;
    }

    /**
     * Pins the PNG request to the latest frame so its time matches the image
     * @return the Radford geographic tile and its observation time
     * @throws IOException if metadata, downloading, or PNG decoding fails
     * @throws InterruptedException if an HTTP request is interrupted
     */
    public CloudTile fetchLatestCloudMask() throws IOException, InterruptedException {
        Instant observationTime = fetchLatestFrame();
        String frame = FRAME_TIME.format(observationTime.atOffset(ZoneOffset.UTC)).replace('.', '_');
        URI uri = apiRoot.resolve("image?products=" + PRODUCT + "_" + frame
                + "&x=" + RADFORD_TILE_COLUMN + "&y=" + RADFORD_TILE_ROW + "&z=" + TILE_ZOOM);

        return new CloudTile(observationTime, uri, downloadImage(uri),
                RADFORD_TILE_COLUMN, RADFORD_TILE_ROW, TILE_ZOOM);
    }

    /** Reads the products timestamp from the latest json object. */
    private Instant fetchLatestFrame() throws IOException, InterruptedException {
        URI uri = apiRoot.resolve("latest?products=" + PRODUCT);
        var response = httpClient.send(HttpRequest.newBuilder(uri).timeout(REQUEST_TIMEOUT).GET().build(),
                HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != SUCCESS_STATUS) {
            throw new IOException("RealEarth metadata HTTP " + response.statusCode());
        }

        try {
            var root = JsonParser.parseString(response.body());

            if (!root.isJsonObject()) {
                throw new IOException("RealEarth latest response must be a JSON object");
            }

            var frame = root.getAsJsonObject().get(PRODUCT);

            if (frame == null || !frame.isJsonPrimitive() || !frame.getAsJsonPrimitive().isString()) {
                throw new IOException("RealEarth latest response is missing the cloud frame time");
            }

            return LocalDateTime.parse(frame.getAsString(), FRAME_TIME).toInstant(ZoneOffset.UTC);
        } catch (JsonParseException | DateTimeException exception) {
            throw new IOException("RealEarth latest response has invalid JSON or frame time", exception);
        }
    }

    /** Decodes a PNG with the JDK image reader */
    private BufferedImage downloadImage(URI uri) throws IOException, InterruptedException {
        var response = httpClient.send(HttpRequest.newBuilder(uri).timeout(REQUEST_TIMEOUT).GET().build(),
                HttpResponse.BodyHandlers.ofByteArray());

        if (response.statusCode() != SUCCESS_STATUS) {
            throw new IOException("RealEarth image HTTP " + response.statusCode());
        }

        try (var input = new ByteArrayInputStream(response.body())) {
            BufferedImage image = ImageIO.read(input);

            if (image == null) {
                throw new IOException("RealEarth returned an unreadable PNG");
            }

            return image;
        }
    }

}
