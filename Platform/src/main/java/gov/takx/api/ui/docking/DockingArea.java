package gov.takx.api.ui.docking;

import gov.takx.api.annotation.Experimental;

/**
 */
@Experimental
public enum DockingArea
{
    CENTER("center"),
    NORTH("north"),
    SOUTH("south"),
    EAST("east"),
    WEST("west"),
    FLOATING("");

    private String dockingAreaId;

    DockingArea(String dockingAreaId)
    {
        this.dockingAreaId = dockingAreaId;
    }

    public String getDockingAreaId()
    {
        return dockingAreaId;
    }
}
