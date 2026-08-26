package gov.takx.api.plugin;

import javax.swing.JPanel;

import java.net.URL;

/**
 * Interface for plugin panels.
 *
 */

public interface IRaptorPanel
{
    /**
     * Return the panel to be displayed in the map object dialog.
     *
     * @return the panel
     */
    JPanel getPanel();

    /**
     * Return the name to be used for the panel in the map object dialog
     *
     * @return String panel name
     */
    String getTitle();

    /**
     * Return the icon associated with this panel. If no icon is provided (return value is {@code null}),
     * a default icon will be used.
     *
     * @return The {@link java.net.URL} pointing at the icon associated with the panel. May be {@code null}.
     */
    URL getIcon();
}
