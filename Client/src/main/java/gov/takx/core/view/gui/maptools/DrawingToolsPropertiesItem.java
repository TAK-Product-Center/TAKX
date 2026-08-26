package gov.takx.core.view.gui.maptools;

import gov.takx.core.view.gui.propertiespanel.APropertiesItem;

import javax.swing.JPanel;

/**
 */
public class DrawingToolsPropertiesItem extends APropertiesItem
{
    /**
     * Simple constructor defaulting identityObject to {@code null} and type to the label text.
     *
     * @param panel     The panel associated with this properties item
     * @param labelText The display name to use
     */
    public DrawingToolsPropertiesItem(JPanel panel, String labelText)
    {
        super(panel, labelText, null, labelText);
    }

    @Override
    public boolean isDialogOpen()
    {
        return false;
    }
}
