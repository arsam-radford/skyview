package skyview;

import com.google.gson.JsonParser;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import org.apache.commons.csv.CSVFormat;

/** Dependency and repeated input driver */
public class SetupCheckDriver {
    /**
     * Verifies dependency loading, quoted CSV fields, missing JSON data, and terminal EOF
     *
     * @param args unused cli arguments
     * @throws Exception if the setup check fails
     */
    public static void main(String[] args) throws Exception {
        try (var rows = CSVFormat.RFC4180.builder().setHeader().get().parse(
                new StringReader("name,mag\n\"Test, star\",0.0\n"))) {
            assert rows.getRecords().get(0).get("name").equals("Test, star");
        }
        assert JsonParser.parseString("{\"value\":null}").getAsJsonObject().get("value").isJsonNull();
        boolean rejectedInvalidLocation = false;
        try {
            new ObserverLocation(Double.NaN, 0.0, 0.0);
        } catch (IllegalArgumentException exception) {
            rejectedInvalidLocation = true;
        }
        assert rejectedInvalidLocation;
        var originalInput = System.in;
        var originalOutput = System.out;
        var output = new ByteArrayOutputStream();
        try (var capturedOutput = new PrintStream(output, true, StandardCharsets.UTF_8)) {
            System.setIn(new ByteArrayInputStream("\n\n".getBytes(StandardCharsets.UTF_8)));
            System.setOut(capturedOutput);
            SkyviewDriver.main(new String[0]);
        } finally {
            System.setIn(originalInput);
            System.setOut(originalOutput);
        }
        assert output.toString(StandardCharsets.UTF_8).split("Retrieving enabled", -1).length == 3;
        System.out.println("Setup check passed.");
    }
}
