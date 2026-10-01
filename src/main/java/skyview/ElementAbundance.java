package skyview;

public class ElementAbundance {
    private final String starName;
    private final String element;
    private final String solarNormalization;
    private final Double medianAbundanceDex;

    /**
     * Creates a stellar abundance result.
     *
     * @param starName source's returned star name
     * @param element chemical element label
     * @param solarNormalization reference scale identifier
     * @param medianAbundanceDex median element-to-hydrogen abundance relative to the Sun in dex, or null
     */
    public ElementAbundance(String starName, String element, String solarNormalization,
            Double medianAbundanceDex) {
        this.starName = starName;
        this.element = element;
        this.solarNormalization = solarNormalization;
        this.medianAbundanceDex = medianAbundanceDex;
    }

    /**
     * Returns source's returned star name.
     *
     * @return source's returned star name
     */
    public String getStarName() {
        return starName;
    }

    /**
     * Returns chemical element label.
     *
     * @return chemical element label
     */
    public String getElement() {
        return element;
    }

    /**
     * Returns reference scale identifier.
     *
     * @return reference scale identifier
     */
    public String getSolarNormalization() {
        return solarNormalization;
    }

    /**
     * Returns median element-to-hydrogen abundance relative to the Sun in dex, or null.
     *
     * @return median element-to-hydrogen abundance relative to the Sun in dex, or null
     */
    public Double getMedianAbundanceDex() {
        return medianAbundanceDex;
    }
}
