package skyview;

import java.util.ArrayList;
import java.util.List;

/** Retains the catalog for repeated searches. */
public class StarStore {
    private final List<Star> stars;

    /**
     * Copies the supplied catalog
     * @param stars non null catalog with no null entries
     */
    public StarStore(List<Star> stars) {
        this.stars = new ArrayList<>(stars);
    }

    /**
     * Returns a separate list in catalog order
     * @return all stored stars
     */
    public List<Star> getStars() {
        return new ArrayList<>(stars);
    }

    /**
     * Finds names that contain the query, ignoring letter case and outer spaces
     * @param query non null name text, blank to match all names
     * @param magnitudeLimit finite inclusive upper limit
     * @return a separate result list in catalog order
     * @throws IllegalArgumentException if the query is null or the limit is not finite
     */
    public List<Star> findStars(String query, double magnitudeLimit) {
        // TODO Bryson: filter the stored list without changing it.
        throw new UnsupportedOperationException("Star search task pending");
    }
}
