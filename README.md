# Skyview

Java 17+ and a small Gradle build. To build and run:

```sh
./gradlew run --console=plain
./gradlew check
```

Run `./gradlew gui` for the graphical catalog browser. The first GUI or terminal run downloads the full HYG and OpenNGC catalogs. Later runs reuse those files. Solar system positions, selected-star abundances, and cloud imagery use live requests.

On Windows, use `gradlew.bat`. In the terminal demo, press Enter to retrieve data for Radford. Press Ctrl+C to exit. The cloud image is saved as `build/clouds/radford.png`.
**Failure contract:**

- Successful readers return a result that is not `null`. A list can be empty when there are no matching records.
- Missing optional numbers use `null`. Do not replace them with zero
- Missing or malformed required data produces an `IOException` that describes the problem. Failed requests also produce an `IOException`
- Readers pass `InterruptedException` to the application so it can handle cancellation
- A failed request must not remove successful results from other sources

Saved responses are used only by parser checks. The application does not use them as fallback data.

Sources: [OpenNGC](https://github.com/mattiaverga/OpenNGC), [HYG](https://github.com/astronexus/HYG-Database/blob/main/hyg/README.md), [Horizons](https://ssd-api.jpl.nasa.gov/doc/horizons.html), [Hypatia](https://hypatiacatalog.com/api), [Commons CSV](https://commons.apache.org/proper/commons-csv/), [Gson](https://google.github.io/gson/), [Gradle](https://docs.gradle.org/current/userguide/application_plugin.html). HYG and OpenNGC use the CC BY-SA 4.0 license. See [catalog provenance](data/README.md).

Cloud source: [UW–Madison SSEC RealEarth API](https://realearth.ssec.wisc.edu/doc/api.php), [cloud-mask legend](https://clouds.ssec.wisc.edu/api/legend?products=G18-ACM), [JDK HttpClient](https://docs.oracle.com/en/java/javase/17/docs/api/java.net.http/java/net/http/HttpClient.html), and [ImageIO](https://docs.oracle.com/en/java/javase/17/docs/api/java.desktop/javax/imageio/ImageIO.html). The client downloads the latest available frame.

Internet access is required for initial dependency setup and live retrieval

GUI sources: [JavaFX setup](https://openjfx.io/openjfx-docs/#gradle), [ListView](https://openjfx.io/javadoc/21/javafx.controls/javafx/scene/control/ListView.html), [layouts](https://openjfx.io/javadoc/21/javafx.graphics/javafx/scene/layout/package-summary.html).

AI assistance: OpenAI Codex assisted with catalog integration and live source wiring.
