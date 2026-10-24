package skyview;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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

    if (query == null) {
        throw new IllegalArgumentException("query cannot be null");
    }

    if (!Double.isFinite(magnitudeLimit)) {
        throw new IllegalArgumentException("magnitude limit must be a finite number");
    }

    String normalizedQuery = query.strip().toLowerCase(Locale.ROOT);

    List<Star> matches = new ArrayList<>();

    for (Star star : stars) {

        boolean magnitudeMatch = star.getApparentMagnitude() <= magnitudeLimit;

        String normalizedName = star.getDisplayName().toLowerCase(Locale.ROOT);

        boolean nameMatch = normalizedQuery.isEmpty() || normalizedName.contains(normalizedQuery);

        if (nameMatch && magnitudeMatch) {
            matches.add(star);
        }
    }

    return matches;
}
}
