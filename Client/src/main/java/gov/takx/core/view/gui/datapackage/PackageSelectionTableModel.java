package gov.takx.core.view.gui.datapackage;

import gov.tak.api.comms.takserver.ITakServerConnection;
import gov.tak.api.comms.takserver.mission.data.AssociatedResource;
import gov.tak.api.comms.takserver.mission.data.Resource;
import jakarta.annotation.Nonnull;
import org.apache.commons.collections4.ListUtils;

import javax.swing.table.AbstractTableModel;

import java.time.Instant;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Table model holding a list of {@link AssociatedResource} elements, allowing the user to select individual entries (rows).
 * The model may also be sorted on any of file name, user, size, or date, ascending or descending.
 *
 * @since 3.1
 */
public class PackageSelectionTableModel extends AbstractTableModel
{
    // Model columns
    enum Column
    {
        SELECTED,
        NAME_AND_DATE,
        SIZE_AND_USER;

        static final Column[] values = values();
    }

    // Possible sort fields
    enum SortOn
    {
        Name, User, Size, Date
    }

    // Comparators for sorting the model by various fields
    private static final Comparator<ModelEntry> BY_NAME = Comparator.comparing(ModelEntry::name, String.CASE_INSENSITIVE_ORDER);
    private static final Comparator<ModelEntry> BY_USER = Comparator.comparing(ModelEntry::user, String.CASE_INSENSITIVE_ORDER);
    private static final Comparator<ModelEntry> BY_SIZE = Comparator.comparing(ModelEntry::size);
    private static final Comparator<ModelEntry> BY_DATE = Comparator.comparing(ModelEntry::instant);

    private static final Map<SortOn, Comparator<ModelEntry>> SORT_COMPARATORS = Map.of(
            SortOn.Name, BY_NAME,
            SortOn.User, BY_USER,
            SortOn.Size, BY_SIZE,
            SortOn.Date, BY_DATE);

    // List of our model entries
    private final List<ModelEntry> entries = new CopyOnWriteArrayList<>();

    // Set of file hashes already in our entry list, used to avoid duplicates
    private final Set<String> fileHashes = ConcurrentHashMap.newKeySet();

    private final Predicate<AssociatedResource> fileHashNotPresent = fileEntry -> !fileHashes.contains(fileEntry.hash());

    /**
     * Add the given file list to the model.
     */
    synchronized void addFileList(@Nonnull ITakServerConnection connection, @Nonnull List<AssociatedResource> resourceList)
    {
        resourceList.stream()
                .filter(fileHashNotPresent) // no duplicate hashes
                .map(fileResource -> new ModelEntry(connection, fileResource))
                .forEach(modelEntry -> {
                    fileHashes.add(modelEntry.hash());
                    entries.add(modelEntry);
                });
        fireTableDataChanged();
    }

    /**
     * Sort the model based on the given field.
     *
     * @param sortOn Field to sort on
     * @param ascending Pass {@code true} for ascending sort, {@code false} for descending
     */
    synchronized void sort(@Nonnull SortOn sortOn, boolean ascending)
    {
        final var comparator = SORT_COMPARATORS.get(sortOn);

        entries.sort(ascending ? comparator : comparator.reversed());
        fireTableDataChanged();
    }

    /**
     * @return Resource at the given row index
     */
    @Nonnull
    public AssociatedResource getFileEntry(int rowIndex)
    {
        return entries.get(rowIndex).fileResource;
    }

    /**
     * @return A list of all the selected file entries, mapped per server connection, may be empty.
     */
    @Nonnull
    public Map<ITakServerConnection, List<AssociatedResource>> getSelectedFileEntriesPerConnection()
    {
        return entries.stream()
                .filter(ModelEntry::isSelected)
                .collect(Collectors.toMap(e -> e.connection, e -> Collections.singletonList(e.fileResource),
                        ListUtils::union));
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

        switch (Column.values[columnIndex])
        {
            case SELECTED:
                return modelEntry.selected;
            case NAME_AND_DATE:
            case SIZE_AND_USER:
                return modelEntry.fileResource;
            default:
                return "";
        }
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex)
    {
        if (columnIndex == Column.SELECTED.ordinal())
        {
            entries.get(rowIndex).selected = (Boolean) aValue;
        }
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex)
    {
        return columnIndex == Column.SELECTED.ordinal();
    }

    /**
     * Our model entry.  Holds a {@link Resource} and a selected state.
     */
    private static class ModelEntry
    {
        private final ITakServerConnection connection;
        private final AssociatedResource fileResource;

        boolean selected = false;

        private ModelEntry(ITakServerConnection connection, AssociatedResource fileResource)
        {
            this.connection = connection;
            this.fileResource = fileResource;
        }

        private AssociatedResource getFileResource()
        {
            return fileResource;
        }

        private boolean isSelected()
        {
            return selected;
        }

        private String hash()
        {
            return fileResource.hash();
        }

        private String name()
        {
            return fileResource.safeFileName();
        }

        private String user()
        {
            return fileResource.getUploaderCallsign();
        }

        private long size()
        {
            return fileResource.size();
        }

        private Instant instant()
        {
            return fileResource.submissionTime();
        }
    }
}
