package gov.tak.platform.cot.exporter;

import gov.tak.api.cot.event.CotDetail;
import gov.tak.api.cot.event.CotEvent;
import gov.tak.api.cot.exporter.AShapeExporter;
import gov.tak.api.maps.coords.GeoPoint;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IShapePathMapObject;
import gov.takx.dataservices.persist.ShapePathMapObject;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

import static gov.tak.api.cot.detail.DetailConstants.ATTR_POINT;
import static gov.tak.api.cot.detail.DetailConstants.LINK;

/**
 * Abstract base for exporters handling polygon/polyline shapes.
 *
 * @since 3.0
 */
@PseudoBean
public abstract class APolyshapeExporter extends AShapeExporter
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * Export polygon/polyline shapes to CoT events
     *
     * @param mapObject map object to process
     */
    @Nullable
    @Override
    public CotEvent export(@Nonnull IMapObject mapObject)
    {
        final CotEvent event = createBasicEvent(mapObject, ONE_DAY_SECONDS);
        final CotDetail detail = event.getDetail();
        final IShapePathMapObject<?> polyshape;

        if (mapObject instanceof ShapePathMapObject) // ShapePolygonMapObject is a subclass of ShapePathMapObject
        {
            try
            {
                polyshape = (IShapePathMapObject<?>) mapObject;
            } catch (UnsupportedOperationException e)
            {
                logger.debug("Ignoring: {}", e.getMessage());
                return null;
            }
        } else
        {
            logger.error("Map object type not a polygon or polyline! ShapeType={} UID={}", mapObject.getType(), event.getUID());
            return null;
        }

        // Add in all the vertices as 'link' details
        for (ILocation vertex : polyshape.getVertices())
        {
            detail.addChild(new CotDetail(LINK, ATTR_POINT, new GeoPoint(vertex).toString()));
        }

        return event;
    }
}
