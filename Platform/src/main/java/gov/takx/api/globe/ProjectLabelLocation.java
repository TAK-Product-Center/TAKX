package gov.takx.api.globe;

/**
 * Defines the six predefined locations for static project text labels to display above the globe.
 *
 * @since 1.4
 */
public enum ProjectLabelLocation
{
    UPPER_CENTER("UpperCenterLabel"),
    LOWER_CENTER("LowerCenterLabel"),
    UPPER_LEFT("UpperLeftLabel"),
    UPPER_RIGHT("UpperRightLabel"),
    LOWER_RIGHT("LowerRightLabel"),
    LOWER_LEFT("LowerLeftLabel");

    private final String locationName;

    ProjectLabelLocation(String locationName)
    {
        this.locationName = locationName;
    }

    @Override
    public String toString()
    {
        return locationName;
    }
}
