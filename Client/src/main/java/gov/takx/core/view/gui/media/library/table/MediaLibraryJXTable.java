package gov.takx.core.view.gui.media.library.table;

import gov.takx.api.state.IRestorableMap;
import gov.takx.api.state.JSONMapUtils;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.renderer.JRendererLabel;
import org.jdesktop.swingx.table.DefaultTableColumnModelExt;
import org.jdesktop.swingx.table.TableColumnExt;

import javax.swing.JComponent;
import javax.swing.SortOrder;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import java.awt.Color;
import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * JXTable extension designed to handle the displaying of the creation time and color columns, as well as the tooltips.
 * <p>
 * The color column needs to contain empty cells with the background color set to the value specified.  The creation
 * time column contains a zoned time for when the file or stream reference was created.  The renderer displays the
 * date, time, and zone information.
 *
 * @since 2.37
 */
public class MediaLibraryJXTable extends JXTable
{
    private static final String MEDIALIBRARY_MAP_KEY = "headers";
    private static final String HEADER_KEY_VISIBLE = "visible";
    private static final String HEADER_SEQUENCE = "sequence";
    private static final String HEADER_KEY_WIDTH = "width";
    private static final String HEADER_SORT_COLUMN = "sort column";
    private static final String HEADER_SORT_ORDER = "sort order";

    @Override
    public Component prepareRenderer(TableCellRenderer renderer, int row, int column)
    {
        final Component component = super.prepareRenderer(renderer, row, column);

        int modelRowIdx = convertRowIndexToModel(row);
        int modelColumnIndex = getColumn(column).getModelIndex();
        Object columnValue = getModel().getValueAt(modelRowIdx, modelColumnIndex);
        if (columnValue instanceof Color color)
        {
            ((JRendererLabel) component).setText("");
            component.setBackground(color);
        }

        return component;
    }

    /**
     * Create tooltip for display, accounting for clipping
     *
     * @param event Event for mouse hovering over a cell
     * @return Tooltip to display
     */
    @Override
    public String getToolTipText(MouseEvent event)
    {
        Point p = event.getPoint();
        String tip = null;

        // Locate the renderer under the event location
        int hitRowIndex = rowAtPoint(p);
        int hitColumnIndex = columnAtPoint(p);

        final boolean validCell = (hitColumnIndex != -1) && (hitRowIndex != -1);

        if (validCell)
        {
            TableCellRenderer renderer = getCellRenderer(hitRowIndex, hitColumnIndex);
            Component component = prepareRenderer(renderer, hitRowIndex, hitColumnIndex);

            // Now have to see if the component is a JComponent before getting the tip
            if (component instanceof JComponent jcomponent)
            {
                // If the preferred size is larger than the real size, then it is clipped
                Rectangle cellRect = getCellRect(hitRowIndex, hitColumnIndex, false);
                if (cellRect.width >= component.getPreferredSize().width)
                {
                    return null;
                }

                p.translate(-cellRect.x, -cellRect.y);

                // Get tooltip text from renderer
                tip = jcomponent.getToolTipText(new MouseEvent(component, event.getID(),
                        event.getWhen(), event.getModifiers(),
                        p.x, p.y, event.getClickCount(),
                        event.isPopupTrigger()));
            }
        }

        // No tip from the renderer, see whether any tooltip is set on JTable
        if (tip == null)
        {
            tip = getToolTipText();
        }

        // calculate tooltip from cell value
        if (tip == null && validCell)
        {
            tip = convertValueToText(getValueAt(hitRowIndex, hitColumnIndex));
        }

        if (tip != null && tip.isEmpty())
        {
            tip = null; // don't show empty tooltips
        }

        return tip;
    }

    /**
     * Returns the toString representation of the object, or an empty string if the toString representation is {@code null}
     *
     * @param value Object whose toString representation is requested
     * @return toString representation of the object, or an empty string if the toString representation is {@code null}
     */
    private String convertValueToText(Object value)
    {
        return value == null ? "" : value.toString();
    }

    /**
     * Save the library table state.
     *
     * @see #restoreFromMap(IRestorableMap)
     */
    public void getRestorableMap(IRestorableMap rootMap)
    {
        // map of Explorers
        final IRestorableMap mediaLibraryMap = JSONMapUtils.createNewRestorableMap();

        // add explorers map to root map
        rootMap.setRestorableMap(MEDIALIBRARY_MAP_KEY, mediaLibraryMap);

        // Restore all Media Library Headers
        final DefaultTableColumnModelExt tcm = (DefaultTableColumnModelExt) getColumnModel();

        final List<TableColumn> columns = getColumns();
        final List<String> columnSequence = new ArrayList<>();
        columns.forEach(tc -> {
            String title = ((TableColumnExt) tc).getTitle();
            columnSequence.add(title);
        });
        rootMap.setStringList(HEADER_SEQUENCE, columnSequence);

        final TableColumnExt sortColumn = (TableColumnExt) getSortedColumn();
        if (sortColumn != null)
        {
            rootMap.setString(HEADER_SORT_COLUMN, sortColumn.getTitle());
            final int sortIdx = getSortedColumnIndex();
            final SortOrder sortOrder = getSortOrder(sortIdx);
            rootMap.setString(HEADER_SORT_ORDER, sortOrder.name());
        }

        for (TableColumn tc : tcm.getColumns(true))
        {
            TableColumnExt ce = (TableColumnExt) tc;

            IRestorableMap state = JSONMapUtils.createNewRestorableMap();
            state.setBoolean(HEADER_KEY_VISIBLE, ce.isVisible());
            state.setInt(HEADER_KEY_WIDTH, ce.getWidth());

            mediaLibraryMap.setRestorableMap(ce.getTitle(), state);
        }
    }

    /**
     * Restore the state of the table
     *
     * @see #getRestorableMap(IRestorableMap)
     */
    public void restoreFromMap(IRestorableMap rootMap)
    {
        if (rootMap == null) return;

        String sortColumn = rootMap.getString(HEADER_SORT_COLUMN, null);
        if (sortColumn != null)
        {
            String sortOrder = rootMap.getString(HEADER_SORT_ORDER, null);
            setSortOrder(sortColumn, SortOrder.valueOf(sortOrder));
        }

        List<String> columnSequence = rootMap.getStringList(HEADER_SEQUENCE, null);
        if (columnSequence != null)
        {
            setColumnSequence(columnSequence.toArray());
        }

        final IRestorableMap headersMap = rootMap.getRestorableMap(MEDIALIBRARY_MAP_KEY, null);
        if (headersMap == null) return;
        DefaultTableColumnModelExt tcm = (DefaultTableColumnModelExt) getColumnModel();
        headersMap.getKeys().forEach(s -> {
            TableColumnExt cm = tcm.getColumnExt(s);
            IRestorableMap m = headersMap.getRestorableMap(s, null);

            boolean viz = m.getBoolean(HEADER_KEY_VISIBLE, false);
            cm.setVisible(viz);

            int w = m.getInt(HEADER_KEY_WIDTH, cm.getPreferredWidth());
            cm.setPreferredWidth(w);
        });
    }
}
