package gov.takx.api.explorer;

import gov.takx.commons.swing.IPanelStateListenerReceiver;
import gov.takx.commons.swing.IValidateAction;

import javax.swing.JPanel;

/**
 * Interface for explorable rule view component.
 *
 * @since 1.48
 */
public interface IExplorableRuleViewComponent extends IValidateAction, IPanelStateListenerReceiver
{
    /**
     * Get the panel for this view component.
     *
     * @return the panel for this view component.
     */
    JPanel getPanel();

    /**
     * Apply any changes made in this view component to its explorable rule.
     */
    void applyAction();
}
