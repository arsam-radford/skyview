package skyview;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/** Retrieves JPL Horizons observer ephemerides using Gson and Commons CSV. */
public class HorizonsClient {
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
        // TODO: Implement the Horizons issue; requests must be sequential.
        throw new IOException("Horizons client not implemented yet.");
    }
}
