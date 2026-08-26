package gov.takx.api.globe;

/**
 * Represents the style of interaction between the mouse and the globe.
 * <ol>
 *     <li>TAKX Style - left button pans, right button tilt-rotates, middle button zooms</li>
 *     <li>WinTAK Style - left button pans, right button rotates, middle button tilts</li>
 * </ol>
 *
 * @since 5.6
 */
public enum GlobeNavigationStyle
{
    TAKX("TAKX"), WinTAK("WinTAK");

    private final String name;

    GlobeNavigationStyle(String name)
    {
        this.name = name;
    }

    /**
     * @return the display name of this navigation style
     */
    public String getName()
    {
        return name;
    }

    @Override
    public String toString()
    {
        return name;
    }
}