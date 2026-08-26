package gov.takx.core.view.gui.explorer.view;

import gov.takx.api.explorer.IExplorable;
import gov.takx.api.explorer.ITreeNodeComparator;
import gov.takx.api.explorer.IUserExplorableGroup;
import jakarta.annotation.Nonnull;

import javax.swing.Action;

import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.List;

/**
 * Interface for an explorer view.
 *
 * @since 1.48
 */
public interface IExplorerView
{
    /**
     * Set selected explorables in view.
     *
     * @param explorable selected explorables.
     */
    void setSelected(@Nonnull Collection<IExplorable> explorable);

    /**
     * Register a comparator for children of the named node
     *
     * @param key        The unique key of the targeted node
     * @param comparator The comparator used to sort children of the named parent node
     * @since 2.29
     */
    void registerComparator(String key, ITreeNodeComparator comparator);

    /**
     * Show popup menu for an explorable.  Extra actions may be provided to be inserted into the popup before being
     * displayed.
     *
     * @param explorable explorable of interest.
     * @param actions    extra actions to insert into the popup menu.
     * @param event      the originating event.
     */
    void showPopupMenu(@Nonnull IExplorable explorable, @Nonnull List<Action> actions, @Nonnull MouseEvent event);

    /**
     * Show popup menu for a list of actions.
     * TODO DCW TAKX-3282 this is unused. Not sure what it would ever be used for.
     * @param actions actions to insert into the popup menu.
     * @param event   the originating event.
     */
    void showPopupMenu(List<Action> actions, MouseEvent event);

    /**
     * Show the dialog for an explorable.
     *
     * @param explorable explorable of interest.
     */
    void showDialog(@Nonnull IExplorable explorable);

    /**
     * Show a dialog for the specified group.
     *
     * @param group the group to open the dialog for
     * @since 5.3
     */
    void showGroupDialog(@Nonnull IUserExplorableGroup group);

    /**
     * Show an 'add group' dialog where the user can create a name and select a type of group to create.
     *
     * @since 5.3
     */
    void showAddGroupsDialog();

    /**
     * Refresh or repaint the view.
     */
    void refresh();

    /**
     * Shows or hides an explorable in the explorer tree and table.
     *
     * @param explorable the explorable
     * @param show {@code true} to make the explorable visible or {@code false} to hide it
     * @since 5.3
     */
    void setExplorableVisible(@Nonnull IExplorable explorable, boolean show);
}
