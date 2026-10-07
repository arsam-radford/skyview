package skyview;
import java.util.ArrayList;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

/**
 * Reads the local HYG v4.1 CSV using Apache Commons CSV.
 * AI assistance: OpenAI Codex helped repair the merge and reader setup.
 */
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
        validateArguments(csvFile, magnitudeLimit);

        CSVFormat format = CSVFormat.RFC4180.builder()
                .setHeader()
                .get();

        try (Reader in = Files.newBufferedReader(csvFile, StandardCharsets.UTF_8);
                CSVParser parser = format.parse(in)) {
            for (CSVRecord record : parser) {
                readRecord(record, magnitudeLimit);
            }
        }

       // collects each parsed Star into a list and returns it
        List<Star> stars = new ArrayList<>();
        try (Reader in = Files.newBufferedReader(csvFile, StandardCharsets.UTF_8);
                CSVParser parser = format.parse(in)) {
            for (CSVRecord record : parser) {
                Star star = readRecord(record, magnitudeLimit);
                if (star != null) {
                    stars.add(star);
                }
            }
        }
        return stars;
    }

    /** Validates caller arguments before opening the catalog. */
    private void validateArguments(Path csvFile, double magnitudeLimit) {
        if (csvFile == null) {
            throw new IllegalArgumentException("csvFile cannot be null");
        }

        if (Double.isNaN(magnitudeLimit)) {
            throw new IllegalArgumentException("magnitudeLimit cannot be NaN");
        }

        if (Double.isInfinite(magnitudeLimit)) {
            throw new IllegalArgumentException("magnitudeLimit cannot be infinite");
        }
    }

    /**
 * Reads one row (record) from the HYG CSV catalog and converts it into a Star object.
 * Filters out stars that are dimmer than the given magnitude limit.
 */
private Star readRecord(CSVRecord record, double magnitudeLimit) {
    // Get the apparent magnitude value from the CSV row
    // This determines how bright the star appears from Earth.
    String magStr = record.get("mag");

    // Only process this record if the magnitude field is not blank
    if (!magStr.isBlank()) {
        double apparentMagnitude = Double.parseDouble(magStr);

        // Skip stars that are dimmer than the magnitude limit
        if (apparentMagnitude <= magnitudeLimit) {

            // Parse the unique catalog IDs (HYG, HIP, HD)
            // These help identify the star across different catalogs.
            int hygId = Integer.parseInt(record.get("id"));
            Integer hipId = record.isSet("hip") && !record.get("hip").isEmpty()
                    ? Integer.parseInt(record.get("hip"))
                    : null;
            Integer hdId = record.isSet("hd") && !record.get("hd").isEmpty()
                    ? Integer.parseInt(record.get("hd"))
                    : null;

            // Determine the display name for the star
            // Prefer the "proper" name; if missing, fall back to HD or HIP identifiers.
            String displayName = record.get("proper");
            if (displayName == null || displayName.isEmpty()) {
                if (hdId != null) {
                    displayName = "HD " + hdId;
                } else if (hipId != null) {
                    displayName = "HIP " + hipId;
                } else {
                    displayName = "Unnamed";
                }
            }

            // Parse the star’s position and characteristics
            // RA (right ascension) and Dec (declination) describe where the star is in the sky.
            double rightAscensionHours = Double.parseDouble(record.get("ra"));
            double declinationDegrees = Double.parseDouble(record.get("dec"));

            // Spectral class describes the star’s color and temperature (e.g., "G2V" for the Sun).
            String spectralClass = record.get("spect");

            // Color index (B–V) gives a numeric measure of the star’s color.
            double colorIndex = record.isSet("ci") && !record.get("ci").isEmpty()
                    ? Double.parseDouble(record.get("ci"))
                    : Double.NaN;

            // Construct and return a Star object
            return new Star(hygId, hipId, hdId, displayName,
                rightAscensionHours, declinationDegrees,
                apparentMagnitude, spectralClass, colorIndex);
        }
        else {
            // Skip this star because it is dimmer than the magnitude limit
            return null;
        }

            
        
    }
    else {
        // Skip this star because the magnitude field is blank
        return null;
    }
    
}

}
