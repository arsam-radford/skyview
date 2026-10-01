package skyview;

/** earth coords, east longitude is positive, west longitude is negative */
public class ObserverLocation {
    private static final double MAX_LATITUDE_DEGREES = 90.0;
    private static final double MAX_LONGITUDE_DEGREES = 180.0;
    private final double latitudeDegrees;
    private final double longitudeDegrees;
    private final double elevationMeters;

    /**
     * Creates an observer location.
     *
     * @param latitudeDegrees latitude between -90 and 90 degrees
     * @param longitudeDegrees longitude between -180 and 180 degrees
     * @param elevationMeters finite height above the WGS-84 reference ellipsoid in meters
     * @throws IllegalArgumentException if a value is nonfinite or outside its range
     */
    public ObserverLocation(double latitudeDegrees, double longitudeDegrees,
            double elevationMeters) {
        if (!Double.isFinite(latitudeDegrees)
                || Math.abs(latitudeDegrees) > MAX_LATITUDE_DEGREES
                || !Double.isFinite(longitudeDegrees)
                || Math.abs(longitudeDegrees) > MAX_LONGITUDE_DEGREES
                || !Double.isFinite(elevationMeters)) {
            throw new IllegalArgumentException("Invalid observer coordinates or elevation.");
        }
        this.latitudeDegrees = latitudeDegrees;
        this.longitudeDegrees = longitudeDegrees;
        this.elevationMeters = elevationMeters;
    }

    /**
     * Returns latitude in degrees.
     *
     * @return latitude in degrees
     */
    public double getLatitudeDegrees() {
        return latitudeDegrees;
    }

    /**
     * Returns longitude in degrees, positive east.
     *
     * @return longitude in degrees, positive east
     */
    public double getLongitudeDegrees() {
        return longitudeDegrees;
    }

    /**
     * Returns height above the WGS-84 reference ellipsoid in meters.
     *
     * @return height above the WGS-84 reference ellipsoid in meters
     */
    public double getElevationMeters() {
        return elevationMeters;
    }
}
