# Catalogs

Run `./gradlew downloadCatalogs` to download all three catalog files. The `gui` and `run` tasks do this automatically when a file is missing. These full scientific catalogs are reused locally and ignored by Git.

- `hygdata_v41.csv` is the full HYG v4.1 star catalog by David Nash / Astronexus. Its source is [HYG commit c7f7f883](https://github.com/astronexus/HYG-Database/blob/c7f7f883fe678cc7680169a50ccd7dcc49b060ce/hyg/CURRENT/hygdata_v41.csv).
- `openngc.csv` and `openngc-addendum.csv` are the full OpenNGC catalog and addendum by Mattia Verga and contributors. Their source is [OpenNGC commit be3cd6c5](https://github.com/mattiaverga/OpenNGC/tree/be3cd6c5498806a2fbb2f7e0452ef802dbb02be0/database_files). The addendum includes objects such as M45 that have no NGC or IC designation.

Both catalogs use the [CC BY-SA 4.0 license](https://creativecommons.org/licenses/by-sa/4.0/). Their downloaded CSV contents are unchanged. The OpenNGC reader excludes duplicate and nonexistent entries, individual stars, and entries without coordinates. Missing measurements remain unknown. The reader normalizes coordinates whose seconds round to 60.

# Parser fixtures

These saved responses are for parser development only. The application does not load them.

- `hyg-sample.csv` contains the original full-column Sirius and Vega rows from HYG v4.1. Field values are unchanged. CSV quoting was normalized when the rows were extracted.
- `horizons-moon.json` is a full [Horizons response](https://ssd-api.jpl.nasa.gov/doc/horizons.html) for the Moon near Radford on 2026 October 1, from 0000 to 0010 UTC. The observer is at latitude 37.14 degrees, longitude -80.55 degrees, and elevation 600 metres. The positions are five minutes apart.
- `hypatia-calcium.json` is the full response from the [calcium query for HIP32970](https://hypatiacatalog.com/hypatia/api/v2/composition?name=HIP32970&element=ca&solarnorm=asplund09).
- `hypatia-no-match.json` is the full response from the [Vega calcium query](https://hypatiacatalog.com/hypatia/api/v2/composition?name=Vega&element=ca&solarnorm=asplund09). It records a valid result with no matching star.
