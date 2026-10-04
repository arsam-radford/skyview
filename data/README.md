# samples

These are saved input for parser development October 1, 2026.

- `hyg-sample.csv`: original full-column Sirius and Vega rows from [HYG v4.1](https://github.com/astronexus/HYG-Database/blob/main/hyg/CURRENT/hygdata_v41.csv). Attribution: David Nash / Astronexus, HYG Database. The catalog and this excerpt are [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0/). No field values were changed; CSV quoting was normalized when extracting rows.
- `horizons-moon.json`: full [JPL Horizons](https://ssd-api.jpl.nasa.gov/doc/horizons.html) response for body 301; Earth observer longitude -80.55 degrees, latitude 37.14 degrees, elevation 0.6 km; UTC 2026-10-01 00:00 through 00:10, five-minute steps. Parameters: `EPHEM_TYPE=OBSERVER`, `CENTER=coord@399`, `COORD_TYPE=GEODETIC`, `QUANTITIES=4,9,10`, `CSV_FORMAT=YES`, `TIME_DIGITS=SECONDS`, `ANG_FORMAT=DEG`, `EXTRA_PREC=YES`, `OBJ_DATA=NO`.
- `hypatia-calcium.json`: full response from the [official calcium example](https://hypatiacatalog.com/hypatia/api/v2/composition?name=HIP32970&element=ca&solarnorm=asplund09).
- `hypatia-no-match.json`: full [Vega calcium response](https://hypatiacatalog.com/hypatia/api/v2/composition?name=Vega&element=ca&solarnorm=asplund09); this returns a `name: "not-found"` object, not an empty array.

To obtain the full HYG catalog, download the linked CSV as `data/hygdata_v41.csv`, then change the `CATALOG_FILE` variable in `SkyviewApplication`. The full downloaded file is ignored by Git; the small development sample is included.
