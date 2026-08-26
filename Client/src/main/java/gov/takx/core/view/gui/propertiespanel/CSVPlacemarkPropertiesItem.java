package gov.takx.core.view.gui.propertiespanel;

import gov.takx.core.placemarkoverlay.CSVPlaceMark;

import javax.swing.JPanel;

/**
 */
public class CSVPlacemarkPropertiesItem extends APropertiesItem
{
    private final CSVPlaceMark placeMark;

    public CSVPlacemarkPropertiesItem(CSVPlaceMark placeMark)
    {
        super("CSV Placemark Properties Item");
        this.placeMark = placeMark;
    }

    @Override
    public JPanel getPanel()
    {
        return placeMark.getPropertiesPanel();
    }

    @Override
    public String getLabelText()
    {
        return "Placemark: " + placeMark;
    }

    @Override
    public Object getIdentityObject()
    {
        return placeMark;
    }

    @Override
    public boolean isDialogOpen()
    {
        return false;
    }
}
