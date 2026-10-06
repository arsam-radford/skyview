# Skyview

Java 17+ and a small Gradle build. To build and run:

```sh
./gradlew run --console=plain
./gradlew check
```

On Windows use `gradlew.bat`. Press Enter to fetch current Moon positions from JPL Horizons and the latest GOES-19 cloud mask for Radford. Ctrl+C exits. The terminal shows UTC position times and the cloud scan's actual observation times and age.

HYG and Hypatia remain unimplemented and disabled for this retrieval demo. Horizons returns calculated positions; GOES returns satellite observations of geographic cloud coverage.


**Failure contract:**

- Successful methods return a non null list
- An empty list means a valid result with no matching records (and conversely not a network/parsing failure)
- Missing optional fields remain unavailable. Optional numeric values use `null`, never zero
- Missing required fields or malformed responses produce an IOException with useful context
- Interrupted requests propagate InterruptedException, the application handles cancellation
- The application handles each source’s failure separately, so one unavailable service doesnt erase successful results from others.


Saved examples are for development, the lab demonstration must acquire current external data on each run.

Sources: [HYG](https://github.com/astronexus/HYG-Database/blob/main/hyg/README.md), [Horizons](https://ssd-api.jpl.nasa.gov/doc/horizons.html), [Hypatia](https://hypatiacatalog.com/api), [Commons CSV](https://commons.apache.org/proper/commons-csv/), [Gson](https://google.github.io/gson/), [Gradle](https://docs.gradle.org/current/userguide/application_plugin.html). HYG excerpts are CC BY-SA 4.0; see [sample provenance](data/README.md).

GOES sources: [NOAA public files](https://noaa-goes19.s3.amazonaws.com/), [NOAA cloud-mask flags](https://www.ncei.noaa.gov/sites/default/files/2024-12/GOES-19_ABI_L2_CSM_Beta_ReadMe.pdf), [NOAA navigation](https://www.star.nesdis.noaa.gov/atmospheric-composition-training/satellite_data_goes_imager_projection.php), [NetCDF-Java](https://docs.unidata.ucar.edu/netcdf-java/current/userguide/netcdf_dataset.html), [JDK HttpClient](https://docs.oracle.com/en/java/javase/17/docs/api/java.net.http/java/net/http/HttpClient.html), [S3 listings](https://docs.aws.amazon.com/AmazonS3/latest/API/API_ListObjectsV2.html), [Gradle source dependencies](https://docs.gradle.org/current/javadoc/org/gradle/vcs/SourceControl.html).

The build downloads and builds the external GOES parser from GitHub. Internet access is required for initial dependency setup and live retrieval. AI assistance: OpenAI Codex assisted with GOES integration and the contingency demo changes.
