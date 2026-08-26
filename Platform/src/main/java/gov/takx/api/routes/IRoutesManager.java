package gov.takx.api.routes;

import gov.takx.api.annotation.InjectableService;
import gov.takx.api.annotation.NoPluginImplementations;
import gov.takx.api.mapobject.IRouteMapObject;
import gov.takx.api.navigation.IRouteNavigationManager;
import gov.takx.api.shapes.IRoute;
import gov.takx.dataservices.shapes.Route;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;

/**
 * Contract for the Routes manager, responsible for all {@link IRoute} instances.
 *
 * @since 3.4
 */
@InjectableService
@NoPluginImplementations
public interface IRoutesManager
{
    /**
     * @return the route with the given key, or {@code null} if not found or the identified MapObject is not a route.
     */
    @Nullable
    IRoute findRouteByKey(@Nonnull String key);

    /**
     * @return List of all existing routes
     */
    @Nonnull
    List<IRoute> getRoutes();

    /**
     * Starts the creation of a new route, using default route property values.
     *
     * @see #startNewRouteEditing(IRoute)
     */
    default void startNewRouteEditing()
    {
        startNewRouteEditing(new Route());
    }

    /**
     * Starts the creation of a new route, using the given {@code routeProperties} to configure the new route when it is
     * actually created by the user.
     * Note, only the following properties are copied over:
     * <ul>
     *     <li>{@link IRoute#setName name}</li>
     *     <li>{@link IRoute#setPrefix prefix}</li>
     *     <li>{@link IRoute#setRemarks remarks}</li>
     *     <li>{@link IRoute#setDirection RouteDirection}</li>
     *     <li>{@link IRoute#setMethod RouteMethod}</li>
     *     <li>{@link IRoute#setRouteType RouteType}</li>
     *     <li>{@link IRoute#setRouteOrder RouteOrder}</li>
     *     <li>{@link IRoute#setLineColor lineColor}</li>
     *     <li>{@link IRoute#setLineWidth lineWidth}</li>
     * </ul>
     * Notification of the new route being created by the user can be obtained via {@link IRouteEditListener#onEditComplete}
     * or {@link IRouteListener#onRouteAdded}.
     * <p/>
     * If the user cancels the creation of the route, {@link IRouteEditListener#onEditComplete(IRoute, boolean)}
     * will be called with a {@code canceled} value of {@code true}.
     * @see #addRouteListener
     * @see #addRouteEditListener
     */
    void startNewRouteEditing(IRoute routeProperties);

    /**
     * Starts editing for an existing {@link IRoute}, which should be editable.
     *
     * @throws IllegalStateException if the provided route is not currently editable
     */
    void startRouteEditing(@Nonnull IRoute route);

    /**
     * Stops route creation/editing by informing the editor to cancel, deleting any route modifications in progress.
     */
    void cancelRouteEditing();

    /**
     * Finishes route creation/editing.
     */
    void finishRouteEditing();

    /**
     * @return The current route being edited, {@code null} if none
     * @see #cancelRouteEditing() Discard changes.
     * @see #finishRouteEditing() Save changes.
     */
    @Nullable
    IRoute getCurrentlyEditingRoute();

    /**
     * Add a route to the system, creating a {@link IRouteMapObject} from the given route model.
     *
     * @param route Route model to add
     * @return The created Map Object
     * @throws IllegalArgumentException If the route
     * <ul>
     *     <li>already has a unique key, see {@link IRoute#getKey()}, or</li>
     *     <li>is already associated with a Map Object, see {@link IRoute#getShapeMapObject()}, or</li>
     *     <li>doesn't have at least 2 control points, see {@link IRoute#getControlPoints()}</li>
     * </ul>
     */
    @Nonnull
    IRouteMapObject addRoute(@Nonnull IRoute route);

    /**
     * Deletes the route map object with the associated key, if it is a route.
     *
     * @param routeKey The {@link IRoute#getKey()} of the object to delete.
     * @throws IllegalArgumentException If no MapObject has the given key
     * @throws IllegalArgumentException If the given key identifies a Map Object that is not a route
     */
    void deleteRoute(@Nonnull String routeKey) throws IllegalArgumentException;

    /**
     * Deletes the route map object with the key of the given route.
     *
     * @param route The route instance to remove from the map.
     */
    default void deleteRoute(@Nonnull IRoute route)
    {
        deleteRoute(route.getKey());
    }

    /**
     * @param listener Listener to receive updates on creation/removal of routes.
     */
    void addRouteListener(@Nonnull IRouteListener listener);

    /**
     * @param listener Listener to remove from receiving route creation/removal updates.
     */
    void removeRouteListener(@Nonnull IRouteListener listener);

    /**
     * @param listener Listener to receive route edit updates.
     */
    void addRouteEditListener(@Nonnull IRouteEditListener listener);

    /**
     * @param listener Listener to remove from receiving route edit updates.
     */
    void removeRouteEditListener(@Nonnull IRouteEditListener listener);

    /**
     * Requests that the {@link IRouteNavigationManager} begins navigating to the provided route.
     *
     * @param route The route to navigate to.
     */
    void navigateRoute(@Nonnull IRoute route);
}
