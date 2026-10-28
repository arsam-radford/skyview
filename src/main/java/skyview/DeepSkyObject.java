package skyview;

import java.util.Objects;

/** One OpenNGC object with J2000 coordinates and optional observing information */
public class DeepSkyObject {
    public final String catalogName;
    public final String objectType;
    public final String commonNames;
    public final String messierName;

    public final double rightAscensionHours;
    public final double declinationDegrees;

    public final Double visualMagnitude;
    public final Double majorAxisArcminutes;
    public final Double minorAxisArcminutes;
    public final Double surfaceBrightness;

    /**
     * Creates a catalog object from validated fields
     * @param catalogName OpenNGC identifier
     * @param objectType OpenNGC classification code
     * @param commonNames common names, or null
     * @param messierName Messier identifier, or null
     * @param rightAscensionHours J2000 right ascension in hours
     * @param declinationDegrees J2000 declination in degrees
     * @param visualMagnitude apparent V magnitude, or null
     * @param majorAxisArcminutes major angular diameter, or null
     * @param minorAxisArcminutes minor angular diameter, or null
     * @param surfaceBrightness galaxy B surface brightness in magnitudes per square arcsecond, or null
     */
    public DeepSkyObject(String catalogName, String objectType, String commonNames, String messierName,
            double rightAscensionHours, double declinationDegrees, Double visualMagnitude,
            Double majorAxisArcminutes, Double minorAxisArcminutes, Double surfaceBrightness) {
        this.catalogName = catalogName;
        this.objectType = objectType;
        this.commonNames = commonNames;
        this.messierName = messierName;

        this.rightAscensionHours = rightAscensionHours;
        this.declinationDegrees = declinationDegrees;

        this.visualMagnitude = visualMagnitude;
        this.majorAxisArcminutes = majorAxisArcminutes;
        this.minorAxisArcminutes = minorAxisArcminutes;
        this.surfaceBrightness = surfaceBrightness;
    }

    /**
     * Returns a name for the object list
     * @return common name or catalog identifier
     */
    public String getDisplayName() {
        if (commonNames != null) {
            return commonNames.split(",", 2)[0] + " | " + catalogName;
        }

        if (messierName != null) {
            return messierName + " | " + catalogName;
        }

        return catalogName;
    }

    /**
     * Returns names for searches with or without catalog padding
     * @return catalog identifiers and common names
     */
    public String getSearchNames() {
        int prefixLength = 0;
        while (prefixLength < catalogName.length() && !Character.isDigit(catalogName.charAt(prefixLength))) {
            prefixLength++;
        }

        int numberStart = prefixLength;
        while (numberStart < catalogName.length() && catalogName.charAt(numberStart) == '0') {
            numberStart++;
        }

        String alias = catalogName.substring(0, prefixLength) + catalogName.substring(numberStart);

        return catalogName + " " + alias + " " + Objects.toString(messierName, "")
                + " " + Objects.toString(commonNames, "");
    }
}
