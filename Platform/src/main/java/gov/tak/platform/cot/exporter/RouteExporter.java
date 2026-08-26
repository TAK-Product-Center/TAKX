package gov.tak.platform.cot.exporter;

import gov.tak.api.cot.CotTypes;
import gov.tak.api.cot.event.CotDetail;
import gov.tak.api.cot.event.CotEvent;
import gov.tak.api.cot.exporter.ACotEventExporter;
import gov.tak.api.maps.coords.GeoPoint;
import gov.tak.api.maps.coords.GeoPointMetaData;
import gov.tak.api.util.AttributeSet;
import gov.tak.api.util.StandardAttributeNames;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.dataservices.persist.MapObject;
import gov.takx.dataservices.persist.RouteMapObject;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;

import static gov.tak.api.cot.detail.DetailConstants.ATTR_CALLSIGN;
import static gov.tak.api.cot.detail.DetailConstants.ATTR_POINT;
import static gov.tak.api.cot.detail.DetailConstants.ATTR_RELATION;
import static gov.tak.api.cot.detail.DetailConstants.ATTR_REMARKS;
import static gov.tak.api.cot.detail.DetailConstants.ATTR_TYPE;
import static gov.tak.api.cot.detail.DetailConstants.ATTR_UID;
import static gov.tak.api.cot.detail.DetailConstants.LINK;
import static gov.tak.api.util.StandardAttributeNames.CALLSIGN;
import static gov.tak.api.util.StandardAttributeNames.REMARKS;
import static gov.takx.api.shapes.IRoute.IS_A_WAYPOINT;

/**
 * CoT Exporter handling {@link RouteMapObject} objects.
 *
 * @since 3.0
 */
public class RouteExporter extends ACotEventExporter
{
    private static final String DEFAULT_RELATION = "c";

    @Override
    public Class<? extends MapObject> getType()
    {
        return RouteMapObject.class;
    }

    @Nullable
    @Override
    public CotEvent export(@Nonnull IMapObject mapObject)
    {
        final RouteMapObject routeMO = (RouteMapObject) mapObject;

        final CotEvent event = createBasicEvent(routeMO, ONE_DAY_SECONDS);
        final CotDetail detail = event.getDetail();

        final List<GeoPointMetaData> controlPoints = routeMO.getControlPoints();
        for (GeoPointMetaData controlPoint : controlPoints)
        {
            AttributeSet attributes = controlPoint.getMetaData();
            String uid = attributes.getStringAttribute(StandardAttributeNames.UID);
            GeoPoint point = controlPoint.get();

            String callsign = attributes.getStringAttribute(CALLSIGN, "");
            String type = attributes.getBooleanAttribute(IS_A_WAYPOINT, false) ? CotTypes.WAYPOINT : CotTypes.CONTROL_POINT;
            String remarks = attributes.getStringAttribute(REMARKS, "");

            // Per the schema, all the attributes are required, so set them even if they are empty strings
            CotDetail linkDetail = new CotDetail(LINK, ATTR_POINT, point.toString());
            linkDetail.setAttribute(ATTR_UID, uid);
            linkDetail.setAttribute(ATTR_CALLSIGN, callsign);
            linkDetail.setAttribute(ATTR_TYPE, type);
            linkDetail.setAttribute(ATTR_REMARKS, remarks);
            linkDetail.setAttribute(ATTR_RELATION, DEFAULT_RELATION);

            detail.addChild(linkDetail);
        }

        return event;
    }
}
