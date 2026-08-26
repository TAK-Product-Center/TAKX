package gov.takx.core.view.gui.media.library.table;

import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.media.IMedia;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.table.AbstractTableModel;

import java.awt.Color;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Custom table model for the library table used in the media panel.  Elements are either files or streams of audio,
 * images, or videos.
 * <p>
 * Columns are:
 * <ul>
 * <li>Name (String) - Name of the stream or file</li>
 * <li>Path/URL (String) - Local path or URL to the item</li>
 * <li>Type (String) - Type of media.  Options are "audio" "image" or "video"</li>
 * <li>Creation Time (ZonedDateTime) - Date and time that the file or stream was created.  For files, it is the
 * creation date while it is the time the user added the entry for streams.</li>
 * <li>Duration/Size (String) - Only applicable for files.  For audio or video files, string stating the file's
 * duration in hours, minutes, and seconds.  For image files, the file's size in MB.</li>
 * <li>Timeline (Boolean) - When true, the media is displayed on the timeline.  When false, the media is not
 * displayed on the timeline</li>
 * <li>Color (Color) - Color to use for the associated device</li>
 * </ul>
 * <p>
 * Only the timeline and color fields are editable.
 *
 * @since 2.37
 */
public class MediaLibraryTableModel extends AbstractTableModel
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final List<MediaLibraryTableObject> rowList;

    public MediaLibraryTableModel()
    {
        rowList = Collections.synchronizedList(new ArrayList<>());
    }

    @Override
    public Class<?> getColumnClass(int columnIndex)
    {
        return MediaLibraryTableColumnNames.getColumnClassFromIndex(columnIndex);
    }

    @Override
    public int getRowCount()
    {
        return rowList.size();
    }

    @Override
    public String getColumnName(int columnIndex)
    {
        return MediaLibraryTableColumnNames.getColumnNameFromIndex(columnIndex).toString();
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex)
    {
        return ((columnIndex == MediaLibraryTableColumnNames.Timeline.ordinal()) || (columnIndex == MediaLibraryTableColumnNames.Color.ordinal()));
    }

    @Override
    public int getColumnCount()
    {
        return MediaLibraryTableColumnNames.values().length;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex)
    {
        var entry = rowList.get(rowIndex);

        return switch (MediaLibraryTableColumnNames.getColumnNameFromIndex(columnIndex))
        {
            case Name -> entry.getName();
            case PathURL -> entry.getPathURL();
            case Type -> entry.getType();
            case CreationTime -> entry.getCreationTime();
            case DurationSize -> entry.getDurationSize();
            case Timeline -> entry.isOnTimeline();
            case Color -> entry.getDeviceColor();
        };
    }

    @Override
    public synchronized void setValueAt(Object aValue, int rowIndex, int columnIndex)
    {
        final var entry = rowList.get(rowIndex);
        final var columnName = MediaLibraryTableColumnNames.getColumnNameFromIndex(columnIndex);

        switch (columnName)
        {
            case Timeline -> entry.setOnTimeline((Boolean) aValue);
            case Color -> entry.setDeviceColor((Color) aValue);
            default -> logger.error("Media library table assumed that the {} column was not editable.", columnName);
        }

        rowList.set(rowIndex, entry);

        fireTableCellUpdated(rowIndex, columnIndex);
    }

    @InvokeOnEDT
    public void refresh()
    {
        fireTableDataChanged();
    }

    /**
     * Add a media object to the table.
     *
     * @param media Media to add
     * @since 3.0
     */
    public void addMedia(IMedia media)
    {
        rowList.add(new MediaLibraryTableObject(media));

        final int newRowIndex = rowList.size() - 1;
        fireTableRowsInserted(newRowIndex, newRowIndex);
    }

    /**
     * Remove a media object from the table.
     *
     * @param media Media to remove
     * @since 3.0
     */
    public void removeMedia(IMedia media)
    {
        for (int row = 0, sz = rowList.size(); row < sz; row++)
        {
            if (media.equals(rowList.get(row).getMedia()))
            {
                rowList.remove(row);
                fireTableRowsDeleted(row, row);
                break;
            }
        }
    }

    /**
     * Get the object associated with the specified row
     *
     * @param row Index of desired row
     * @return Object representing the displayed row
     * @throws IndexOutOfBoundsException if parameter row exceeds the number of rows contained within the model
     */
    public MediaLibraryTableObject getRowObject(int row)
    {
        return rowList.get(row);
    }

    /**
     * Clear the dataVector. Fire a notification if we actually deleted anything
     */
    public void clear()
    {
        int old = rowList.size();
        if (old > 0)
        {
            rowList.clear();
            fireTableRowsDeleted(0, old - 1);
        }
    }

    /**
     * Get the underlying vector of rows data.
     *
     * @return the vector
     */
    public List<MediaLibraryTableObject> getRowList()
    {
        return Collections.unmodifiableList(rowList);
    }
}
