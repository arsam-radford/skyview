package skyview;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Path;
import java.util.List;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

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
    public List<Star> loadStars(Path csvFile, double magnitudeLimit) throws IOException, IllegalArgumentException {
        // TODO: Implement the HYG issue using Commons CSV and the shared Star class.
        magnitudeLimit = 6.5;
        Reader in = new FileReader("csvFile");
CSVFormat format = CSVFormat.RFC4180.builder()
.setHeader()
.get();
CSVParser parser = format.parse(in);
for (CSVRecord record : parser) {
        int hygId = Integer.parseInt(record.get("id"));
        Integer hipId = Integer.parseInt(record.get("hip"));
        Integer hdId = Integer.parseInt(record.get("hd"));
        String displayName = record.get("proper");
        double rightAscensionHours = Double.parseDouble(record.get("ra"));
        double declinationDegrees = Double.parseDouble(record.get("dec"));
        double apparentMagnitude = Double.parseDouble(record.get("mag"));
        String spectralClass = record.get("spect");
        double colorIndex = Double.parseDouble(record.get("ci"));
        if (hygId == 0) continue;
        if (apparentMagnitude > magnitudeLimit) continue;
}
        throw new IOException("HYG reader not implemented yet.");
    }
}
