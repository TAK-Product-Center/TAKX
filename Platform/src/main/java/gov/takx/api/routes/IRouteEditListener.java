package gov.takx.api.routes;

import gov.takx.api.shapes.IRoute;
import jakarta.annotation.Nonnull;

/**
 * Contract for a listener that wants to receive notifications when various edit operations are performed on Routes.
 *
 * @since 3.4
 */
public interface IRouteEditListener
{
    /**
     * Called when the creation/edit of a new route is initiated.  Note that the properties of this route, such as
     * vertices, are not complete.  They will be configured as the user edits the route on the map.
     *
     * @param route The route being created
     */
    void onEditNewRoute(@Nonnull IRoute route);

    /**
     * Called when editing an existing route is initiated.
     *
     * @param route The route being editing
     */
    void onEditExistingRoute(@Nonnull IRoute route);

    /**
     * Called when the edit operation is completed.  If {@code canceled} is {@code true}, then any modifications
     * to the route have been discarded, or, in the case of a new route, the route will be deleted.
     *
     * @param route The route being edited
     * @param canceled {@code true} if the edit operation was canceled
     */
    void onEditComplete(@Nonnull IRoute route, boolean canceled);
}
