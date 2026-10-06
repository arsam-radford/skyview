package skyview;

import java.util.Scanner;

/** Terminal entry point for demonstrating the data readers */
public class SkyviewDriver {
    /**
     * Retrieves enabled sources whenever Enter is pressed.
     *
     * @param args unused command-line arguments
     */
    public static void main(String[] args) {
        SkyviewApplication application = new SkyviewApplication();
        try (Scanner input = new Scanner(System.in)) {
            System.out.println("Skyview - live data demo");
            System.out.println("JPL Horizons: current Moon positions | NOAA GOES-19: cloud mask and cloud-top height");
            System.out.println("Press Enter to fetch data for Radford. Ctrl+C to exit.");
            System.out.print("> ");
            while (input.hasNextLine()) {
                input.nextLine();
                application.retrieveData();
                System.out.print("> ");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            System.out.println("Retrieval interrupted; exiting.");
        }
    }
}
