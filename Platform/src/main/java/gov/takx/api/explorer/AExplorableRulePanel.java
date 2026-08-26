package gov.takx.api.explorer;

import gov.takx.commons.swing.IPanelStateListener;

import javax.swing.JPanel;

/**
 * Abstract base class implementation of the {@link IExplorableRuleViewComponent} interface.
 *
 * @since 1.48
 */
public abstract class AExplorableRulePanel extends JPanel implements IExplorableRuleViewComponent
{
    private IPanelStateListener listener;

    public AExplorableRulePanel()
    {
        listener = null;
    }

    @Override
    public JPanel getPanel()
    {
        return this;
    }

    @Override
    public void setPanelListener(IPanelStateListener listener)
    {
        this.listener = listener;
    }

    @Override
    public String validateContents()
    {
        return null;
    }

    /**
     * Convenience method for extending classes to send a dirty notification to the panel state listener.
     */
    protected void setDirty()
    {
        if (listener != null)
        {
            listener.setPanelDirty();
        }
    }
}
