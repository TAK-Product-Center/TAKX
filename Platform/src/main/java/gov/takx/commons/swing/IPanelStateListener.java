package gov.takx.commons.swing;

/**
 */
public interface IPanelStateListener
{
    /**
     * Set the panel state to dirty (values have been changed by the user).  Configuration panels created for plugins
     * should call this method if they receive an event that the operator has changed a value of a control. The dialog
     * displaying the panel will enable its "Apply" button.
     */
    void setPanelDirty();

    /**
     * Set the panel state to clean (values are accurately persisted and unchanged by the user). Configuration panels
     * created for plugins should call this method if their data appear to be the same as the values most recently
     * applied. The dialog displaying the panel will then disable it's "Apply" button.
     * <p>
     * Note: If the host dialog can display multiple configuration panels (e.g. AppConfigurationDialog,
     * MapObjectConfigurationDialog), use this method with caution. If other panels are still dirty, calling this will
     * still disable the apply button.
     */
    void setPanelClean();
}
