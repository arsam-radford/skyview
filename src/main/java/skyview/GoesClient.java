package skyview;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.skyview.goes.CloudMask;
import org.skyview.goes.CloudTopHeight;
import org.skyview.goes.GoesCloudMaskParser;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Finds and downloads NOAA's latest available GOES-19 CONUS cloud products.
 * Sources: <a href="https://noaa-goes19.s3.amazonaws.com/">NOAA public bucket</a>,
 * <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_ListObjectsV2.html">S3 listings</a>,
 * and <a href="https://docs.oracle.com/en/java/javase/17/docs/api/java.net.http/java/net/http/HttpClient.html">JDK HttpClient</a>
 */
public class GoesClient {
    private static final URI NOAA_BUCKET = URI.create("https://noaa-goes19.s3.amazonaws.com/");
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(15);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(60);
    private static final int SUCCESS_STATUS = 200;
    private static final int SEARCH_HOURS = 3;
    private static final DateTimeFormatter HOUR_PATH = DateTimeFormatter.ofPattern("uuuu/DDD/HH").withZone(ZoneOffset.UTC);
    private static final String MASK_PRODUCT = "ABI-L2-ACMC";
    private static final String HEIGHT_PRODUCT = "ABI-L2-ACHAC";
    private static final String FILE_SUFFIX_PATTERN = "-M[36]_G19_s[0-9]{14}_e[0-9]{14}_c[0-9]{14}\\.nc";

    private final HttpClient httpClient;
    private final URI bucket;
    private final Clock clock;
    private final GoesCloudMaskParser parser = new GoesCloudMaskParser();

    public GoesClient() {
        this(HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build(), NOAA_BUCKET, Clock.systemUTC());
    }

    GoesClient(HttpClient httpClient, URI bucket, Clock clock) {
        this.httpClient = httpClient;
        this.bucket = bucket;
        this.clock = clock;
    }

    /**
     * Retrieves current external data on each call, searches this UTC hour and two hours before
     * Uses the newest scan start in the first nonempty hour, then its newest creation timestamp
     * @return a decoded mask with the actual observation timestamps
     * @throws IOException if discovery, download, or parsing fails. no file in the search window is also a failure
     * @throws InterruptedException if acquisition is interrupted
     */
    public CloudMask fetchLatestCloudMask() throws IOException, InterruptedException {
        try {
            return downloadMask(findLatestKey(MASK_PRODUCT));
        } catch (IOException exception) {
            throw new IOException("GOES acquisition: " + exception.getMessage(), exception);
        }
    }

    /**
     * Retrieves the newest available cloud-top height grid independently of the cloud mask.
     * @return geopotential heights with their own grid, quality flags, and scan times
     * @throws IOException if discovery, download, or parsing fails
     * @throws InterruptedException if acquisition is interrupted
     */
    public CloudTopHeight fetchLatestCloudTopHeight() throws IOException, InterruptedException {
        try {
            Path file = downloadFile(findLatestKey(HEIGHT_PRODUCT));
            try {
                return parser.parseCloudTopHeight(file);
            } finally {
                Files.deleteIfExists(file);
            }
        } catch (IOException exception) {
            throw new IOException("GOES height acquisition: " + exception.getMessage(), exception);
        }
    }

    /** checks the previous hour/day when the current hour has no published scan */
    private String findLatestKey(String product) throws IOException, InterruptedException {
        Instant hour = Instant.now(clock).truncatedTo(ChronoUnit.HOURS);
        String key = null;
        int offset = 0;

        while (key == null && offset < SEARCH_HOURS) {
            String prefix = product + "/" + HOUR_PATH.format(hour.minus(offset, ChronoUnit.HOURS)) + "/";
            key = latestKeyInHour(prefix, product);
            offset++;
        }

        if (key == null) {
            throw new IOException("No GOES-19 " + product + " in the last " + SEARCH_HOURS + " UTC hours.");
        }

        return key;
    }

    /** retrieves one hour listing, a normal CONUS hour contains only about twelve scans */
    private String latestKeyInHour(String prefix, String product) throws IOException, InterruptedException {
        String encodedPrefix = URLEncoder.encode(prefix, StandardCharsets.UTF_8);
        URI uri = URI.create(bucket + "?list-type=2&prefix=" + encodedPrefix);

        var response = httpClient.send(request(uri), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        if (response.statusCode() != SUCCESS_STATUS) {
            throw new IOException("HTTP " + response.statusCode() + " from " + uri);
        }

        Document listing = parseListing(response.body());
        return selectLatestKey(listing, prefix, product);
    }

    private String selectLatestKey(Document listing, String prefix, String product) throws IOException {
        var truncation = listing.getElementsByTagNameNS("*", "IsTruncated");

        if (!"ListBucketResult".equals(listing.getDocumentElement().getLocalName())
                || truncation.getLength() != 1 || !truncation.item(0).getTextContent().trim().equals("false")) {
            throw new IOException("Malformed or truncated GOES bucket listing for " + prefix);
        }

        var keys = listing.getElementsByTagNameNS("*", "Key");
        String latest = null;

        for (int index = 0; index < keys.getLength(); index++) {
            String key = keys.item(index).getTextContent();

            if (key.startsWith(prefix) && key.substring(prefix.length()).matches("OR_" + product + FILE_SUFFIX_PATTERN)
                    && (latest == null || key.compareTo(latest) > 0)) {
                latest = key;
            }
        }

        return latest;
    }

    /** Parses a network XML response with external entities and DTDs disabled */
    private Document parseListing(String xml) throws IOException {
        try {
            var factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");

            try (var reader = new StringReader(xml)) {
                return factory.newDocumentBuilder().parse(new InputSource(reader));
            }
        } catch (ParserConfigurationException | SAXException | IllegalArgumentException exception) {
            throw new IOException("Invalid GOES bucket XML listing.", exception);
        }
    }

    /** Uses a temporary file since the parser needs random access; removes it after decoding */
    private CloudMask downloadMask(String key) throws IOException, InterruptedException {
        Path file = downloadFile(key);

        try {
            return parser.parse(file);
        } catch (IOException exception) {
            throw new IOException("Decode " + key + ": " + exception.getMessage(), exception);
        } finally {
            Files.deleteIfExists(file);
        }
    }

    /** Removes partial downloads on failure; the caller removes a completed file after parsing. */
    private Path downloadFile(String key) throws IOException, InterruptedException {
        URI uri = bucket.resolve(key);
        Path file = Files.createTempFile("skyview-goes-", ".nc");

        try {
            var response = httpClient.send(request(uri), HttpResponse.BodyHandlers.ofFile(file));

            if (response.statusCode() != SUCCESS_STATUS) {
                throw new IOException("HTTP " + response.statusCode() + " from " + uri);
            }

            return file;
        } catch (IOException | InterruptedException exception) {
            Files.deleteIfExists(file);
            throw exception;
        }
    }

    /** Applies the same timeout to listings and file downloads. */
    private HttpRequest request(URI uri) {
        return HttpRequest.newBuilder(uri).timeout(REQUEST_TIMEOUT).GET().build();
    }
}
