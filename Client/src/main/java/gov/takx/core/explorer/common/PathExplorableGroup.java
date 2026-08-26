package gov.takx.core.explorer.common;

import gov.takx.api.explorer.IExplorable;
import gov.takx.core.explorer.controller.IExplorerController;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.UUID;

/**
 * A basic implementation of {@link IPathExplorableGroup}.
 *
 * @since 5.3
 */
public class PathExplorableGroup extends ControllerExplorableGroup implements IPathExplorableGroup
{
    /**
     * Construct a new instance with the given parameters.
     *
     * @param controller the owning controller
     * @param parent the parent group
     * @param name the name of the new path
     */
    public PathExplorableGroup(@Nonnull IExplorerController controller, @Nullable IExplorable parent, @Nonnull String name)
    {
        this(controller, parent, name, UUID.randomUUID().toString());
    }

    /**
     * Construct a new instance with the given parameters.
     *
     * @param controller the owning controller
     * @param parent the parent group
     * @param name the name of the new path
     * @param uniqueKey the unique key
     */
    public PathExplorableGroup(@Nonnull IExplorerController controller, @Nullable IExplorable parent, @Nonnull String name, @Nonnull String uniqueKey)
    {
        super(controller, name, uniqueKey, null, null);
        if (parent != null) setParent(parent);
        supportsDeletion = false;
    }

    /**
     * {@inheritDoc}
     * <br>
     * For right now, paths can only exist under their primary parent, so groups cannot be added if that already exists.
     */
    @Override
    public boolean addGroup(@Nonnull IExplorable group)
    {
        return getGroups().isEmpty() && super.addGroup(group);
    }
}
