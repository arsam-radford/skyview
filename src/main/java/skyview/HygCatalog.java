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
        Files.newBufferedReader(csvFile, StandardCharsets.UTF_8);
        Reader in = new FileReader("csvFile");
<<<<<<< HEAD
        if (StandardCharsets.UTF_8 == null) {
=======
    
    
    // Validate arguments
    if (csvFile == null) {
>>>>>>> 628c3841f6aab1a9fe81bd28ea73fbb4c8a23143
    throw new IllegalArgumentException("csvFile cannot be null"); // checks if csvFile is null
    }
    if (Double.isNaN(magnitudeLimit)) {
    throw new IllegalArgumentException("magnitudeLimit cannot be NaN"); // checks if magnitudeLimit is NaN
    }
    if (Double.isInfinite(magnitudeLimit)) {
    throw new IllegalArgumentException("magnitudeLimit cannot be infinite"); // checks if magnitudeLimit is infinite
    }
CSVFormat format = CSVFormat.RFC4180.builder()
.setHeader()
.get();
CSVParser parser = format.parse(in);
for (CSVRecord record : parser) {
    // some random bullshit?
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
        throw new IOException("HYG reader not implemented yet.");
    // process one row
    String idStr = record.get("id");
    String raStr = record.get("ra");
    String decStr = record.get("dec");
    String magStr = record.get("mag");
    if (magStr ==null || magStr.isBlank()) {
    continue; // skip this record if magStr is null or blank    

    Double mag = Double.parseDouble(magStr);
    if (mag > magnitudeLimit) {
    continue; // skip this record if mag is greater than magnitudeLimit
    }
    int Id = Integer.parseInt(idStr);
    if (Id == 0) {
    continue; // skip this record if Id is 0
    }
        }
    }
    


    for (CSVRecord record : parser) {
    // process one row
    String idStr = record.get("id");
    String raStr = record.get("ra");
    String decStr = record.get("dec");
    String magStr = record.get("mag");
    }
    if (magStr ==null || magStr.isBlank()) {
    continue; // skip this record if magStr is null or blank    

    Double mag = Double.parseDouble(magStr);
    if (mag > magnitudeLimit) {
    continue; // skip this record if mag is greater than magnitudeLimit
    
    int hygId = Integer.parseInt(idStr);
    hygId = 0;
    continue; // skip this record if hygId is 0


}

    
}

}
}