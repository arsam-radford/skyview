package skyview;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/** Retrieves stellar elemental abundances from Hypatia using Gson */
public class HypatiaClient {
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

    private static final String solarNorm = "asplund09";

    public List<ElementAbundance> fetchAbundances(String starIdentifier, String element)
            throws IOException, InterruptedException {
        // TODO: impl the Hypatia issue using the shared ElementAbundance class
        
        String hypatiaUrl = "https://hypatiacatalog.com/hypatia/api/v2/composition/" + "?name=" + starIdentifier + "&element=" + element + "&solarnorm=" + solarNorm;

        HttpClient client = HttpClient.newBuilder().build();

        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(hypatiaUrl)).GET().build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("Hypatia returned HTTP status " + response.statusCode());
        }

        Gson gson = new Gson();
        JsonArray results = gson.fromJson(response.body(), JsonArray.class);

        List<ElementAbundance> abundances = new ArrayList<>();

        for (int i = 0; i < results.size(); i++) {
            JsonObject result = results.get(i).getAsJsonObject();

            String starName = result.get("name").getAsString();

            String resultElement = result.get("element").getAsString();

            String solarNormalization = result.get("solarnorm").getAsString();

            Double medianAbundance = result.get("median_value").getAsDouble();

            ElementAbundance abundance = new ElementAbundance(starName, resultElement, solarNormalization, medianAbundance);
            abundances.add(abundance);
        }

        return abundances;
    }
}

