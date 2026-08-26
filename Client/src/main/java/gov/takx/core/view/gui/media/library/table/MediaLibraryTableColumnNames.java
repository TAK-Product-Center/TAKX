package gov.takx.core.view.gui.media.library.table;

/**
 * Names of the columns used in the Media panel's library table
 *
 * @see MediaLibraryTableModel
 * @see MediaLibraryTableObject
 * @since 2.37
 */
public enum MediaLibraryTableColumnNames
{
    Type("Type", String.class),
    Name("Name", String.class),
    PathURL("Path/URL", String.class),
    CreationTime("Creation Time", String.class),
    DurationSize("Duration/Size", String.class),
    Timeline("Timeline", Boolean.class),
    Color("Color", java.awt.Color.class);

    private static final MediaLibraryTableColumnNames[] columnNames = values();
    private final String columnName;
    private final Class<?> columnClass;

    MediaLibraryTableColumnNames(String columnName, Class<?> columnClass)
    {
        this.columnName = columnName;
        this.columnClass = columnClass;
    }

    @Override
    public String toString()
    {
        return columnName;
    }

    public static MediaLibraryTableColumnNames getColumnNameFromIndex(int columnIndex)
    {
        return columnNames[columnIndex];
    }

    public static Class<?> getColumnClassFromIndex(int columnIndex)
    {
        return columnNames[columnIndex].columnClass;
    }
}
