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
            System.out.println("Press Enter to retrieve enabled data. Ctrl+C to exit.");
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
