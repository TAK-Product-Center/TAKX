package gov.takx.api.ui.docking;

/**
 * The state of a dockable panel's tray button while the panel is minimized which will determine
 * the color and flashing behavior. All dock states will change the minimized dockable
 * tray's background color, but only the highest level states will flash. This behavior can be disabled
 * from the User Preferences panel.
 *
 * @since 2.24
 */
public enum DockState
{
    Default, Active, Warning, Critical;

    private static final DockState[] DOCK_STATES = values();

    /**
     * Converts the name of a DockState to its corresponding enum value. This method is case insensitive.
     *
     * @param name The identifier of a DockState
     * @return The identified DockState.
     */
    public static DockState stringToDockState(String name)
    {
        if (name != null)
        {
            for (DockState dockState : DOCK_STATES)
            {
                if (dockState.toString().equalsIgnoreCase(name))
                {
                    return dockState;
                }
            }
        }

        return Default;
    }
}
