package skyview;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

/** Reads the full OpenNGC catalogs using the documented field definitions */
public class OpenNgcCatalog {
    private static final double MINUTES_PER_UNIT = 60.0;
    private static final double SECONDS_PER_UNIT = 3600.0;
    private static final double HOURS_PER_CIRCLE = 24.0;
    private static final double MAX_DECLINATION = 90.0;
    private static final int COORDINATE_PARTS = 3;
    private static final Set<String> EXCLUDED_TYPES = Set.of("Dup", "NonEx", "*", "**", "Nova");
    private static final List<String> REQUIRED_HEADERS = List.of("Name", "Type", "RA", "Dec",
            "V-Mag", "MajAx", "MinAx", "SurfBr", "M", "Common names");

    /**
     * Reads objects with known positions without applying a brightness limit
     * @param csvFile local OpenNGC CSV file
     * @return objects in file order, excluding duplicate entries and individual stars
     * @throws IOException if the file or a nonblank field is malformed
     */
    public List<DeepSkyObject> loadObjects(Path csvFile) throws IOException {
        CSVFormat format = CSVFormat.DEFAULT.builder().setDelimiter(';').setHeader().get();
        List<DeepSkyObject> objects = new ArrayList<>();

        try (Reader reader = Files.newBufferedReader(csvFile, StandardCharsets.UTF_8);
                var parser = format.parse(reader)) {
            for (String column : REQUIRED_HEADERS) {
                if (!parser.getHeaderMap().containsKey(column)) {
                    throw new IOException("OpenNGC missing header " + column);
                }
            }

            for (CSVRecord row : parser) {
                if (!EXCLUDED_TYPES.contains(row.get("Type"))
                        && !row.get("RA").isBlank() && !row.get("Dec").isBlank()) {
                    objects.add(readObject(row));
                }
            }
        } catch (UncheckedIOException exception) {
            throw new IOException("Cannot read OpenNGC catalog " + csvFile, exception.getCause());
        }

        return objects;
    }

    // converts one row into a deep sky object
    private DeepSkyObject readObject(CSVRecord row) throws IOException {
        if (row.get("Name").isBlank() || row.get("Type").isBlank()) {
            throw new IOException("OpenNGC row " + row.getRecordNumber() + " has no name or type");
        }

        return new DeepSkyObject(row.get("Name"), row.get("Type"), optionalText(row, "Common names"),
                messierName(row), coordinate(row, "RA"), coordinate(row, "Dec"), optionalNumber(row, "V-Mag"),
                optionalNumber(row, "MajAx"), optionalNumber(row, "MinAx"), optionalNumber(row, "SurfBr"));
    }

    // converts a j2000 coordinate from sexagesimal notation
    private double coordinate(CSVRecord row, String column) throws IOException {
        String text = row.get(column).strip();
        String[] parts = text.split(":", -1);

        if (parts.length != COORDINATE_PARTS) {
            throw new IOException("OpenNGC row " + row.getRecordNumber() + " column " + column
                    + " must have three coordinate parts");
        }

        try {
            double first = Double.parseDouble(parts[0]);
            double minutes = Double.parseDouble(parts[1]);
            double seconds = Double.parseDouble(parts[2]);
            double value = Math.copySign(Math.abs(first) + minutes / MINUTES_PER_UNIT
                    + seconds / SECONDS_PER_UNIT, first);

            // rounded seconds can equal 60 and carry into minutes
            if (!Double.isFinite(value) || minutes < 0 || minutes >= MINUTES_PER_UNIT
                    || seconds < 0 || seconds > MINUTES_PER_UNIT
                    || (column.equals("RA") && (value < 0 || value >= HOURS_PER_CIRCLE))
                    || (column.equals("Dec") && Math.abs(value) > MAX_DECLINATION)) {
                throw new IOException("OpenNGC row " + row.getRecordNumber() + " column " + column
                        + " has an invalid coordinate");
            }

            return value;
        } catch (NumberFormatException exception) {
            throw new IOException("OpenNGC row " + row.getRecordNumber() + " column " + column
                    + " has an invalid coordinate", exception);
        }
    }

    // returns messier identifier without catalog padding, or null
    private String messierName(CSVRecord row) throws IOException {
        String text = optionalText(row, "M");
        if (text == null) {
            return null;
        }

        try {
            return "M" + Integer.parseInt(text);
        } catch (NumberFormatException exception) {
            throw new IOException("OpenNGC row " + row.getRecordNumber() + " has an invalid Messier number", exception);
        }
    }

    // returns field text, or null when it is blank
    private String optionalText(CSVRecord row, String column) {
        String value = row.get(column).strip();
        if (value.isEmpty()) {
            return null;
        }

        return value;
    }

    // returns an optional finite measurement
    private Double optionalNumber(CSVRecord row, String column) throws IOException {
        String text = optionalText(row, column);
        if (text == null) {
            return null;
        }

        try {
            double value = Double.parseDouble(text);
            if (!Double.isFinite(value)) {
                throw new IOException("OpenNGC row " + row.getRecordNumber() + " column " + column
                        + " must be finite");
            }

            return value;
        } catch (NumberFormatException exception) {
            throw new IOException("OpenNGC row " + row.getRecordNumber() + " column " + column
                    + " has an invalid number", exception);
        }
    }

}
