package gov.tak.platform.cot.detail;

import gov.tak.api.cot.detail.CotDetailHandler;
import gov.tak.api.cot.detail.DetailConstants;
import gov.tak.api.cot.event.CotDetail;
import gov.tak.api.cot.event.CotEvent;
import gov.tak.api.cot.importer.ImportResult;
import gov.tak.api.cot.route.RouteDirection;
import gov.tak.api.cot.route.RouteMethod;
import gov.tak.api.cot.route.RouteOrder;
import gov.tak.api.cot.route.RouteType;
import gov.tak.api.maps.MapItem;
import gov.takx.api.mapobject.IRouteMapObject;

import java.awt.Color;

import static gov.tak.api.cot.detail.DetailConstants.ATTR_COLOR;
import static gov.tak.api.cot.detail.DetailConstants.ATTR_DIRECTION;
import static gov.tak.api.cot.detail.DetailConstants.ATTR_METHOD;
import static gov.tak.api.cot.detail.DetailConstants.ATTR_ORDER;
import static gov.tak.api.cot.detail.DetailConstants.ATTR_PLANNING_METHOD;
import static gov.tak.api.cot.detail.DetailConstants.ATTR_PREFIX;
import static gov.tak.api.cot.detail.DetailConstants.ATTR_ROUTE_TYPE;
import static gov.tak.api.cot.detail.DetailConstants.ATTR_STROKE;
import static gov.tak.api.cot.detail.DetailConstants.ATTR_TYPE;

/**
 * Handler for {@code <link_attr/>} detail tag that is used in CoT routes.
 *
 * @since 3.0
 */
public class LinkAttributeDetailHandler extends CotDetailHandler
{
    private static final int DEFAULT_COLOR_RGB = Color.WHITE.getRGB();
    private static final double DEFAULT_STROKE_WEIGHT = 3.0d;

    protected LinkAttributeDetailHandler()
    {
        super(DetailConstants.LINK_ATTR);
    }

    @Override
    public boolean isSupported(MapItem item, CotEvent event, CotDetail detail)
    {
        return item instanceof IRouteMapObject;
    }

    @Override
    public ImportResult toItemMetadata(MapItem item, CotEvent event, CotDetail detail)
    {
        IRouteMapObject routeMapObject = (IRouteMapObject) item;

        // The attribute 'planningmethod' is no longer used and can be ignored.
        // The attribute 'type' can be derived from 'method', so no need to parse/store it.

        Color color = new Color(parseInt(detail.getAttribute(ATTR_COLOR), DEFAULT_COLOR_RGB), true);
        routeMapObject.setLineColor(color);

        // Technically the schema says this should be an unsigned byte, but WinTAK outputs a double (and internally
        // we use a double), so just go with it.
        String strokeString = detail.getAttribute(ATTR_STROKE);
        double strokeWeight = parseDouble(strokeString, DEFAULT_STROKE_WEIGHT);
        routeMapObject.setLineWidth(strokeWeight);

        RouteDirection direction = RouteDirection.fromText(detail.getAttribute(ATTR_DIRECTION));
        if (direction != null) routeMapObject.setDirection(direction);

        RouteMethod method = RouteMethod.fromText(detail.getAttribute(ATTR_METHOD));
        if (method != null) routeMapObject.setMethod(method);

        RouteOrder routeOrder = RouteOrder.fromText(detail.getAttribute(ATTR_ORDER));
        if (routeOrder != null) routeMapObject.setRouteOrder(routeOrder);

        String prefix = detail.getAttribute(ATTR_PREFIX);
        if (prefix != null) routeMapObject.setPrefix(prefix);

        RouteType routeType = RouteType.fromText(detail.getAttribute(ATTR_ROUTE_TYPE));
        if (routeType != null) routeMapObject.setRouteType(routeType);

        return ImportResult.SUCCESS;
    }

    @Override
    public boolean toCotDetail(MapItem item, CotEvent event, CotDetail detail)
    {
        final IRouteMapObject routeMapObject = (IRouteMapObject) item;

        CotDetail linkAttrDetail = new CotDetail(DetailConstants.LINK_ATTR);
        detail.addChild(linkAttrDetail);

        linkAttrDetail.setAttribute(ATTR_COLOR, Integer.toString(routeMapObject.getLineColor().getRGB()));

        // Per schema and ATAK code, this needs to be an int, so just round the double
        linkAttrDetail.setAttribute(ATTR_STROKE, Long.toString(Math.round(routeMapObject.getLineWidth())));

        RouteDirection direction = routeMapObject.getDirection();
        linkAttrDetail.setAttribute(ATTR_DIRECTION, direction.getDisplayText());

        // planningmethod isn't used anymore, but schema says it is required, so populate from direction which uses the same values
        linkAttrDetail.setAttribute(ATTR_PLANNING_METHOD, direction.getDisplayText());

        RouteMethod method = routeMapObject.getMethod();
        linkAttrDetail.setAttribute(ATTR_METHOD, method.getDisplayText());
        linkAttrDetail.setAttribute(ATTR_TYPE, method.getTransportationType());

        linkAttrDetail.setAttribute(ATTR_ORDER, routeMapObject.getRouteOrder().getDisplayText());
        linkAttrDetail.setAttribute(ATTR_PREFIX, routeMapObject.getPrefix());
        linkAttrDetail.setAttribute(ATTR_ROUTE_TYPE, routeMapObject.getRouteType().getDisplayText());

        return true;
    }
}
