package gov.takx.api.ui;

/**
 * Identifies a panel's position on the main app frame.
 *
 */
public enum ComponentPosition
{
    /**
     * Tabs on top of the globe.  Tabs in this group will replace the globe view when active.
     */
    GLOBE("globe"),

    /**
     * Panels to the right of the primary globe. Similar fashion to the secondary globes
     */
    CENTER("center"),

    /**
     * Tabs to the left side of the main app frame.
     */
    LEFT("west"),

    /**
     * Tabs on the right side of the main app frame.
     */
    RIGHT("east"),

    /**
     * Tabs on the bottom of the main app frame.
     */
    BOTTOM("south");

    /**
     * The componentPositionId is used by the Docking Framework to locate the area of interest
     */
    private final String componentPositionId;

    ComponentPosition(String componentPositionId)
    {
        this.componentPositionId = componentPositionId;
    }

    public String getComponentPositionId()
    {
        return componentPositionId;
    }
}
