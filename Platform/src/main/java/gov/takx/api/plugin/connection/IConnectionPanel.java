package gov.takx.api.plugin.connection;

import gov.takx.api.plugin.IRaptorPanel;
import gov.takx.commons.swing.IValidateAction;

import java.util.Map;

/**
 * Marker interface that distinguishes a panel as a Connection Plugin panel.
 *
 */
public interface IConnectionPanel extends IRaptorPanel, IValidateAction
{
    /**
     * Configuration map passed into the connection panel just after instantiation.
     *
     * @param configuration configuration map.
     * @since 1.14
     */
    void setConfiguration(Map<String, Object> configuration);
}
