package skyview;

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

        // TODO: Collect and return Star objects once the row mapping is finished.
        throw new IOException("HYG reader not implemented yet.");
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

    /** Keeps the existing field conversion together for rows that pass the brightness filter. */
    private void readRecord(CSVRecord record, double magnitudeLimit) {
        String magStr = record.get("mag");

        if (!magStr.isBlank()) {
            double apparentMagnitude = Double.parseDouble(magStr);

            if (apparentMagnitude <= magnitudeLimit) {
                int hygId = Integer.parseInt(record.get("id"));

                if (hygId != 0) {
                    Integer hipId = Integer.parseInt(record.get("hip"));
                    Integer hdId = Integer.parseInt(record.get("hd"));
                    String displayName = record.get("proper");
                    double rightAscensionHours = Double.parseDouble(record.get("ra"));
                    double declinationDegrees = Double.parseDouble(record.get("dec"));
                    String spectralClass = record.get("spect");
                    double colorIndex = Double.parseDouble(record.get("ci"));

                    // TODO: Handle blank optional fields, name fallback, and the issue's validation.
                    // TODO: Use these values to construct a Star and add it to the returned list.
                }
            }
        }
    }
}
