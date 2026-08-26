package gov.takx.api.ui;

import gov.takx.api.plugin.connection.IConnectionPanel;

/**
 * Contract for how a connection plugin controller provides the needed Swing UI components for the TAKX
 * Swing UI expression (thick client).
 * <p/>
 * Note that unlike all other UI Provider types, connections are handled uniquely.  Each provider, for a given
 * plugin controller, will never be created more than once.
 *
 * @since 2.0.0
 */
public interface IConnectionSwingUIProvider
{
    /**
     * Returns custom connection management panel.  There should only be one implementation of
     * IConnectionPanel per IConnectionPluginController implementation.
     *
     * @return connection panel
     */
    IConnectionPanel getConnectionPanel();
}
