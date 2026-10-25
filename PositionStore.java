package skyview;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Retains calculated positions for lookup */
public class PositionStore {
    private final List<PositionSample> samples;

    /**
     * Copies the supplied samples in their original order.
     * @param samples non null samples with no null entries
     */
    public PositionStore(List<PositionSample> samples) {
        this.samples = new ArrayList<>(samples);
    }

    /**
     * Finds the nearest stored time for the given body
     * Equal distances select the earlier time. Duplicate times select the first entry.
     * @param bodyId exact non blank Horizons identifier
     * @param time non null requested time
     * @return an actual stored sample, or null when the body has no samples
     * @throws IllegalArgumentException if either argument is invalid
     */
    public PositionSample findNearest(String bodyId, Instant time) {
        if (bodyId == null || bodyId.isBlank() || time == null) {
            throw new IllegalArgumentException("Body identifier and time must be provided");
        }

        PositionSample nearest = null;
        Duration nearestDistance = null;
        for (PositionSample sample : samples) {
            if (bodyId.equals(sample.getBodyId())) {
                Duration distance = Duration.between(time, sample.getTime()).abs();
                if (nearest == null || distance.compareTo(nearestDistance) < 0
                        || (distance.compareTo(nearestDistance) == 0
                        && sample.getTime().isBefore(nearest.getTime()))) {
                    nearest = sample;
                    nearestDistance = distance;
                }
            }
        }
        return nearest;
    }
}
