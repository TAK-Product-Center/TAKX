package gov.takx.core.placemarkoverlay;

import javax.swing.table.DefaultTableModel;

/**
 */
public class PlacemarkOverlayPropertiesModel extends DefaultTableModel
{
    private boolean editable;

    public enum COLUMNS
    {
        key,
        value
    }

    public PlacemarkOverlayPropertiesModel()
    {
        setEditable(true);
        for (COLUMNS nextColumn : COLUMNS.values())
        {
            addColumn(nextColumn);
        }
    }

    public void setEditable(boolean editable)
    {
        this.editable = editable;
    }

    public boolean isEditable()
    {
        return editable;
    }

    @Override
    public boolean isCellEditable(int row, int column)
    {
        return editable && column != COLUMNS.key.ordinal();
    }
}
