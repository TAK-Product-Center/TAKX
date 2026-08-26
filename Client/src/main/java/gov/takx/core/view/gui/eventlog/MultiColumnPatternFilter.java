package gov.takx.core.view.gui.eventlog;

import gov.takx.core.eventlog.EventLogTableModel;
import org.jdesktop.swingx.JXTable;

import javax.swing.RowFilter;
import javax.swing.table.TableColumn;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Filter to match rows in the table against a configured pattern.  We only match against visible rows.
 *
 */
public class MultiColumnPatternFilter extends RowFilter<EventLogTableModel, Integer>
{
    private Matcher matcher;
    private JXTable table;

    public MultiColumnPatternFilter(String regularExpr, JXTable table)
    {
        matcher = Pattern.compile(regularExpr, Pattern.CASE_INSENSITIVE).matcher("");
        this.table = table;
    }

    /**
     * Scan all the visible columns and match against the configured pattern.
     * <p/>
     * {@inheritDoc}
     */
    @Override
    public boolean include(Entry<? extends EventLogTableModel, ? extends Integer> entry)
    {
        for (TableColumn col : table.getColumns(false))
        {
            int modelIndex = col.getModelIndex();

            // We don't want to search for the icon source. We want to search for the Map Object name instead.
            if (modelIndex == EventLogTableModel.ColumnNames.SOURCE.ordinal())
            {
                modelIndex = EventLogTableModel.ColumnNames.MAP_OBJECT_NAME.ordinal();
            }

            final String value = entry.getStringValue(modelIndex);
            matcher.reset(value);

            final boolean matched = matcher.find();
            if (matched) return true;
        }
        return false;
    }
}
