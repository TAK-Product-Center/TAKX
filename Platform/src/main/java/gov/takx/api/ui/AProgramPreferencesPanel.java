package gov.takx.api.ui;

import gov.takx.commons.swing.IPanelStateListener;

import javax.swing.JPanel;

/**
 * This abstract preferences panel allows a no-arg constructor. This lets it meet the Swing beans requirement so that
 * the panels extending it can be used in JFormDesigner as a custom component.
 *
 */
public abstract class AProgramPreferencesPanel extends JPanel implements IProgramPreferencesPanel
{
    private IPanelStateListener panelListener;

    @Override
    public void setPanelListener(IPanelStateListener panelListener)
    {
        this.panelListener = panelListener;
    }

    @Override
    public IPanelStateListener getPanelListener()
    {
        return panelListener;
    }
}
