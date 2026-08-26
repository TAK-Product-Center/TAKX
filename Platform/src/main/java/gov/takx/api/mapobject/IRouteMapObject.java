package gov.takx.api.mapobject;

import gov.takx.api.annotation.NoPluginImplementations;
import gov.takx.api.shapes.IRoute;
import gov.takx.dataservices.shapes.Route;

/**
 * Contract for a map object defining a route.
 *
 * @since 3.0
 */
@NoPluginImplementations
public interface IRouteMapObject extends IRoute, IShapeMapObject<Route>
{
    /**
     * Map Object Type for routes.
     */
    String ROUTE_TYPE = "Route";
}
