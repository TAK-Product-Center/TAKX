package gov.takx.api.ui;

import gov.takx.api.plugin.connection.IConnectionPanel;
import gov.takx.api.plugin.connection.IConnectionPluginController;

/**
 * Interface for consuming a connection UI that is produced by an {@link IConnectionSwingUIProvider}.  The
 * connection controller must implement this interface to have the correct UI injected into it so that user
 * supplied connection parameters can be pulled out.
 *
 * @see IConnectionPluginController
 * @since 2.0.0
 */
public interface IConnectionSwingUIConsumer
{
    /**
     * Injection point of the connection UI panel.  Can be cast to the plugin defined connection panel.
     *
     * @param connectionPanel the connection panel
     */
    void setConnectionPanel(IConnectionPanel connectionPanel);
}
