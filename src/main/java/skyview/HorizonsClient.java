package skyview;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

/** Retrieves JPL Horizons observer ephemerides using Gson and Commons CSV. */
public class HorizonsClient {
    private static final String HORIZONS_URL =
            "https://ssd.jpl.nasa.gov/api/horizons.api";

    private static final Duration CONNECTION_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

    private static final DateTimeFormatter HORIZONS_TIME_FORMAT =
            DateTimeFormatter.ofPattern("uuuu-MMM-dd HH:mm:ss", Locale.ENGLISH)
                    .withResolverStyle(ResolverStyle.STRICT);

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(CONNECTION_TIMEOUT)
            .build();

    /**
     * Retrieves calculated positions for one solar-system body in a UTC window.
     *
     * @param bodyId JPL target identifier, such as "301" for the Moon
     * @param observer Earth observer coordinates
     * @param start inclusive window start
     * @param end window end, strictly after start
     * @param interval positive whole minute spacing of samples
     * @return non null samples in ascending time order, samples below horizon
     * @throws IOException if acquisition, API processing, or required parsing fails
     * @throws InterruptedException if the HTTP request is interrupted
     * @throws IllegalArgumentException if an argument is null or invalid
     */
    public List<PositionSample> fetchPositions(String bodyId, ObserverLocation observer,
            Instant start, Instant end, Duration interval)
            throws IOException, InterruptedException {

        validateArguments(bodyId, observer, start, end, interval);

        long intervalMinutes = interval.toMinutes();

        URI requestUri;
        try {
            requestUri = buildRequestUri(bodyId, observer, start, end, intervalMinutes);
        } catch (IllegalArgumentException exception) {
            throw new IOException("Could not build Horizons request URL.", exception);
        }

        HttpRequest request;
        try {
            request = HttpRequest.newBuilder()
                    .uri(requestUri)
                    .timeout(REQUEST_TIMEOUT)
                    .header("Accept", "application/json")
                    .GET()
                    .build();
        } catch (IllegalArgumentException exception) {
            throw new IOException("Could not create Horizons HTTP request.", exception);
        }

        HttpResponse<String> response;
        try {
            response = HTTP_CLIENT.send(
                    request,
                    HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new IOException(
                    "Horizons HTTP request failed for body " + bodyId + ".", exception);
        }

        int status = response.statusCode();
        String responseBody = response.body();

        if (status < 200 || status >= 300) {
            String apiError = tryReadApiError(responseBody);

            if (apiError != null) {
                throw new IOException(
                        "Horizons HTTP " + status + " for body " + bodyId
                                + ": " + apiError);
            }

            throw new IOException(
                    "Horizons HTTP request for body " + bodyId
                            + " returned status " + status
                            + " with response: " + summarize(responseBody));
        }

        try {
            return parseJsonResponse(bodyId, responseBody);
        } catch (IOException exception) {
            throw new IOException(
                    "Horizons response parsing failed for body " + bodyId + ": " + exception.getMessage(),
                    exception);
        }
    }

    /**
     * Parses a complete Horizons JSON response.
     *
     * Package-private so the runnable parser check can exercise parsing
     * without making another network request.
     *
     * @param bodyId target identifier supplied by the caller
     * @param json complete Horizons JSON response
     * @return parsed position samples
     * @throws IOException if the JSON, API response, table markers, header,
     *         or required values are malformed
     */
    static List<PositionSample> parseJsonResponse(String bodyId, String json)
            throws IOException {

        if (bodyId == null || bodyId.isBlank()) {
            throw new IllegalArgumentException("bodyId must not be blank.");
        }

        if (json == null || json.isBlank()) {
            throw new IOException("Horizons response body was empty.");
        }

        JsonObject root;

        try {
            JsonElement parsed = JsonParser.parseString(json);

            if (!parsed.isJsonObject()) {
                throw new IOException("Horizons JSON root was not an object.");
            }

            root = parsed.getAsJsonObject();
        } catch (JsonParseException | IllegalStateException exception) {
            throw new IOException("Horizons response was not valid JSON.", exception);
        }

        JsonElement errorElement = root.get("error");

        if (errorElement != null && !errorElement.isJsonNull()) {
            String errorMessage;

            try {
                errorMessage = errorElement.isJsonPrimitive()
                        ? errorElement.getAsString()
                        : errorElement.toString();
            } catch (RuntimeException exception) {
                throw new IOException(
                        "Horizons response contained an unreadable error field.",
                        exception);
            }

            throw new IOException("Horizons API returned an error: " + errorMessage);
        }

        JsonElement resultElement = root.get("result");

        if (resultElement == null || resultElement.isJsonNull()) {
            throw new IOException("Horizons response did not contain a result field.");
        }

        if (!resultElement.isJsonPrimitive()) {
            throw new IOException("Horizons result field was not a string.");
        }

        String result;

        try {
            result = resultElement.getAsString();
        } catch (RuntimeException exception) {
            throw new IOException(
                    "Horizons result field could not be read as text.",
                    exception);
        }

        return parseResult(bodyId, result);
    }

    /**
     * Parses the embedded Horizons table between $$SOE and $$EOE.
     */
    private static List<PositionSample> parseResult(String bodyId, String result)
            throws IOException {

        if (result == null || result.isBlank()) {
            throw new IOException("Horizons result was empty.");
        }

        int soeIndex = result.indexOf("$$SOE");

        if (soeIndex < 0) {
            throw new IOException("Horizons result did not contain $$SOE.");
        }

        int eoeIndex = result.indexOf("$$EOE", soeIndex + "$$SOE".length());

        if (eoeIndex < 0) {
            throw new IOException("Horizons result did not contain $$EOE.");
        }

        if (eoeIndex < soeIndex) {
            throw new IOException("Horizons table markers were out of order.");
        }

        String textBeforeTable = result.substring(0, soeIndex);
        String headerLine = findTableHeader(textBeforeTable);

        String tableRows = result.substring(
                soeIndex + "$$SOE".length(),
                eoeIndex);

        String csvText = headerLine.trim() + System.lineSeparator() + tableRows;

        CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                .setIgnoreEmptyLines(true)
                .setIgnoreSurroundingSpaces(true)
                .build();

        List<PositionSample> samples = new ArrayList<>();

        try (CSVParser parser = csvFormat.parse(new StringReader(csvText))) {
            List<CSVRecord> records = parser.getRecords();

            if (records.isEmpty()) {
                throw new IOException("Horizons table did not contain a header.");
            }

            CSVRecord header = records.get(0);

            int timeColumn = findColumn(
                    header,
                    "Date__(UT)__HR:MN:SS");

            int azimuthColumn = findColumn(
                    header,
                    "Azimuth_(a-app)");

            int altitudeColumn = findColumn(
                    header,
                    "Elevation_(a-app)");

            int magnitudeColumn = findColumn(
                    header,
                    "APmag");

            int illuminationColumn = findColumn(
                    header,
                    "Illu%");

            int requiredColumnCount = Math.max(
                    Math.max(timeColumn, azimuthColumn),
                    Math.max(altitudeColumn,
                            Math.max(magnitudeColumn, illuminationColumn))) + 1;

            for (int rowIndex = 1; rowIndex < records.size(); rowIndex++) {
                CSVRecord record = records.get(rowIndex);

                /*
                 * The Horizons CSV has extra flag columns and a trailing
                 * delimiter. Only use columns identified from the header.
                 */
                if (record.size() < requiredColumnCount) {
                    throw new IOException(
                            "Horizons CSV row " + rowIndex
                                    + " did not contain all required columns.");
                }

                Instant time = parseTime(
                        record.get(timeColumn),
                        rowIndex);

                double azimuth = parseRequiredDouble(
                        record.get(azimuthColumn),
                        "azimuth",
                        rowIndex);

                double altitude = parseRequiredDouble(
                        record.get(altitudeColumn),
                        "altitude",
                        rowIndex);

                if (azimuth < 0.0 || azimuth >= 360.0) {
                    throw new IOException(
                            "Horizons CSV row " + rowIndex
                                    + " contained azimuth outside [0, 360): "
                                    + azimuth);
                }

                if (altitude < -90.0 || altitude > 90.0) {
                    throw new IOException(
                            "Horizons CSV row " + rowIndex
                                    + " contained altitude outside [-90, 90]: "
                                    + altitude);
                }

                Double magnitude = parseOptionalDouble(
                        record.get(magnitudeColumn),
                        "apparent magnitude",
                        rowIndex);

                Double illuminatedPercent = parseOptionalDouble(
                        record.get(illuminationColumn),
                        "illuminated percentage",
                        rowIndex);

                if (illuminatedPercent != null
                        && (illuminatedPercent < 0.0
                        || illuminatedPercent > 100.0)) {
                    throw new IOException(
                            "Horizons CSV row " + rowIndex
                                    + " contained illuminated percentage "
                                    + "outside [0, 100]: "
                                    + illuminatedPercent);
                }

                samples.add(new PositionSample(
                        bodyId,
                        time,
                        azimuth,
                        altitude,
                        magnitude,
                        illuminatedPercent));
            }
        } catch (IOException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new IOException(
                    "Horizons CSV table could not be parsed.",
                    exception);
        }

        samples.sort(Comparator.comparing(PositionSample::getTime));

        return samples;
    }

    /**
     * Finds the actual CSV header line rather than attempting to parse
     * Horizons' descriptive text as CSV.
     */
    private static String findTableHeader(String precedingText)
            throws IOException {

        String[] lines = precedingText.split("\\R", -1);

        String requiredTimeHeader = "Date__(UT)__HR:MN:SS";
        String requiredAzimuthHeader = "Azimuth_(a-app)";
        String requiredAltitudeHeader = "Elevation_(a-app)";

        for (String line : lines) {
            if (line.contains(requiredTimeHeader)
                    && (line.contains(requiredAzimuthHeader) || line.contains("Azi_(a-app)"))
                    && (line.contains(requiredAltitudeHeader) || line.contains("Elev_(a-app)"))
                    && line.contains("APmag")
                    && line.contains("Illu%")) {
                return line;
            }
        }

        throw new IOException("Horizons CSV table header was not found.");
    }

    /**
     * Looks up a column by its actual header label.
     */
    private static int findColumn(CSVRecord header, String expected)
            throws IOException {

        for (int index = 0; index < header.size(); index++) {
            String label = header.get(index).trim();
            // Horizons can return abbreviated position labels.
            boolean matches = expected.equals(label)
                    || (expected.equals("Azimuth_(a-app)") && label.equals("Azi_(a-app)"))
                    || (expected.equals("Elevation_(a-app)") && label.equals("Elev_(a-app)"));

            if (matches) {
                return index;
            }
        }

        throw new IOException(
                "Horizons CSV header did not contain required column: " + expected);
    }

    /**
     * Parses a required UTC timestamp.
     */
    private static Instant parseTime(String text, int rowIndex)
            throws IOException {

        String value = text == null ? "" : text.trim();

        if (value.isEmpty()) {
            throw new IOException(
                    "Horizons CSV row " + rowIndex + " contained a blank time.");
        }

        try {
            return LocalDateTime.parse(value, HORIZONS_TIME_FORMAT)
                    .toInstant(ZoneOffset.UTC);
        } catch (RuntimeException exception) {
            throw new IOException(
                    "Horizons CSV row " + rowIndex
                            + " contained an invalid UTC time: " + value,
                    exception);
        }
    }

    /**
     * Parses a required finite numeric field.
     */
    private static double parseRequiredDouble(
            String text, String field, int rowIndex) throws IOException {

        String value = text == null ? "" : text.trim();

        if (value.isEmpty()) {
            throw new IOException(
                    "Horizons CSV row " + rowIndex
                            + " contained a blank " + field + ".");
        }

        try {
            double parsed = Double.parseDouble(value);

            if (!Double.isFinite(parsed)) {
                throw new IOException(
                        "Horizons CSV row " + rowIndex
                                + " contained non-finite " + field
                                + ": " + value);
            }

            return parsed;
        } catch (NumberFormatException exception) {
            throw new IOException(
                    "Horizons CSV row " + rowIndex
                            + " contained malformed " + field
                            + ": " + value,
                    exception);
        }
    }

    /**
     * Parses an optional numeric field.
     *
     * Horizons uses values such as "n.a." when a quantity is unavailable.
     */
    private static Double parseOptionalDouble(
            String text, String field, int rowIndex) throws IOException {

        String value = text == null ? "" : text.trim();
        String normalized = value.toLowerCase(Locale.ROOT);

        if (normalized.isEmpty()
                || normalized.equals("n.a.")
                || normalized.equals("n/a")
                || normalized.equals("na")) {
            return null;
        }

        try {
            double parsed = Double.parseDouble(value);

            if (!Double.isFinite(parsed)) {
                throw new IOException(
                        "Horizons CSV row " + rowIndex
                                + " contained non-finite " + field
                                + ": " + value);
            }

            return parsed;
        } catch (NumberFormatException exception) {
            throw new IOException(
                    "Horizons CSV row " + rowIndex
                            + " contained malformed " + field
                            + ": " + value,
                    exception);
        }
    }

    /**
     * Validates all public method arguments before any HTTP request occurs.
     */
    private static void validateArguments(
            String bodyId,
            ObserverLocation observer,
            Instant start,
            Instant end,
            Duration interval) {

        if (bodyId == null || bodyId.isBlank()) {
            throw new IllegalArgumentException("bodyId must not be blank.");
        }

        if (observer == null) {
            throw new IllegalArgumentException("observer must not be null.");
        }

        if (start == null) {
            throw new IllegalArgumentException("start must not be null.");
        }

        if (end == null) {
            throw new IllegalArgumentException("end must not be null.");
        }

        if (!end.isAfter(start)) {
            throw new IllegalArgumentException("end must be after start.");
        }

        if (interval == null) {
            throw new IllegalArgumentException("interval must not be null.");
        }

        if (interval.isZero() || interval.isNegative()) {
            throw new IllegalArgumentException(
                    "interval must be positive.");
        }

        long intervalMinutes = interval.toMinutes();

        if (!Duration.ofMinutes(intervalMinutes).equals(interval)) {
            throw new IllegalArgumentException(
                    "interval must be a whole number of minutes.");
        }
    }

    /**
     * Builds the Horizons API URI.
     *
     * Each Horizons value is individually quoted and URL encoded.
     * The URL structure itself remains unencoded.
     */
    private static URI buildRequestUri(
            String bodyId,
            ObserverLocation observer,
            Instant start,
            Instant end,
            long intervalMinutes) {

        double elevationKilometers =
                observer.getElevationMeters() / 1000.0;

        String siteCoord =
                Double.toString(observer.getLongitudeDegrees())
                        + ","
                        + Double.toString(observer.getLatitudeDegrees())
                        + ","
                        + Double.toString(elevationKilometers);

        String startText = formatTime(start);
        String endText = formatTime(end);
        String stepText = intervalMinutes + " min";

        String query = String.join("&",
                "format=json",
                encodeParameter("COMMAND", bodyId),
                encodeParameter("OBJ_DATA", "NO"),
                encodeParameter("MAKE_EPHEM", "YES"),
                encodeParameter("EPHEM_TYPE", "OBSERVER"),
                encodeParameter("CENTER", "coord@399"),
                encodeParameter("COORD_TYPE", "GEODETIC"),
                encodeParameter("SITE_COORD", siteCoord),
                encodeParameter("START_TIME", startText),
                encodeParameter("STOP_TIME", endText),
                encodeParameter("STEP_SIZE", stepText),
                encodeParameter("TIME_TYPE", "UT"),
                encodeParameter("TIME_DIGITS", "SECONDS"),
                encodeParameter("ANG_FORMAT", "DEG"),
                encodeParameter("QUANTITIES", "4,9,10"),
                encodeParameter("CSV_FORMAT", "YES"));

        return URI.create(HORIZONS_URL + "?" + query);
    }

    /**
     * Encodes one Horizons query value after adding the required single quotes.
     */
    private static String encodeParameter(String name, String value) {
        String quoted = "'" + value + "'";
        String encoded = URLEncoder.encode(
                quoted,
                StandardCharsets.UTF_8);

        return name + "=" + encoded;
    }

    /**
     * Formats an Instant as a UTC Horizons calendar timestamp.
     */
    private static String formatTime(Instant instant) {
        return HORIZONS_TIME_FORMAT.format(
                instant.atOffset(ZoneOffset.UTC));
    }

    /**
     * Attempts to extract a Horizons API error from a non-success response.
     */
    private static String tryReadApiError(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }

        try {
            JsonElement parsed = JsonParser.parseString(json);

            if (!parsed.isJsonObject()) {
                return null;
            }

            JsonElement error = parsed.getAsJsonObject().get("error");

            if (error == null || error.isJsonNull()) {
                return null;
            }

            return error.isJsonPrimitive()
                    ? error.getAsString()
                    : error.toString();

        } catch (RuntimeException exception) {
            return null;
        }
    }

    /**
     * Prevents an unusually large HTTP body from being copied into an
     * exception message.
     */
    private static String summarize(String text) {
        if (text == null) {
            return "<null>";
        }

        String normalized = text.replaceAll("\\s+", " ").trim();

        if (normalized.length() <= 300) {
            return normalized;
        }

        return normalized.substring(0, 300) + "...";
    }
}
