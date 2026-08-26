package gov.takx.core.explorer.common;

import gov.takx.api.explorer.IManualExplorableGroup;
import gov.takx.core.explorer.controller.IExplorerController;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.awt.image.BufferedImage;
import java.util.Collections;

/**
 * Extension of {@link ControllerExplorableGroup} for {@link IManualExplorableGroup} instances.
 *
 * @since 5.3
 */
public class ManualExplorableGroup extends ControllerExplorableGroup implements IManualExplorableGroup
{
    /**
     * Construct a new instance.
     *
     * @param controller the Controller that owns this group
     * @param displayName the display name of the group
     * @param iconImage the icon for the group
     */
    public ManualExplorableGroup(@Nonnull IExplorerController controller, @Nonnull String displayName, @Nonnull String uniqueKey, @Nullable BufferedImage iconImage)
    {
        super(controller, displayName, uniqueKey, Collections.emptyList(), iconImage);
    }
}
