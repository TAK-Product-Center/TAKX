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
import gov.takx.api.mapobject.IShapeArrowMapObject;
import gov.takx.api.shapes.ShapeAlreadyExistsException;
import gov.takx.dataservices.shapes.Arrow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

import static gov.tak.api.cot.CotParseUtils.getDetailValue;
import static gov.tak.api.cot.CotParseUtils.getDetailValueAsDouble;

/**
 * CoT Event Importer that produces arrow shapes.
 *
 * @since 3.0
 */
public class ArrowImporter extends AShapeImporter
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static final String DISTANCE_LOCK_AVAILABLE = "distanceLockAvailable";
    public static final String ANCHOR_AVAILABLE = "anchorAvailable";
    public static final String RADIUS_AVAILABLE = "radiusAvailable";

    public ArrowImporter()
    {
        super(CotTypes.SHAPE_ARROW);
    }

    @Override
    protected ImportResult importMapItem(MapItem existing, CotEvent event)
    {
        if (existing != null && !(existing instanceof IShapeArrowMapObject))
        {
            logger.warn("Import failure: expected arrow map object but was {}", existing.getClass());
            return ImportResult.FAILURE;
        }

        final CotDetail detail = event.getDetail();
        if (detail == null)
        {
            logger.warn("Import failure: detail was null.");
            return ImportResult.FAILURE;
        }

        double range = getDetailValueAsDouble(event, DetailConstants.RANGE, Double.NaN); // Always in meters
        double bearing = getDetailValueAsDouble(event, DetailConstants.BEARING, Double.NaN); // Always degrees True

        if (Double.isNaN(range) || Double.isNaN(bearing))
        {
            logger.warn("Import failure: arrow must include range and bearing.");
            return ImportResult.FAILURE;
        }

        double inclination = getDetailValueAsDouble(event, DetailConstants.INCLINATION, Double.NaN);

        String startAnchorUid = getDetailValue(event, DetailConstants.ANCHOR_UID, null);
        String rangeAnchorUid = getDetailValue(event, DetailConstants.RANGE_UID, null);

        IMapObject startAnchor = startAnchorUid == null ? null : mapObjectPersist.findMapObjectByUid(startAnchorUid);
        IMapObject rangeAnchor = rangeAnchorUid == null ? null : mapObjectPersist.findMapObjectByUid(rangeAnchorUid);

        // Check if we should attempt to re-import this later in case one of the anchors couldn't be found
        final boolean missingStartAnchor = startAnchorUid != null && startAnchor == null;
        final boolean missingRangeAnchor = rangeAnchorUid != null && rangeAnchor == null;
        if (missingStartAnchor || missingRangeAnchor)
        {
            logger.debug("DEFER Arrow {}, start UID {}, range UID {}", event.getUID(), startAnchorUid, rangeAnchorUid);
            return ImportResult.DEFERRED;
        }

        final GeoPoint startingPoint = new GeoPoint(event.getPoint());

        IShapeArrowMapObject arrowMapObject = (IShapeArrowMapObject) existing;
        if (arrowMapObject == null)
        {
            final String uid = event.getUID();
            try
            {
                arrowMapObject = shapeService.createArrowMapObject(new Arrow(uid, startingPoint, range, bearing));
            } catch (ShapeAlreadyExistsException e)
            {
                logger.error("Failed to create arrow, UID={}", uid, e);
                return ImportResult.FAILURE;
            }
        } else
        {
            arrowMapObject.setLocation(startingPoint);
            arrowMapObject.setStartingPoint(startingPoint);
            arrowMapObject.setRange(range);
            arrowMapObject.setBearing(bearing);
        }

        // TODO KMB: I'm not sure what to do with the inclination... in ATAK it is only used for calculating the end
        //  point, but none of the WW calculations support it (at least as far as I can tell). I am storing it as a
        //  meta value so it can be exported correctly even though we don't use it. We can revisit this when we switch
        //  to the TAK map engine.
        if (!Double.isNaN(inclination)) arrowMapObject.setMetaDouble(StandardAttributeNames.INCLINATION, inclination);

        if (startAnchor != null)
        {
            arrowMapObject.setStartAnchor(startAnchor);
            arrowMapObject.setMetaBoolean(ANCHOR_AVAILABLE, true);
        }

        if (rangeAnchor != null)
        {
            arrowMapObject.setRangeAnchor(rangeAnchor);
            arrowMapObject.setMetaBoolean(RADIUS_AVAILABLE, true);
        }

        arrowMapObject.setMetaBoolean(DISTANCE_LOCK_AVAILABLE, startAnchor == null || rangeAnchor == null);

        return super.importMapItem(arrowMapObject, event);
    }
}
