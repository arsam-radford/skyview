package skyview;

import java.time.Instant;
import java.util.List;

/** Synthetic examples for component development, never current observations. */
public class PreviewData {
    private final Star complete = new Star(1, 10, 20, "Example star", 2.5, 15.0, 2.0, "G2V", 0.6);
    private final Star missing = new Star(2, null, null, "Unknown fields", 1.0, -5.0, 6.5, null, null);

    /** @return synthetic star with all fields present */
    public Star getCompleteStar() {
        return complete;
    }

    /** @return synthetic star with optional fields missing */
    public Star getMissingStar() {
        return missing;
    }

    /** @return synthetic composition, including an unavailable numeric value */
    public List<ElementAbundance> getAbundances() {
        return List.of(new ElementAbundance("Example star", "Ca", "asplund09", 0.3),
                new ElementAbundance("Example star", "Fe", "asplund09", null));
    }

    /** @return unsorted synthetic samples for two Horizons body identifiers */
    public List<PositionSample> getPositions() {
        return List.of(
                new PositionSample("301", Instant.parse("2026-10-01T00:10:00Z"), 45, 20, null, null),
                new PositionSample("499", Instant.parse("2026-10-01T00:00:00Z"), 90, -5, null, null),
                new PositionSample("301", Instant.parse("2026-10-01T00:00:00Z"), 40, 15, null, null));
    }
}
