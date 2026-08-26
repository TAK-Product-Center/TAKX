package gov.takx.core.placemarkoverlay.gui;

import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.core.placemarkoverlay.PlacemarkOverlayPropertiesModel;

import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.TableColumn;

/**
 */
public class KeyValueTable extends JTable
{
    private final TableColumn valueColumn;

    public KeyValueTable()
    {
        super(new PlacemarkOverlayPropertiesModel());

        setTableHeader(null);
        setColumnSelectionAllowed(false);
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setShowHorizontalLines(false);
        setShowVerticalLines(false);

        TableColumn keyColumn = getColumnModel().getColumn(PlacemarkOverlayPropertiesModel.COLUMNS.key.ordinal());
        keyColumn.setCellRenderer(new PlacemarkKeyFieldRenderer());
        keyColumn.setPreferredWidth(100);
        keyColumn.setMaxWidth(100);

        valueColumn = getColumnModel().getColumn(PlacemarkOverlayPropertiesModel.COLUMNS.value.ordinal());
        valueColumn.setCellRenderer(new PlacemarkValueFieldRenderer());
        valueColumn.setCellEditor(new PlacemarkValueFieldEditor());
        keyColumn.setPreferredWidth(200);
        keyColumn.setMaxWidth(200);
    }

    /**
     * TODO: hacky, whole placemark dialog stuff is kind of hacky, may want to revisit it at some point and do it properly when theres some moola.
     * This is just a quick fix to have the text field display as a label when editing of values are disabled  in
     * placemark property panel.
     */
    @InvokeOnEDT
    public void setEditEnabled(final boolean enable)
    {
        ((PlacemarkValueFieldRenderer) valueColumn.getCellRenderer()).setEnabled(enable);
        repaint();
    }
}
