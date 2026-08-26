package gov.takx.core.view.gui.layers.viewmanager;

import com.formdev.flatlaf.ui.FlatTableUI;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.event.MouseInputListener;

import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;

/**
 */
public class LayerTableUI extends FlatTableUI
{
    private final LayerViewManager manager;
    private final String group;
    private IPopupListener popupListener = null;
    private boolean dragEnabled = true;
    private boolean draggingRow = false;
    private int startDragPoint;
    private int dyOffset;

    /**
     * Popup menu listener interface.  showPopup() is called after a right click on a layer.
     */
    public interface IPopupListener
    {
        void showPopup(MouseEvent e);
    }

    public LayerTableUI(LayerViewManager manager, String group)
    {
        this.manager = manager;
        this.group = group;
    }

    /**
     * Enable/disable dragging of layers in the associated layer table.
     */
    public void setDragEnabled(boolean enabled)
    {
        dragEnabled = enabled;
    }

    /**
     * Set the popup click listener.
     *
     * @param listener popup listener.
     */
    public void setPopupListener(IPopupListener listener)
    {
        popupListener = listener;
    }

    @Override
    public void paint(Graphics g, JComponent c)
    {
        super.paint(g, c);

        // Drag painting
        if (dragEnabled && draggingRow)
        {
            g.setColor(table.getParent().getBackground());
            Rectangle cellRect = table.getCellRect(table.getSelectedRow(), 0, false);
            g.copyArea(cellRect.x, cellRect.y, table.getWidth(), table.getRowHeight(), cellRect.x, dyOffset);

            if (dyOffset < 0)
            {
                g.fillRect(cellRect.x, cellRect.y + (table.getRowHeight() + dyOffset), table.getWidth(), (dyOffset * -1));
            } else
            {
                g.fillRect(cellRect.x, cellRect.y, table.getWidth(), dyOffset);
            }
        }
    }

    @Override
    protected MouseInputListener createMouseInputListener()
    {
        return new DragDropRowMouseInputHandler();
    }

    /**
     * Modified mouse input handler to provide the ability to drag layers from one row to another.
     */
    protected class DragDropRowMouseInputHandler extends MouseInputHandler
    {
        // This variable is used to avoid showing the popup at the end of a drag operation using the right button
        private boolean wasDragging = false;
        private boolean wasPopupTrigger = false;

        @Override
        public void mousePressed(MouseEvent e)
        {
            int mouseRow = table.rowAtPoint(e.getPoint());
            int selectedRow = table.getSelectedRow();
            int[] selectedRows = table.getSelectedRows();
            boolean isRightClick = SwingUtilities.isRightMouseButton(e);

            // on Mac OS, the mouse release is not marked "isPopupTrigger", so we must record
            // whether the mouse press was.
            wasPopupTrigger = e.isPopupTrigger();

            boolean selectedRowsContainsMouseRow = false;
            for (int row : selectedRows)
            {
                if (row == mouseRow)
                {
                    selectedRowsContainsMouseRow = true;
                    break;
                }
            }

            // Note that "popup trigger" is never set on a mouse pressed event, so test for right button.
            if (mouseRow != -1 && isRightClick && !selectedRowsContainsMouseRow)
            {
                // Force a right click to "select" the row, just like a left click
                if (mouseRow != selectedRow)
                {
                    table.clearSelection(); // If not right clicking a highlighted item, deselect highlighted item(s)

                    // Without this for loop, any highlighted items in other groups will remain highlighted
                    // CORE-3942
                    final LayerViewPanel layerViewPanel = manager.getPanel();

                    for (String group : layerViewPanel.getOpenGroups())
                    {
                        layerViewPanel.updateGroup(group, true);
                    }

                    table.setRowSelectionInterval(mouseRow, mouseRow);
                }
            }

            // I know that it seems odd to also pass this event to the superclass, but if I don't it
            // royally screws up dragging.  It seems that drag events ALWAYS report button 0 as the
            // return value of getButton(), so I can't detect and ignore a drag operation using the
            // right button.
            // Also needed to select undraggable groups
            super.mousePressed(e);

            if (dragEnabled)
            {
                startDragPoint = (int) e.getPoint().getY();

                wasDragging = false;
            }
        }

        @Override
        public void mouseDragged(MouseEvent e)
        {
            // If drag is not enabled, skip this method.
            if (!dragEnabled)
            {
                super.mouseDragged(e);
                return;
            }

            wasDragging = true;

            int fromRow = table.getSelectedRow();

            // If the start layer index is valid, then if necessary, move the layer to a new index.
            if (fromRow >= 0)
            {
                draggingRow = true;

                int rowHeight = table.getRowHeight();
                int middleOfSelectedRow = (rowHeight * fromRow) + (rowHeight / 2);

                int toRow = -1;
                int yMousePoint = (int) e.getPoint().getY();

                if (yMousePoint < (middleOfSelectedRow - rowHeight))
                {
                    // Move row up
                    toRow = fromRow - 1;
                } else if (yMousePoint > (middleOfSelectedRow + rowHeight))
                {
                    // Move row down
                    toRow = fromRow + 1;
                }

                if (toRow >= 0 && toRow < table.getRowCount())
                {
                    moveRow(fromRow, toRow);
                    table.setRowSelectionInterval(toRow, toRow);
                    startDragPoint = yMousePoint;
                }

                dyOffset = (startDragPoint - yMousePoint) * -1;
                table.repaint();
            }
        }

        @Override
        public void mouseReleased(MouseEvent e)
        {

            boolean isPopupTrigger = wasPopupTrigger || e.isPopupTrigger();

            // Show the popup menu if we didn't just finish a drag operation
            if (!wasDragging && isPopupTrigger && popupListener != null)
            {
                popupListener.showPopup(e);
            }

            // See the note above in mousePressed about why this even needs to be passed on to the super class.
            super.mouseReleased(e);

            draggingRow = false;
            table.repaint();
        }
    }

    /**
     * Move a layer to a new location in its group.
     *
     * @param source index of layer to move.
     * @param destination index of position where layer should be moved.
     */
    protected void moveRow(int source, int destination)
    {
        manager.moveLayer(group, source, destination);
    }
}
