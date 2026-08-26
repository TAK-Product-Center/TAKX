package gov.takx.core.explorer.common;

import gov.takx.api.explorer.Explorable;
import gov.takx.api.explorer.IExplorableGroup;
import gov.takx.core.explorer.controller.IExplorerController;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.awt.image.BufferedImage;
import java.util.List;

/**
 * Extension of {@link Explorable} that implements {@link IControllerExplorable} and holds a reference to the
 * {@link IExplorerController}
 *
 * @since 5.3
 */
public class ControllerExplorableGroup extends Explorable implements IControllerExplorable, IExplorableGroup
{
    protected final IExplorerController controller;

    /**
     * Construct a new instance with the given parameters.
     *
     * @param controller the Controller that owns this group
     * @param displayName the display name of the group
     * @param uniqueKey the unique key
     * @param parentPath the parent path list
     * @param iconImage the icon for the group, no {@code null} for no icon
     */
    public ControllerExplorableGroup(@Nonnull IExplorerController controller,
                                     @Nonnull String displayName,
                                     @Nonnull String uniqueKey,
                                     @Nullable List<String> parentPath,
                                     @Nullable BufferedImage iconImage)
    {
        super(controller, displayName, uniqueKey, parentPath, iconImage, true);
        this.controller = controller;
        this.supportsVisibility = false;
    }

    @Nonnull
    @Override
    public IExplorerController getController()
    {
        return controller;
    }
}
