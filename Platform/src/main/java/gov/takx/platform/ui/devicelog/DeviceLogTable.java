package gov.takx.platform.ui.devicelog;

import org.jdesktop.swingx.JXTable;

public class DeviceLogTable extends JXTable
{
    public DeviceLogTable()
    {
        super();
        getTableHeader().setAutoscrolls(true);

        // Now that AStandardDialog calls pack after a dialog is visible, the size of the log table is being taken into
        // account when sizing the device log dialog. If we don't specify how many columns we want visible,
        // ColumnFactory.getPreferredScrollableViewportWidth will assume we want all of the columns visible and return
        // a preferred size equal to the summed width of all the columns. By saying we just want 1 column visible, it
        // uses the first column to determine the preferred size of that table, which will be smaller than the other
        // components in the dialog, allowing them to determine the dialog size. This does *not* mean only one column
        // will be visible; this is only used for initially sizing the table. Once the dialog size has been determined,
        // it will show as many columns as it can (right now ~7.5).
        setVisibleColumnCount(1);
    }
}
