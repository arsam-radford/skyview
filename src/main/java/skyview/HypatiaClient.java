package skyview;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

/** Retrieves stellar elemental abundances from Hypatia using Gson. */
public class HypatiaClient {

    private static final String SOLAR_NORM = "asplund09";
    private static final Duration CONNECTION_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(CONNECTION_TIMEOUT).build();

    /**
     * Retrieves one element's measurements using the fixed asplund09 solar reference.
     *
     * @param starIdentifier catalog identifier, such as "HIP32970"
     * @param element chemical element name, such as "ca"
     * @return non null results, empty for a documented no match or no measurement result
     * @throws IOException if acquisition, API processing, or required parsing fails
     * @throws InterruptedException if the HTTP request is interrupted
     * @throws IllegalArgumentException if an argument is null or invalid
     */
    public List<ElementAbundance> fetchAbundances(
            String starIdentifier, String element)
            throws IOException, InterruptedException {

        validateArguments(starIdentifier, element);

        URI uri = buildUri(starIdentifier, element);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri).timeout(REQUEST_TIMEOUT).GET().build();

        HttpResponse<String> response = HTTP_CLIENT.send(
                request, HttpResponse.BodyHandlers.ofString());

        checkStatus(response);

        return parseResponse(response.body());
    }

    /**
     * Checks that the star identifier and element are not null or blank.
     *
     * @param starIdentifier catalog identifier for the star
     * @param element chemical element to retrieve
     * @throws IllegalArgumentException if either argument is null or blank
     */
    private void validateArguments(String starIdentifier, String element) {

        if (starIdentifier == null || starIdentifier.isBlank()) {
            throw new IllegalArgumentException(
                    "starIdentifier must not be null or blank.");
        }

        if (element == null || element.isBlank()) {
            throw new IllegalArgumentException(
                    "element must not be null or blank.");
        }
    }

    /**
     * Builds the Hypatia composition API URI using encoded parameters.
     *
     * @param starIdentifier catalog identifier for the star
     * @param element chemical element to retrieve
     * @return URI for the Hypatia request
     */
    private URI buildUri(String starIdentifier, String element) {
        String encodedName = URLEncoder.encode(
                starIdentifier, StandardCharsets.UTF_8);

        String encodedElement = URLEncoder.encode(
                element, StandardCharsets.UTF_8);

        String url = "https://hypatiacatalog.com/hypatia/api/v2/composition/"
                + "?name=" + encodedName
                + "&element=" + encodedElement
                + "&solarnorm=" + SOLAR_NORM;

        return URI.create(url);
    }

    /**
     * Checks that the HTTP response has a successful status code.
     *
     * @param response response received from Hypatia
     * @throws IOException if the response status is not successful
     */
    private void checkStatus(HttpResponse<String> response)
            throws IOException {

        int status = response.statusCode();

        if (status < 200 || status >= 300) {
            throw new IOException(
                    "Hypatia returned HTTP status " + status);
        }
    }

    /**
     * Parses the Hypatia JSON response into abundance results.
     *
     * @param body JSON response body
     * @return parsed abundance measurements
     * @throws IOException if the response cannot be parsed
     */
    private List<ElementAbundance> parseResponse(String body)
            throws IOException {

        try {
            Gson gson = new Gson();
            JsonArray results = gson.fromJson(body, JsonArray.class);

            if (results == null) {
                throw new IOException("Hypatia response must contain a JSON array.");
            }

            return parseResults(results);

        } catch (JsonParseException | IllegalStateException | UnsupportedOperationException exception) {
            throw new IOException(
                    "Failed to parse Hypatia response.", exception);
        }
    }

    /**
     * Converts each Hypatia result into an ElementAbundance object.
     *
     * @param results array of results returned by Hypatia
     * @return list of parsed abundance measurements
     * @throws IOException if a result cannot be parsed
     */
    private List<ElementAbundance> parseResults(JsonArray results)
            throws IOException {

        List<ElementAbundance> abundances = new ArrayList<>();

        for (int i = 0; i < results.size(); i++) {
            JsonObject result = results.get(i).getAsJsonObject();

            if (isNotFound(result)) {
                continue;
            }

            abundances.add(parseAbundance(result));
        }

        return abundances;
    }

    /**
     * Checks whether Hypatia reports that a star was not found.
     *
     * @param result result object returned by Hypatia
     * @return true if the result is marked as not found
     */
    private boolean isNotFound(JsonObject result) {
        JsonElement name = result.get("name");

        return name != null
                && !name.isJsonNull()
                && "not-found".equals(name.getAsString());
    }

    /**
     * Converts one Hypatia result into an ElementAbundance object.
     *
     * @param result Hypatia result to parse
     * @return parsed ElementAbundance object
     * @throws IOException if required fields are missing or invalid
     */
    private ElementAbundance parseAbundance(JsonObject result)
            throws IOException {

        try {
            String starName = readRequiredText(result, "name");
            String element = readRequiredText(result, "element");
            String solarNorm = readRequiredText(result, "solarnorm");
            Double median = parseMedianValue(result);

            return new ElementAbundance(
                    starName, element, solarNorm, median);

        } catch (NullPointerException | IllegalStateException exception) {
            throw new IOException(
                    "Hypatia response is missing required fields.",
                    exception);
        }
    }

    /** Reads a required nonblank string without treating numbers as names. */
    private String readRequiredText(JsonObject result, String field) throws IOException {
        JsonElement value = result.get(field);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()
                || value.getAsString().isBlank()) {
            throw new IOException("Hypatia missing or invalid required field: " + field);
        }

        return value.getAsString();
    }

    /**
     * Reads the optional median abundance value from a result.
     *
     * @param result Hypatia result containing the median value
     * @return median abundance in dex, or null if unavailable
     * @throws IOException if the median value is invalid
     */
    private Double parseMedianValue(JsonObject result)
            throws IOException {

        JsonElement median = result.get("median_value");

        if (median == null || median.isJsonNull()) {
            return null;
        }

        String text = median.getAsString();

        if (text.isBlank()) {
            return null;
        }

        return parseFiniteDouble(text);
    }

    /**
     * Converts a median value to a finite double.
     *
     * @param text value to convert
     * @return parsed finite double value
     * @throws IOException if the value is malformed or not finite
     */
    private Double parseFiniteDouble(String text)
            throws IOException {

        try {
            double value = Double.parseDouble(text);

            if (!Double.isFinite(value)) {
                throw new IOException(
                        "Hypatia returned non-finite median_value.");
            }

            return value;

        } catch (NumberFormatException exception) {
            throw new IOException(
                    "Hypatia returned malformed median_value: " + text,
                    exception);
        }
    }
}
