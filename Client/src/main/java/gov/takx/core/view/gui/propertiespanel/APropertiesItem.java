package gov.takx.core.view.gui.propertiespanel;

import gov.takx.api.ui.IResourceCleaner;

import javax.swing.JPanel;

/**
 */
public abstract class APropertiesItem implements IPropertiesItem
{
    private JPanel panel;
    private String labelText;
    private Object identityObject;
    private final String type;

    protected APropertiesItem(String type)
    {
        this.type = type;
    }

    protected APropertiesItem(JPanel panel, String labelText, Object identityObject, String type)
    {
        this.panel = panel;
        this.labelText = labelText;
        this.identityObject = identityObject;
        this.type = type;
    }

    @Override
    public void releaseResources()
    {
        if (panel instanceof IResourceCleaner resourceCleaner) resourceCleaner.releaseResources();
    }

    @Override
    public JPanel getPanel()
    {
        return panel;
    }

    @Override
    public String getLabelText()
    {
        return labelText;
    }

    @Override
    public Object getIdentityObject()
    {
        return identityObject;
    }

    @Override
    public String getType()
    {
        return type;
    }
}
