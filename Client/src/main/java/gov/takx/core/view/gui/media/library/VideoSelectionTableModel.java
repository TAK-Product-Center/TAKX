package gov.takx.core.view.gui.media.library;

import gov.tak.api.comms.takserver.mission.data.Feed;
import jakarta.annotation.Nonnull;

import javax.swing.table.AbstractTableModel;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Table model holding a list of {@link Feed video feed} elements, allowing the user to select individual entries (rows).
 *
 * @since 5.0
 */
public class VideoSelectionTableModel extends AbstractTableModel
{
    // Model columns
    enum Column
    {
        SELECTED,
        THUMBNAIL,
        NAME_AND_URL;

        static final Column[] values = values();
    }

    private static final Comparator<ModelEntry> BY_ALIAS = Comparator.comparing(ModelEntry::alias, String.CASE_INSENSITIVE_ORDER);

    // List of our model entries
    private final List<ModelEntry> entries = new CopyOnWriteArrayList<>();

    /**
     * Add the given feed list to the model.
     */
    synchronized void addFeeds(@Nonnull List<Feed> feeds)
    {
        feeds.stream()
                .filter(feed -> feed.alias() != null && feed.url() != null)
                .map(ModelEntry::new)
                .forEach(entries::add);

        entries.sort(BY_ALIAS);

        fireTableDataChanged();
    }

    /**
     * @return Video at the given row index
     */
    @Nonnull
    public Feed getFeed(int rowIndex)
    {
        return entries.get(rowIndex).feed;
    }

    /**
     * @return A list of all the selected video feeds, may be empty.
     */
    @Nonnull
    public List<Feed> getSelectedFeeds()
    {
        return entries.stream()
                .filter(entry -> entry.selected)
                .map(entry -> entry.feed)
                .toList();
    }

    @Override
    public int getRowCount()
    {
        return entries.size();
    }

    @Override
    public int getColumnCount()
    {
        return Column.values.length;
    }

    @Override
    public Class<?> getColumnClass(int columnIndex)
    {
        return columnIndex == Column.SELECTED.ordinal() ? Boolean.class : String.class;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex)
    {
        final var modelEntry = entries.get(rowIndex);

        return switch (Column.values[columnIndex])
        {
            case SELECTED -> modelEntry.selected;
            case THUMBNAIL -> modelEntry.feed.thumbnail();
            case NAME_AND_URL -> modelEntry.feed;
        };
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex)
    {
        if (columnIndex == Column.SELECTED.ordinal()) entries.get(rowIndex).selected = (Boolean) aValue;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex)
    {
        return columnIndex == Column.SELECTED.ordinal();
    }

    /**
     * Our model entry.  Holds a {@link Feed} and a selected state.
     */
    private static class ModelEntry
    {
        private final Feed feed;
        boolean selected = false;

        private ModelEntry(Feed feed)
        {
            this.feed = feed;
        }

        private String alias()
        {
            return feed.alias();
        }
    }
}
