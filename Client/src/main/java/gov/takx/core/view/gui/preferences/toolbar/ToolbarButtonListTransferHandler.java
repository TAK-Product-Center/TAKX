package gov.takx.core.view.gui.preferences.toolbar;

import gov.takx.core.toolbar.ToolbarButton;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.TransferHandler;

import java.awt.datatransfer.Transferable;
import java.awt.event.InputEvent;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

/**
 * Transfer handler for drag and drop of buttons from the button list to the toolbar tree or another button list.
 *
 * @since 2.0 (Renamed in 3.4)
 */
public class ToolbarButtonListTransferHandler extends TransferHandler
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final ToolbarConfigurationDialog toolbarConfigurationDialog;

    /**
     * Indicates if this is the source of the drag operation (used to prevent self drops when configured to do so).
     */
    private boolean isDragSource;

    /**
     * Indicates that any export actions have already been completed (used when buttons are moved within the same list,
     * since they must be removed before they are added)
     */
    private boolean isExportCompleted;

    public ToolbarButtonListTransferHandler(@Nonnull ToolbarConfigurationDialog toolbarConfigurationDialog)
    {
        this.toolbarConfigurationDialog = toolbarConfigurationDialog;
    }

    @Override
    public boolean canImport(TransferHandler.TransferSupport info)
    {
        return false;
    }

    @Override
    public int getSourceActions(JComponent component)
    {
        return component instanceof JList ? TransferHandler.COPY : TransferHandler.NONE;
    }

    @Override
    protected Transferable createTransferable(JComponent component)
    {
        // Always export as a list, even if only one button is selected, as the configuration tree transfer handler
        // uses that fact that the buttons are a list to distinguish between buttons from the button list and a button
        // moved within the tree
        JList<?> jList = (JList<?>) component;
        List<?> buttonList = jList.getSelectedValuesList();

        return new ToolbarConfigurationTransferable(ToolbarConfigurationTransferable.COMPONENT_DATA_FLAVOR, buttonList);
    }

    @Override
    public boolean importData(TransferSupport info)
    {
        if (!canImport(info))
        {
            return false;
        }

        boolean imported = false;
        Transferable transfer = info.getTransferable();
        JList.DropLocation dropLocation = (JList.DropLocation) info.getDropLocation();
        int index = dropLocation.getIndex();

        List<ToolbarButton> buttons = getButtonsFromTransferable(transfer);
        if (!buttons.isEmpty())
        {
            JList<ToolbarButton> jList = (JList<ToolbarButton>) info.getComponent();
            DefaultListModel<ToolbarButton> listModel = (DefaultListModel<ToolbarButton>) jList.getModel();

            // If this is a drag/drop from the same list, remove the original buttons first
            if (isDragSource)
            {
                // Since we are removing the old buttons first, it may be necessary to adjust the index, if any of the
                // old buttons were above the new location.
                int originalIndex = index;
                for (ToolbarButton button : buttons)
                {
                    if (listModel.indexOf(button) < originalIndex)
                    {
                        index--;
                    }
                }

                toolbarConfigurationDialog.removeButtonsFromList(jList, listModel, buttons);

                // Indicate that the export action (i.e. button removal) has already been handled so it won't happen twice
                isExportCompleted = true;
            }

            toolbarConfigurationDialog.addButtonsToList(jList, listModel, index, buttons);

            imported = true;
        }

        return imported;
    }

    @Override
    public void exportAsDrag(JComponent comp, InputEvent e, int action)
    {
        // If this component was the source of the drag operation, we may want to disallow the drop.
        isDragSource = true;
        isExportCompleted = false;
        super.exportAsDrag(comp, e, action);
    }

    @Override
    protected void exportDone(JComponent source, Transferable data, int action)
    {
        isDragSource = false;

        // If the export actions have already been completed, no further actions are required (if it was a move, the
        // remove was handled above)
        if (isExportCompleted)
        {
            isExportCompleted = false;
            return;
        }

        // If the drop action was aborted, do nothing
        if (action == NONE)
        {
            return;
        }

        JList jList = (JList) source;
        //noinspection unchecked
        DefaultListModel<ToolbarButton> listModel = (DefaultListModel<ToolbarButton>) jList.getModel();
        List<ToolbarButton> buttons = getButtonsFromTransferable(data);

        if (action == MOVE)
        {
            // Move buttons out of list
            toolbarConfigurationDialog.removeButtonsFromList(jList, listModel, buttons);
        } else
        {
            // No buttons to move, but the selection should be updated to the next button if it exists
            toolbarConfigurationDialog.updateListSelectionAfterRemoval(jList.getMinSelectionIndex() + 1, jList, listModel);
        }
    }

    /**
     * Converts the transferable to a list of buttons.
     *
     * @param transferable The transferable to convert
     * @return The list of buttons (empty if an error occurred)
     */
    private List<ToolbarButton> getButtonsFromTransferable(@Nonnull Transferable transferable)
    {
        List<ToolbarButton> buttons = new ArrayList<>();

        try
        {
            List<?> dataList = null;
            if (transferable.isDataFlavorSupported(ToolbarConfigurationTransferable.COMPONENT_DATA_FLAVOR))
            {
                dataList = (List<?>) transferable.getTransferData(ToolbarConfigurationTransferable.COMPONENT_DATA_FLAVOR);
            }

            if (dataList != null)
            {
                for (Object dataObject : dataList)
                {
                    if (dataObject instanceof ToolbarButton)
                    {
                        buttons.add((ToolbarButton) dataObject);
                    }
                }
            }
        } catch (Exception e)
        {
            logger.error("Error decoding data for drag/drop operation", e);
        }

        return buttons;
    }
}
