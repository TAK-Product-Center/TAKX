package gov.takx.commons.swing;

import javax.swing.JButton;

/**
 */
public class PanelListener implements IPanelStateListener
{
    private final JButton applyButton;

    public PanelListener(JButton applyButton)
    {
        this.applyButton = applyButton;
    }

    @Override
    public void setPanelDirty()
    {
        applyButton.setEnabled(true);
    }

    @Override
    public void setPanelClean()
    {
        applyButton.setEnabled(false);
    }
}
