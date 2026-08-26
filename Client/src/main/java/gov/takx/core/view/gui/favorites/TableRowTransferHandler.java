package gov.takx.core.view.gui.favorites;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.TransferHandler;

import java.awt.Cursor;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DragSource;
import java.lang.invoke.MethodHandles;

/**
 * Transfer handler for drag and drop of a row in a table.
 * <p/>
 * Note: this class only supports drag/drop of a single selected row, so it is recommended that the associated table is
 * set for single row selection for the best user experience. If drag/drop of multiple rows is desired, this class will
 * need to be modified to change the transferable to contain the start and end indices of the selection and
 * {@link IDraggableTableModel} will need to be updated similarly.
 * <p/>
 * This class and the associated {@link IDraggableTableModel} were modeled after
 * <a href=https://stackoverflow.com/a/4769575>this answer on stack overflow</a>.
 *
 * @since 3.4
 */
public class TableRowTransferHandler extends TransferHandler
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final DataFlavor ROW_INDEX_DATA_FLAVOR = new DataFlavor(RowIndexTransferable.class, "Table Row Index");
    private final JTable table;

    /**
     * @param table The table supported by this transfer handler which uses a {@link IDraggableTableModel}
     * @throws IllegalArgumentException if the table model doesn't support drag and drop
     */
    public TableRowTransferHandler(JTable table)
    {
        this.table = table;
        if (!(table.getModel() instanceof IDraggableTableModel))
        {
            throw new IllegalArgumentException("Table must have a draggable table model; table = " + table);
        }
    }

    @Override
    public boolean canImport(TransferSupport info)
    {
        // Import is supported if a row is being dropped within this table
        final boolean canImport = info.getComponent() == table && info.isDrop();

        // Set the cursor to visually indicate if the move is supported
        table.setCursor(canImport ? DragSource.DefaultMoveDrop : DragSource.DefaultMoveNoDrop);
        return canImport;
    }

    @Override
    public int getSourceActions(JComponent component)
    {
        return TransferHandler.MOVE;
    }

    @Override
    protected Transferable createTransferable(JComponent component)
    {
        if (component != table) return null;

        // Only support drag/drop of a single selected row
        if (table.getSelectedRowCount() != 1) return null;

        final int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) return null;

        return new RowIndexTransferable(selectedRow);
    }

    @Override
    public boolean importData(TransferSupport info)
    {
        if (info.getComponent() != table) return false;
        table.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));

        final JTable.DropLocation dropLocation = (JTable.DropLocation) info.getDropLocation();
        int destinationRowIndex = dropLocation.getRow();

        try
        {
            int sourceRowIndex = (int) info.getTransferable().getTransferData(ROW_INDEX_DATA_FLAVOR);
            if (sourceRowIndex != -1 && sourceRowIndex != destinationRowIndex)
            {
                ((IDraggableTableModel) table.getModel()).moveRow(sourceRowIndex, destinationRowIndex);

                // If the destination index is below the original index of the row, subtract one to account for removing
                // the row at the original location
                if (destinationRowIndex > sourceRowIndex) destinationRowIndex--;
                table.getSelectionModel().setSelectionInterval(destinationRowIndex, destinationRowIndex);
                return true;
            }
        } catch (Exception e)
        {
            logger.warn("Error transferring table row: {}", info.getTransferable(), e);
        }

        return false;
    }

    @Override
    protected void exportDone(JComponent source, Transferable data, int action)
    {
        if ((action == TransferHandler.MOVE) || (action == TransferHandler.NONE))
        {
            table.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }
    }

    /**
     * Transferable for drag and drop of a given table row.
     */
    private record RowIndexTransferable(int rowIndex) implements Transferable
    {
        @Override
        public DataFlavor[] getTransferDataFlavors()
        {
            return new DataFlavor[]{ROW_INDEX_DATA_FLAVOR};
        }

        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor)
        {
            return flavor.equals(ROW_INDEX_DATA_FLAVOR);
        }

        @Override
        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException
        {
            if (!flavor.equals(ROW_INDEX_DATA_FLAVOR)) throw new UnsupportedFlavorException(flavor);
            return rowIndex;
        }
    }
}
