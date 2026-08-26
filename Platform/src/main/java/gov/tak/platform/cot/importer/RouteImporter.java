package gov.tak.platform.cot.importer;

import com.google.common.base.Strings;
import gov.tak.api.cot.CotTypes;
import gov.tak.api.cot.detail.DetailConstants;
import gov.tak.api.cot.event.CotDetail;
import gov.tak.api.cot.event.CotEvent;
import gov.tak.api.cot.importer.ImportResult;
import gov.tak.api.cot.importer.MapItemImporter;
import gov.tak.api.maps.MapItem;
import gov.tak.api.maps.coords.GeoPoint;
import gov.tak.api.maps.coords.GeoPointMetaData;
import gov.tak.api.util.AttributeSet;
import gov.tak.api.util.StandardAttributeNames;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.api.mapobject.IRouteMapObject;
import gov.takx.api.mapobject.MapObjectSpec;
import gov.takx.api.shapes.IRoute;
import gov.takx.dataservices.persist.RouteMapObject;
import gov.takx.dataservices.persist.ShapePathMapObject;
import gov.takx.platform.KeyUtils;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static gov.tak.api.util.StandardAttributeNames.CALLSIGN;
import static gov.takx.api.shapes.IRoute.IS_A_WAYPOINT;

/**
 * CoT event importer that produces {@link RouteMapObject}s, including any associated
 * waypoints and a {@link ShapePathMapObject} to represent the route path.
 *
 * @since 3.0
 */
@PseudoBean
public class RouteImporter extends MapItemImporter
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    public static final String TAK_DEFAULT_ENDPOINT_NAME = "VDO";

    @Inject
    private IMapObjectManager mapObjectManager;

    public RouteImporter()
    {
        super(false, CotTypes.ROUTE);
    }

    @Override
    protected ImportResult importMapItem(MapItem existing, CotEvent event)
    {
        if (existing != null && !(existing instanceof IRouteMapObject))
        {
            logger.warn("Import failure: expected route map object but was {}", existing.getClass());
            return ImportResult.FAILURE;
        }

        String uid = event.getUID();
        final GeoPoint location = new GeoPoint(event.getPoint());
        String routeName = event.getCallsign();

        CotDetail detail = event.getDetail();
        if (detail == null)
        {
            logger.warn("Import failure: detail was null.");
            return ImportResult.FAILURE;
        }

        IRouteMapObject routeMapObject = (IRouteMapObject) existing;

        List<GeoPointMetaData> vertices = getVertices(detail);
        int verticesCount = vertices.size();
        if (verticesCount < 2)
        {
            logger.error("Route only has {} points (expecting at least 2)! UID: {}", verticesCount, uid);
            return ImportResult.FAILURE;
        }

        if (routeMapObject == null)
        {
            // Create new route
            MapObjectSpec routeSpec = MapObjectSpec.builder(KeyUtils.TOOL_FAMILY, IRouteMapObject.ROUTE_TYPE, uid)
                    .uid(uid)
                    .location(location)
                    .name(routeName)
                    .build();
            routeMapObject = (IRouteMapObject) mapObjectManager.getOrCreateMapObject(routeSpec);
        } else
        {
            routeMapObject.setLocation(location, false);
            routeMapObject.setName(routeName);
        }
        // The link_attr element is handled by LinkAttributeDetailHandler within CotDetailManager
        cotDetailManager.processDetails(routeMapObject, event);

        routeMapObject.setLatestCotEvent(event);
        checkVertexNames(vertices, routeMapObject.getName(), routeMapObject.getPrefix());
        routeMapObject.setPoints(vertices);

        return ImportResult.SUCCESS;
    }

    /**
     * Removes any callsigns equal to the default values from TAK, allowing them to be managed by TAKX.
     *
     * @param vertices All of the vertices for the route
     * @param routeName The name of the route
     * @param waypointPrefix The prefix for the routes waypoints
     * @since 3.3
     */
    private void checkVertexNames(@Nonnull List<GeoPointMetaData> vertices, @Nonnull String routeName, @Nonnull String waypointPrefix)
    {
        int waypointCount = 0;

        // Clear the default first waypoint name
        AttributeSet firstPointAttributes = vertices.get(0).getMetaData();
        if (firstPointAttributes.getStringAttribute(CALLSIGN, "")
                .equals(routeName + IRoute.START_POINT_SUFFIX))
        {
            firstPointAttributes.removeAttribute(CALLSIGN);
        }
        // Clear the default last waypoint name
        AttributeSet lastPointAttributes = vertices.get(vertices.size() - 1).getMetaData();
        if (lastPointAttributes.getStringAttribute(CALLSIGN, "").equals(TAK_DEFAULT_ENDPOINT_NAME))
        {
            lastPointAttributes.removeAttribute(CALLSIGN);
        }

        for (GeoPointMetaData vertex : vertices)
        {
            AttributeSet vertexAttributes = vertex.getMetaData();

            if (!vertexAttributes.getBooleanAttribute(IS_A_WAYPOINT, false)) continue;
            if (vertexAttributes.getStringAttribute(CALLSIGN, "").equals(waypointPrefix + waypointCount)) vertexAttributes.removeAttribute(CALLSIGN);

            waypointCount++;
        }
    }

    /**
     * Extract the route vertices from the link detail elements.
     *
     * @param detail Top-level detail from the CoT event
     * @return List of route vertices representing the control points and waypoints.
     */
    @Nonnull
    protected List<GeoPointMetaData> getVertices(@Nonnull CotDetail detail)
    {
        List<GeoPointMetaData> vertices = new ArrayList<>();
        for (CotDetail link : detail.getChildrenByName(DetailConstants.LINK))
        {
            GeoPoint geoPoint = GeoPoint.parseGeoPoint(link.getAttribute(DetailConstants.ATTR_POINT));

            GeoPointMetaData metaData = new GeoPointMetaData(geoPoint);
            AttributeSet attributeSet = metaData.getMetaData();
            // In case the UID/type is missing for some reason, assume this is a control point (see ATAK-10368)
            attributeSet.setAttribute(StandardAttributeNames.UID, link.getAttribute(DetailConstants.ATTR_UID, UUID.randomUUID().toString()));

            String type = link.getAttribute(DetailConstants.ATTR_TYPE, CotTypes.CONTROL_POINT);
            attributeSet.setAttribute(IS_A_WAYPOINT, type.equals(CotTypes.WAYPOINT));
            String callsign = link.getAttribute(DetailConstants.ATTR_CALLSIGN);
            if (!Strings.isNullOrEmpty(callsign)) attributeSet.setAttribute(CALLSIGN, callsign);
            if (link.hasAttribute(DetailConstants.ATTR_REMARKS)) attributeSet.setAttribute(StandardAttributeNames.REMARKS, link.getAttribute(DetailConstants.ATTR_REMARKS));

            vertices.add(metaData);
        }

        return vertices;
    }
}
