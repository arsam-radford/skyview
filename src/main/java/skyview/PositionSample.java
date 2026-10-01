package skyview;

import java.time.Instant;

/** One calculated solar system position. *readers* validate values and units */
public class PositionSample {
    private final String bodyId;
    private final Instant time;
    private final double azimuthDegrees;
    private final double altitudeDegrees;
    private final Double apparentMagnitude;
    private final Double illuminatedPercent;

    /**
     * Creates a position sample.
     *
     * @param bodyId JPL target identifier
     * @param time UTC sample time
     * @param azimuthDegrees azimuth clockwise from north in [0, 360) degrees
     * @param altitudeDegrees elevation above the horizon in [-90, 90] degrees
     * @param apparentMagnitude apparent magnitude, or null
     * @param illuminatedPercent illuminated disk percentage in [0, 100], or null
     */
    public PositionSample(String bodyId, Instant time, double azimuthDegrees,
            double altitudeDegrees, Double apparentMagnitude, Double illuminatedPercent) {
        this.bodyId = bodyId;
        this.time = time;
        this.azimuthDegrees = azimuthDegrees;
        this.altitudeDegrees = altitudeDegrees;
        this.apparentMagnitude = apparentMagnitude;
        this.illuminatedPercent = illuminatedPercent;
    }

    /**
     * Returns JPL target identifier.
     *
     * @return JPL target identifier
     */
    public String getBodyId() {
        return bodyId;
    }

    /**
     * Returns UTC sample time.
     *
     * @return UTC sample time
     */
    public Instant getTime() {
        return time;
    }

    /**
     * Returns azimuth clockwise from north in [0, 360) degrees.
     *
     * @return azimuth clockwise from north in [0, 360) degrees
     */
    public double getAzimuthDegrees() {
        return azimuthDegrees;
    }

    /**
     * Returns elevation above the horizon in [-90, 90] degrees.
     *
     * @return elevation above the horizon in [-90, 90] degrees
     */
    public double getAltitudeDegrees() {
        return altitudeDegrees;
    }

    /**
     * Returns apparent magnitude, or null.
     *
     * @return apparent magnitude, or null
     */
    public Double getApparentMagnitude() {
        return apparentMagnitude;
    }

    /**
     * Returns illuminated disk percentage in [0, 100], or null.
     *
     * @return illuminated disk percentage in [0, 100], or null
     */
    public Double getIlluminatedPercent() {
        return illuminatedPercent;
    }
}
