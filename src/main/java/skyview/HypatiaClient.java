package skyview;

import java.io.IOException;
import java.util.List;

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
    public List<ElementAbundance> fetchAbundances(String starIdentifier, String element)
            throws IOException, InterruptedException {
        // TODO: impl the Hypatia issue using the shared ElementAbundance class
        throw new IOException("Hypatia client not implemented yet.");
    }
}
