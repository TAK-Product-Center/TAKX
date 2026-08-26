package gov.takx.core.explorer.common;

import gov.takx.api.explorer.IExplorable;
import gov.takx.api.explorer.IMapObjectExplorable;
import gov.takx.core.explorer.controller.ExplorerController;
import gov.takx.core.explorer.controller.IExplorerController;
import gov.takx.core.explorer.controller.RuleGroupController;
import jakarta.annotation.Nonnull;

/**
 * Extension of {@link IExplorable} for implementations primarily owned and controlled by the {@link ExplorerController}.
 * e.g.<br>
 * <ul>
 *     <li>{@link PathExplorableGroup}</li>
 *     <li>{@link ManualExplorableGroup}</li>
 *     <li>{@link RuleGroupController}</li>
 * </ul>
 * This is in contrast to other Explorables like {@link IMapObjectExplorable}
 * which are owned by other components. The reference to the controller allows these instances to correctly handle
 * delete requests which require UI interaction.
 *
 * @since 5.3
 */
public interface IControllerExplorable extends IExplorable
{
    /**
     * @return the controller that owns this explorable
     */
    @Nonnull
    IExplorerController getController();

    @Override
    default void requestDelete()
    {
        getController().removeExplorable(this, true);
    }

    @Override
    default void deleteSilently()
    {
        getController().removeExplorable(this);
    }
}