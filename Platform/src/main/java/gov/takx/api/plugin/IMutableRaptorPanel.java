package gov.takx.api.plugin;

import gov.takx.commons.swing.IValidateAction;

/**
 * Mutable panel that supports a commit event but does not support field-level validation prior to issuing
 * the commit.
 *
 * @since 1.4
 */
public interface IMutableRaptorPanel extends IRaptorPanel, IValidateAction
{
    /**
     * Commit the changes to the panel.
     */
    void applyAction();
}
