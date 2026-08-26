package gov.takx.core.explorer.common;

import gov.takx.api.explorer.IUserExplorableGroup;
import gov.takx.commons.swing.SimpleAction;
import gov.takx.core.explorer.controller.IExplorerController;
import gov.takx.core.view.gui.explorer.view.EditGroupDialog;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.JPopupMenu;

import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * Base class {@link IUserExplorableGroup}. Implements the {@code persist} and {@code editable} flags.
 *
 * @since 5.8
 */
public abstract class AUserExplorableGroup extends ControllerExplorableGroup implements IUserExplorableGroup
{
    private boolean persist = true;
    private boolean editable = true;

    /**
     * Construct a new, empty group with the given parameters.
     *
     * @param controller the Controller that owns this group
     * @param displayName the display name of the group
     * @param uniqueKey the unique key
     * @param parentPath the parent path list
     * @param iconImage the icon for the group, or {@code null} for no icon
     */
    protected AUserExplorableGroup(@Nonnull IExplorerController controller, @Nonnull String displayName, @Nonnull String uniqueKey, @Nullable List<String> parentPath, @Nullable BufferedImage iconImage)
    {
        super(controller, displayName, uniqueKey, parentPath, iconImage);
    }

    @Override
    public boolean isPersist()
    {
        return persist;
    }

    @Override
    public void setPersist(boolean persist)
    {
        this.persist = persist;
    }

    @Override
    public boolean isEditable()
    {
        return editable;
    }

    @Override
    public void setEditable(boolean editable)
    {
        this.editable = editable;
    }

    @Override
    public JPopupMenu createPopupMenu(@Nonnull MouseEvent mouseEvent)
    {
        var popupMenu = new JPopupMenu();
        if (editable) popupMenu.add(new SimpleAction("Edit", UIResources.EDIT_ICON, actionEvent -> new EditGroupDialog(controller, this).setVisible(true)));
        if (supportsDeletion) popupMenu.add(new DeleteExplorableAction(this));
        return popupMenu;
    }
}
