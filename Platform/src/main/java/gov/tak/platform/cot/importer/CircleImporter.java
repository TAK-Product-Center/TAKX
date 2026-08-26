package gov.tak.platform.cot.importer;

import gov.tak.api.cot.CotTypes;
import gov.tak.api.cot.detail.DetailConstants;
import gov.tak.api.cot.event.CotDetail;
import gov.tak.api.cot.event.CotEvent;
import gov.tak.api.cot.importer.ImportResult;
import gov.tak.api.maps.MapItem;
import gov.tak.api.maps.coords.GeoPoint;
import gov.tak.api.util.StandardAttributeNames;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IShapeCircleMapObject;
import gov.takx.api.shapes.ShapeAlreadyExistsException;
import gov.takx.dataservices.shapes.Circle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

import static gov.tak.api.cot.importer.ImportResult.DEFERRED;
import static gov.tak.api.cot.importer.ImportResult.FAILURE;
import static gov.tak.api.cot.importer.ImportResult.SUCCESS;

/**
 * CoT Event Importer that produces Circular shapes.
 *
 * @since 3.0
 */
public class CircleImporter extends AEllipseImporter
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    final EllipseImporter ellipseImporter = new EllipseImporter();

    public CircleImporter()
    {
        super(CotTypes.SHAPE_CIRCLE, CotTypes.SHAPE_RANGE_CIRCLE);
    }

    @Override
    protected ImportResult importMapItem(MapItem existing, CotEvent event)
    {
        if (existing != null && !(existing instanceof IShapeCircleMapObject))
        {
            logger.warn("Import failure: expected circle map item but was {}", existing.getClass());
            return FAILURE;
        }

        final ExtractedData extractedData = extractEventData(event, true);

        if (extractedData.importResult == FAILURE) return FAILURE;

        if (!extractedData.isCircle)
        {
            return ellipseImporter.importMapItem(existing, event);
        }

        final String uid = event.getUID();
        final GeoPoint location = new GeoPoint(event.getPoint());

        final double radius = extractedData.baseSemiMinorAxis; // Can use either axis since they are the same for circles
        final int ringCount = extractedData.ringCount;

        IShapeCircleMapObject circleMapObject = (IShapeCircleMapObject) existing;
        if (circleMapObject == null) // Need to create new shape
        {
            try
            {
                circleMapObject = shapeService.createCircleMapObject(new Circle(uid, location, radius));
                circleMapObject.setCotType(event.getType()); // May be circle or range & bearing circle
                circleMapObject.setRingCount(ringCount);
            } catch (ShapeAlreadyExistsException e)
            {
                logger.error("Failed to create Circle, UID={}", uid, e);
                return FAILURE;
            }
        } else // Update existing shape
        {
            circleMapObject.setLocation(location);
            circleMapObject.setRadius(radius);
            circleMapObject.setRingCount(ringCount);
        }

        if (CotTypes.SHAPE_RANGE_CIRCLE.equals(event.getType()))
        {
            circleMapObject.setOpacity(0);
            circleMapObject.setMetaBoolean(StandardAttributeNames.RANGE_BEARING, true);
        }

        processStyleLink(extractedData, circleMapObject);

        if (processCenterAnchor(extractedData, circleMapObject) == DEFERRED) return DEFERRED;
        if (processRadiusAnchor(extractedData, circleMapObject) == DEFERRED) return DEFERRED;

        handleBattlePositionHoldingArea(event, circleMapObject);

        return super.importMapItem(circleMapObject, event);
    }

    /**
     * If a link relation for a radius anchor is present, then install it.
     *
     * @param extractedData   Data extracted from CoT event
     * @param circleMapObject Map Object being constructed
     */
    protected ImportResult processRadiusAnchor(ExtractedData extractedData, IShapeCircleMapObject circleMapObject)
    {
        final CotDetail detail = extractedData.linkRelations.get(DetailConstants.LINK_RELATION_RADIUS_ANCHOR);

        if (detail != null)
        {
            final String uid = detail.getAttribute(DetailConstants.ATTR_UID);
            final IMapObject radiusAnchor = mapObjectPersist.findMapObjectByUid(uid);

            if (radiusAnchor != null)
            {
                circleMapObject.setRadiusAnchor(radiusAnchor);
            } else
            {
                logger.warn("Unable to locate radius anchor '{}' for {} - DEFER", uid, circleMapObject.getKey());
                return DEFERRED;
            }
        }
        return SUCCESS;
    }
}
