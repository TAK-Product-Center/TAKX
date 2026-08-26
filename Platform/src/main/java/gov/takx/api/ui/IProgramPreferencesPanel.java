package gov.takx.api.ui;

import gov.takx.commons.swing.IPanelStateListener;

/**
 * Contract for preference panels.  Implementations must be subclasses of {@link java.awt.Component}.
 */
public interface IProgramPreferencesPanel extends IResourceCleaner
{
    boolean applyAction();

    void restoreDefaults();

    void setPanelListener(IPanelStateListener panelListener);

    IPanelStateListener getPanelListener();

    /**
     * Mark the panel as dirty, which enables the "Apply" button.
     * @since 3.1 (moved default implementation into the interface)
     */
    default void setPanelDirty()
    {
        final IPanelStateListener listener = getPanelListener();
        if (listener != null) listener.setPanelDirty();
    }

    /**
     * Mark the panel as clean, which disables the "Apply" button.
     * @since 3.1 (moved default implementation into the interface)
     */
    default void setPanelClean()
    {
        final IPanelStateListener listener = getPanelListener();
        if (listener != null) listener.setPanelClean();
    }
}
