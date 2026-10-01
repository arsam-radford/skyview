package skyview;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/** Reads the local HYG v4.1 CSV using Apache Commons CSV. */
public class HygCatalog {
    /**
     * Loads stars whose visual magnitude is at most the inclusive limit.
     *
     * @param csvFile local UTF-8 catalog with a header row
     * @param magnitudeLimit finite inclusive upper limit on apparent magnitude
     * @return non-null stars in file order; empty when no eligible stars exist
     * @throws IOException if the file cannot be read or required data is malformed
     * @throws IllegalArgumentException if an argument is null or invalid
     */
    public List<Star> loadStars(Path csvFile, double magnitudeLimit) throws IOException {
        // TODO: Implement the HYG issue using Commons CSV and the shared Star class.
        throw new IOException("HYG reader not implemented yet.");
    }
}
