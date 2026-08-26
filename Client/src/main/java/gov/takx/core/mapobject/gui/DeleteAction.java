package gov.takx.core.mapobject.gui;

import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.api.mapobject.IShapeMapObject;
import gov.takx.api.persist.IPersistServiceProvider;
import gov.takx.commons.swing.AlwaysOnTopJOptionPane;
import gov.takx.commons.swing.ErrorReportingSwingWorker;
import gov.takx.commons.swing.ImageUtils;
import gov.takx.commons.swing.StatusDialog;
import jakarta.inject.Inject;

import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;

import java.awt.event.ActionEvent;

/**
 * Action (invoked from our menus) to delete a Map Object with user confirmation.
 *
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class DeleteAction extends AbstractAction
{
    private final IMapObject mapObjectToDelete;

    @Inject
    private IMapObjectManager mapObjectManager;

    @Inject
    private IPersistServiceProvider persistServiceProvider;

    public DeleteAction(String name, IMapObject mapObjectToDelete)
    {
        super(name);
        this.mapObjectToDelete = mapObjectToDelete;
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        if (confirmDelete())
        {
            final StatusDialog dialog = new StatusDialog(true, true);

            dialog.setStatusText("Wiping " + mapObjectToDelete.getName());

            SwingWorker<Void, Void> worker = new ErrorReportingSwingWorker<>("Error deleting Map Object", "Error deleting Map Object")
            {
                @Override
                protected Void doInBackground()
                {
                    mapObjectManager.deleteMapObject(mapObjectToDelete);
                    return null;
                }

                /**
                 * Dispose of the status dialog.
                 */
                @Override
                protected void cleanup()
                {
                    persistServiceProvider.cleanupPersistService();
                    dialog.setVisible(false);
                    dialog.dispose();
                }
            };

            worker.execute();
            dialog.setVisible(true);
        }
    }

    /**
     * Prompt the user to confirm they really want to delete this MO.
     *
     * @return {@code true} if the user confirmed the deletion
     */
    private boolean confirmDelete()
    {
        Object message;
        Icon icon = null;

        final String moName = mapObjectToDelete.getName();
        if (!mapObjectToDelete.isDevice())
        {
            // Determine which form of message to show the user.  If it is a shape, and it has listeners, then
            // we will show the user the listeners as the message, otherwise just use a simple string message.

            message = "<html>Deleting " + moName
                    + "<br><br><font color=\"red\">Warning:</font> Are you sure you want to delete this object?</html>";

            if (mapObjectToDelete instanceof IShapeMapObject shapeMapObject)
            {
                final int listenerCount = shapeMapObject.getShapeListenerCount();

                if (listenerCount > 0)
                {
                    message = "<html><font color=\"red\">Warning:</font> Are you sure you want to delete this shape?"
                            + "<br><br>It is being monitored by "
                            + (listenerCount > 1 ? "one plugin." : listenerCount + " plugins.") + "</html>";
                }
            }

            // Determine the icon to use in the dialog
            icon = mapObjectToDelete.getSizedIcon(ImageUtils.DEFAULT_ICON_WIDTH);
        } else
        {
            // Delete and wipe data confirmation dialogue
            message = "<html>Wiping " + moName +
                    "<br><br><font color=\"red\">Warning: This action cannot be undone!</font><br>" +
                    "All of the associated data will be destroyed<br>and removed from the project " +
                    "permanently. This data will not be recoverable. <br>Are you sure you wish to proceed?";
        }

        final Object[] dlgButtons = {"Yes", "No"};

        int result = AlwaysOnTopJOptionPane.showOptionDialog(
                message,
                "Confirm Delete - " + moName,
                JOptionPane.WARNING_MESSAGE,
                JOptionPane.YES_NO_OPTION,
                icon,
                dlgButtons,
                dlgButtons[1]);

        return result == 0; // 0 is "yes"
    }
}
