package gov.takx.api.routes;

import gov.takx.api.shapes.IRoute;
import jakarta.annotation.Nonnull;

/**
 * Contract for a listener that wants to receive notifications when routes are created or removed.
 *
 * @since 3.4
 */
public interface IRouteListener
{
    /**
     * Called when a new route has been created.
     */
    void onRouteAdded(@Nonnull IRoute route);

    /**
     * Called when a route is deleted.
     */
    void onRouteRemoved(@Nonnull IRoute route);
}
