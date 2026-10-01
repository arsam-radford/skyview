package skyview;

/** One HYG catalog star, readers validate required coordinates and brightness. */
public class Star {
    private final int hygId;
    private final Integer hipId;
    private final Integer hdId;
    private final String displayName;
    private final double rightAscensionHours;
    private final double declinationDegrees;
    private final double apparentMagnitude;
    private final String spectralType;
    private final Double colorIndex;

    /**
     * Creates a catalog star.
     *
     * @param hygId HYG catalog identifier
     * @param hipId Hipparcos identifier, or null
     * @param hdId Henry Draper identifier, or null
     * @param displayName proper name or a catalog identifier when unnamed
     * @param rightAscensionHours J2000 right ascension in hours
     * @param declinationDegrees J2000 declination in degrees
     * @param apparentMagnitude visual magnitude; smaller is brighter
     * @param spectralType spectral classification, or null
     * @param colorIndex B-V color index, or null
     */
    public Star(int hygId, Integer hipId, Integer hdId, String displayName,
            double rightAscensionHours, double declinationDegrees, double apparentMagnitude,
            String spectralType, Double colorIndex) {
        this.hygId = hygId;
        this.hipId = hipId;
        this.hdId = hdId;
        this.displayName = displayName;
        this.rightAscensionHours = rightAscensionHours;
        this.declinationDegrees = declinationDegrees;
        this.apparentMagnitude = apparentMagnitude;
        this.spectralType = spectralType;
        this.colorIndex = colorIndex;
    }

    /**
     * Returns HYG catalog identifier.
     *
     * @return HYG catalog identifier
     */
    public int getHygId() {
        return hygId;
    }

    /**
     * Returns Hipparcos identifier, or null.
     *
     * @return Hipparcos identifier, or null
     */
    public Integer getHipId() {
        return hipId;
    }

    /**
     * Returns Henry Draper identifier, or null.
     *
     * @return Henry Draper identifier, or null
     */
    public Integer getHdId() {
        return hdId;
    }

    /**
     * Returns proper name or a catalog identifier when unnamed.
     *
     * @return proper name or a catalog identifier when unnamed
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Returns J2000 right ascension in hours.
     *
     * @return J2000 right ascension in hours
     */
    public double getRightAscensionHours() {
        return rightAscensionHours;
    }

    /**
     * Returns J2000 declination in degrees.
     *
     * @return J2000 declination in degrees
     */
    public double getDeclinationDegrees() {
        return declinationDegrees;
    }

    /**
     * Returns visual magnitude; smaller is brighter.
     *
     * @return visual magnitude; smaller is brighter
     */
    public double getApparentMagnitude() {
        return apparentMagnitude;
    }

    /**
     * Returns spectral classification, or null.
     *
     * @return spectral classification, or null
     */
    public String getSpectralType() {
        return spectralType;
    }

    /**
     * Returns B-V color index, or null.
     *
     * @return B-V color index, or null
     */
    public Double getColorIndex() {
        return colorIndex;
    }
}
