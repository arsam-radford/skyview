package skyview;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

/** Runnable assertions for the Horizons JSON/CSV parser. */
public class HorizonsParserCheck {
    /**
     * Runs parser checks without performing network requests.
     *
     * @param args unused command-line arguments
     * @throws Exception if the parser check cannot read the saved response
     */
    public static void main(String[] args) throws Exception {
        String savedJson = Files.readString(
                Path.of("data/horizons-moon.json"),
                StandardCharsets.UTF_8);

        checkSavedResponse(savedJson);
        checkMissingOptionalValues(savedJson);
        checkMalformedMarkers(savedJson);
        checkMalformedRequiredValue(savedJson);
        checkApiError();

        System.out.println("Horizons parser check passed.");
    }

    /**
     * Confirms the supplied saved response produces the expected three samples.
     */
    private static void checkSavedResponse(String json) throws IOException {
        List<PositionSample> samples =
                HorizonsClient.parseJsonResponse("301", json);

        assert samples.size() == 3
                : "Expected 3 samples, got " + samples.size();

        PositionSample first = samples.get(0);

        assert first.getBodyId().equals("301");
        assert first.getTime().equals(
                Instant.parse("2026-10-01T00:00:00Z"));

        assert Math.abs(first.getAzimuthDegrees()
                - 43.418338472) < 1.0e-12;

        assert Math.abs(first.getAltitudeDegrees()
                - (-13.178015592)) < 1.0e-12;

        assert first.getApparentMagnitude() != null;
        assert Math.abs(first.getApparentMagnitude()
                - (-11.283)) < 1.0e-12;

        assert first.getIlluminatedPercent() != null;
        assert Math.abs(first.getIlluminatedPercent()
                - 77.60912) < 1.0e-12;

        assert samples.get(0).getTime()
                .isBefore(samples.get(1).getTime());

        assert samples.get(1).getTime()
                .isBefore(samples.get(2).getTime());
    }

    /**
     * Confirms unavailable optional quantities become null.
     */
    private static void checkMissingOptionalValues(String json)
            throws IOException {

        String result = getResult(json);

        result = result.replace("  -11.282", "  n.a.");
        result = result.replace("   77.56684,", "   n.a.,");

        String modifiedJson = makeResultJson(result);

        List<PositionSample> samples =
                HorizonsClient.parseJsonResponse("301", modifiedJson);

        assert samples.size() == 3;

        assert samples.get(1).getApparentMagnitude() == null;
        assert samples.get(1).getIlluminatedPercent() == null;
    }

    /**
     * Confirms malformed table markers are rejected.
     */
    private static void checkMalformedMarkers(String json)
            throws IOException {

        String result = getResult(json)
                .replace("$$EOE", "");

        expectIOException(
                () -> HorizonsClient.parseJsonResponse(
                        "301",
                        makeResultJson(result)),
                "missing $$EOE");
    }

    /**
     * Confirms a malformed required position field is rejected.
     */
    private static void checkMalformedRequiredValue(String json)
            throws IOException {

        String result = getResult(json)
                .replace("43.418338472", "n.a.");

        expectIOException(
                () -> HorizonsClient.parseJsonResponse(
                        "301",
                        makeResultJson(result)),
                "malformed required azimuth");
    }

    /**
     * Confirms Horizons API error JSON is surfaced as IOException.
     */
    private static void checkApiError() {
        expectIOException(
                () -> HorizonsClient.parseJsonResponse(
                        "301",
                        "{\"error\":\"invalid COMMAND\"}"),
                "API error");
    }

    /**
     * Gets the embedded result string from the saved JSON.
     */
    private static String getResult(String json) {
        JsonObject object =
                JsonParser.parseString(json).getAsJsonObject();

        return object.get("result").getAsString();
    }

    /**
     * Creates a minimal JSON object containing a modified Horizons result.
     */
    private static String makeResultJson(String result) {
        JsonObject object = new JsonObject();
        object.addProperty("result", result);
        return object.toString();
    }

    /**
     * Requires the supplied operation to fail with IOException.
     */
    private static void expectIOException(
            ThrowingOperation operation,
            String description) {

        try {
            operation.run();
            throw new AssertionError(
                    "Expected IOException for " + description);
        } catch (IOException expected) {
            // Expected.
        }
    }

    @FunctionalInterface
    private interface ThrowingOperation {
        void run() throws IOException;
    }
}