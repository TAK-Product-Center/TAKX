package gov.takx.platform.toolbar;

/**
 * Enum representing the location of the button text for the toolbar buttons.
 *
 * @since 3.4
 */
public enum ToolbarButtonLabelLocation
{
    NONE("None"),
    BOTTOM("Bottom"),
    RIGHT("Right");

    private final String name;

    /**
     * Constructor.
     * @param name Human-readable name for the literal
     */
    ToolbarButtonLabelLocation(String name)
    {
        this.name = name;
    }

    @Override
    public String toString()
    {
        return name;
    }
}
