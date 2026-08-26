package gov.takx.core.view.gui.eventlog;

import gov.takx.api.eventlog.EventLogType;
import gov.takx.core.eventlog.EventLogTableModel;

import javax.swing.RowFilter;

import java.util.ArrayList;
import java.util.List;

/**
 * This table filter is responsible for filtering out entries that are excluded by the current event type selections.
 *
 */
public class EventLogFilter extends RowFilter<EventLogTableModel, Integer>
{
    private final List<EventLogType> activeFilters;

    /**
     * @param activeFilters The list of active filters (those to be allowed)
     */
    EventLogFilter(List<EventLogType> activeFilters)
    {
        this.activeFilters = new ArrayList<>(activeFilters);
    }

    /**
     * Determine if the given row should be displayed.
     * <p/>
     * {@inheritDoc}
     */
    @Override
    public boolean include(Entry<? extends EventLogTableModel, ? extends Integer> entry)
    {
        // Check the filters first (since they are large scale cullers)
        final EventLogType type = (EventLogType) entry.getValue(EventLogTableModel.ColumnNames.TYPE.ordinal());

        return activeFilters.contains(type); // Not active, filter it out
    }
}
