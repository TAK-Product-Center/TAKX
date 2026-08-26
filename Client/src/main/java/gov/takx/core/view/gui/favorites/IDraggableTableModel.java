package gov.takx.core.view.gui.favorites;

/**
 * Interface for a table model that supports drag and drop of rows.
 *
 * @since 3.4
 */
public interface IDraggableTableModel
{
    /**
     * Move a row from the provided source index to the specified destination index.
     * 
     * @param sourceIndex The source or starting index for the row being moved
     * @param destinationIndex The destination or ending index for the row
     */
    void moveRow(int sourceIndex, int destinationIndex);
}
