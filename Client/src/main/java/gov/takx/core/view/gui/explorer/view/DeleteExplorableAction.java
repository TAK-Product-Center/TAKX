package gov.takx.core.view.gui.explorer.view;

import gov.takx.api.explorer.IExplorable;
import gov.takx.commons.swing.AlwaysOnTopJOptionPane;
import jakarta.annotation.Nonnull;

import javax.swing.AbstractAction;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

import java.awt.event.ActionEvent;
import java.util.List;

/**
 * An action that deletes an {@link IExplorable}.
 *
 * @since 5.3
 */
class DeleteExplorableAction extends AbstractAction
{
    private final List<IExplorable> explorables;
    private final boolean silent;

    /**
     * Construct a new delete action for the specified explorable.
     *
     * @param explorable the explorable to delete
     */
    DeleteExplorableAction(@Nonnull IExplorable explorable)
    {
        super("Delete");
        explorables = List.of(explorable);
        silent = false;
    }

    /**
     * Construct a new action to silently delete the List of explorables.
     *
     * @param explorables the Explorables to delete
     * @since 5.7
     */
    DeleteExplorableAction(@Nonnull List<IExplorable> explorables)
    {
        super("Delete All (" + explorables.size() + ")");
        this.explorables = explorables;
        silent = explorables.size() > 1;
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        if (silent)
        {
            // TODO TAKX-4269 update this dialog to show an actual list of the items to be deleted.
            var message = "<html><center>Are you sure you want to delete <font color=red><b>" + explorables.size() + "</b></font> item(s)<br>" + "<font color=red>This action cannot be undone.</font></center></html>";
            var title = "Confirm Delete";
            if (AlwaysOnTopJOptionPane.showConfirmDialog(JFrame.getFrames()[0], message, title, JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)
            {
                explorables.forEach(IExplorable::deleteSilently);
            }
        } else
        {
            explorables.forEach(IExplorable::requestDelete);
        }
    }
}