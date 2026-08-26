package gov.takx.api.routes;

import gov.takx.api.shapes.IRoute;
import jakarta.annotation.Nonnull;

/**
 * Functional interface that allows listening for only the {@link #onEditComplete(IRoute, boolean)} call.
 * 
 * @since 5.6
 */
public interface IRouteEditCompleteListener extends IRouteEditListener
{
    /**
     * No-op on this interface.
     */
    @Override
    default void onEditNewRoute(@Nonnull IRoute route)
    {
    }

    /**
     * No-op on this interface.
     */
    @Override
    default void onEditExistingRoute(@Nonnull IRoute route)
    {
    }
}
