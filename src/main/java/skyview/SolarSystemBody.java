package skyview;

/** Solar system targets observable from Earth, using Horizons planet center identifiers */
public enum SolarSystemBody {
    SUN("10", "Sun"),
    MOON("301", "Moon"),
    MERCURY("199", "Mercury"),
    VENUS("299", "Venus"),
    MARS("499", "Mars"),
    JUPITER("599", "Jupiter"),
    SATURN("699", "Saturn"),
    URANUS("799", "Uranus"),
    NEPTUNE("899", "Neptune");

    public final String bodyId;
    public final String displayName;

    SolarSystemBody(String bodyId, String displayName) {
        this.bodyId = bodyId;
        this.displayName = displayName;
    }

}
