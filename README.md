# Skyview

Java 17+ and a small Gradle build. To build and run:

```sh
./gradlew run --console=plain
./gradlew check
```

On Windows use `gradlew.bat`. Press Enter to retrieve enabled sections, Ctrl+C exits
Uncomment your source's call in `src/main/java/skyview/SkyviewApplication.java`.

The three readers are intentionally unimplemented.


**Failure contract:**

- Successful methods return a non null list
- An empty list means a valid result with no matching records (and conversely not a network/parsing failure)
- Missing optional fields remain unavailable. Optional numeric values use `null`, never zero
- Missing required fields or malformed responses produce an IOException with useful context
- Interrupted requests propagate InterruptedException, the application handles cancellation
- The application handles each source’s failure separately, so one unavailable service doesnt erase successful results from others.


Saved examples are for development, the lab demonstration must acquire current
external data on each run.

Sources: [HYG](https://github.com/astronexus/HYG-Database/blob/main/hyg/README.md),
[Horizons](https://ssd-api.jpl.nasa.gov/doc/horizons.html),
[Hypatia](https://hypatiacatalog.com/api),
[Commons CSV](https://commons.apache.org/proper/commons-csv/),
[Gson](https://google.github.io/gson/),
[Gradle](https://docs.gradle.org/current/userguide/application_plugin.html).
HYG excerpts are CC BY-SA 4.0; see [sample provenance](data/README.md).
