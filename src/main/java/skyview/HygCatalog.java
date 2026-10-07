package skyview;

import java.util.ArrayList;
import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
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
    private static final int SUN_ID = 0;
    private static final double HOURS_PER_CIRCLE = 24.0;
    private static final double MAX_DECLINATION_DEGREES = 90.0;

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

        List<Star> stars = new ArrayList<>();
        try (Reader in = Files.newBufferedReader(csvFile, StandardCharsets.UTF_8);
                CSVParser parser = format.parse(in)) {
            validateHeaders(parser);

            for (CSVRecord record : parser) {
                Star star = readRecord(record, magnitudeLimit);
                if (star != null) {
                    stars.add(star);
                }
            }
        } catch (UncheckedIOException exception) {
            throw new IOException("HYG malformed CSV in " + csvFile, exception.getCause());
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

    /** Rejects files missing columns used by the reader, even when they contain no stars. */
    private void validateHeaders(CSVParser parser) throws IOException {
        for (String column : List.of("id", "hip", "hd", "proper", "ra", "dec", "mag", "spect", "ci")) {
            if (!parser.getHeaderMap().containsKey(column)) {
                throw new IOException("HYG missing required header: " + column);
            }
        }
    }

    /** Filters one catalog row before converting its remaining fields. */
    private Star readRecord(CSVRecord record, double magnitudeLimit) throws IOException {
        Double apparentMagnitude = readDouble(record, "mag", false);
        if (apparentMagnitude == null || apparentMagnitude > magnitudeLimit) {
            return null;
        }

        int hygId = readInteger(record, "id", true);
        if (hygId == SUN_ID) {
            return null;
        }

        return readStar(record, hygId, apparentMagnitude);
    }

    /** Converts an eligible catalog row into a Star. */
    private Star readStar(CSVRecord record, int hygId, double apparentMagnitude) throws IOException {
        Integer hipId = readInteger(record, "hip", false);
        Integer hdId = readInteger(record, "hd", false);
        String displayName = readDisplayName(record, hygId, hipId, hdId);
        double rightAscensionHours = readDouble(record, "ra", true);
        double declinationDegrees = readDouble(record, "dec", true);

        if (rightAscensionHours < 0 || rightAscensionHours >= HOURS_PER_CIRCLE) {
            throw malformedField(record, "ra", "must be in [0, 24) hours");
        }

        if (Math.abs(declinationDegrees) > MAX_DECLINATION_DEGREES) {
            throw malformedField(record, "dec", "must be in [-90, 90] degrees");
        }

        String spectralClass = readText(record, "spect");
        Double colorIndex = readDouble(record, "ci", false);

        return new Star(hygId, hipId, hdId, displayName,
                rightAscensionHours, declinationDegrees,
                apparentMagnitude, spectralClass, colorIndex);
    }

    /** Uses the proper name, then HIP, HD, or HYG identifiers. */
    private String readDisplayName(CSVRecord record, int hygId, Integer hipId, Integer hdId) throws IOException {
        String displayName = readText(record, "proper");
        if (displayName != null) {
            return displayName;
        }

        if (hipId != null) {
            return "HIP " + hipId;
        }

        if (hdId != null) {
            return "HD " + hdId;
        }

        return "HYG " + hygId;
    }

    /** Reads a finite number, preserving an unavailable optional value as null. */
    private Double readDouble(CSVRecord record, String column, boolean required) throws IOException {
        String text = readText(record, column);
        if (text == null && !required) {
            return null;
        }

        if (text == null) {
            throw malformedField(record, column, "required value is blank");
        }

        try {
            double value = Double.parseDouble(text);
            if (!Double.isFinite(value)) {
                throw malformedField(record, column, "value must be finite");
            }

            return value;
        } catch (NumberFormatException exception) {
            throw new IOException("HYG row " + record.getRecordNumber() + ", column " + column
                    + ": malformed number " + text, exception);
        }
    }

    /** Reads an integer identifier, preserving an unavailable optional value as null. */
    private Integer readInteger(CSVRecord record, String column, boolean required) throws IOException {
        String text = readText(record, column);
        if (text == null && !required) {
            return null;
        }

        if (text == null) {
            throw malformedField(record, column, "required value is blank");
        }

        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException exception) {
            throw new IOException("HYG row " + record.getRecordNumber() + ", column " + column
                    + ": malformed integer " + text, exception);
        }
    }

    /** Reads a trimmed field; a short row is a malformed file. */
    private String readText(CSVRecord record, String column) throws IOException {
        if (!record.isSet(column)) {
            throw malformedField(record, column, "field is missing from row");
        }

        String text = record.get(column).trim();
        if (text.isEmpty()) {
            return null;
        }

        return text;
    }

    /** Identifies the catalog row and column responsible for a parsing failure. */
    private IOException malformedField(CSVRecord record, String column, String reason) {
        return new IOException("HYG row " + record.getRecordNumber() + ", column " + column + ": " + reason);
    }
}
