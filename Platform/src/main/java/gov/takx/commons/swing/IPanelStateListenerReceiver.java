package gov.takx.commons.swing;

/**
 */
public interface IPanelStateListenerReceiver
{
    /**
     * Inject the panel listener that should be notified when a value on the panel has been updated so that the dialog
     * is properly marked as clean or dirty, indirectly controlling whether the Apply button is enabled or disabled.
     *
     * @param listener register a panel listener
     */
    void setPanelListener(IPanelStateListener listener);
}
