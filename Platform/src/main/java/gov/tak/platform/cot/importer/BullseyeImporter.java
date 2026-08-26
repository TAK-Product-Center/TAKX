package gov.tak.platform.cot.importer;

import com.atakmap.coremap.conversions.Angle;
import com.atakmap.coremap.conversions.Span;
import com.atakmap.coremap.maps.coords.NorthReference;
import gov.tak.api.cot.CotTypes;
import gov.tak.api.cot.detail.DetailConstants;
import gov.tak.api.cot.event.CotDetail;
import gov.tak.api.cot.event.CotEvent;
import gov.tak.api.cot.importer.ImportResult;
import gov.tak.api.maps.MapItem;
import gov.tak.api.maps.coords.GeoPoint;
import gov.tak.api.util.StandardAttributeNames;
import gov.tak.platform.cot.exporter.BullseyeExporter;
import gov.takx.api.mapobject.IShapeBullseyeMapObject;
import gov.takx.api.shapes.ShapeAlreadyExistsException;
import gov.takx.commons.utils.ParseUtils;
import gov.takx.dataservices.shapes.Bullseye;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

/**
 * CoT Event Importer that produces Bullseyes.
 * @since 5.5
 */
public class BullseyeImporter extends AShapeImporter
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public BullseyeImporter()
    {
        super(CotTypes.SHAPE_BULLSEYE);
    }

    @Override
    protected ImportResult importMapItem(MapItem existing, CotEvent event)
    {
        if (existing != null && !(existing instanceof IShapeBullseyeMapObject))
        {
            logger.warn("Import failure: expected bullseye map object but was {}", existing.getClass());
            return ImportResult.FAILURE;
        }

        final CotDetail detail = event.getDetail();
        if (detail == null)
        {
            logger.warn("Import failure: detail was null.");
            return ImportResult.FAILURE;
        }

        CotDetail bullseyeDetail = detail.getFirstChildByName(BullseyeExporter.BULLSEYE);
        if (bullseyeDetail == null)
        {
            logger.warn("Import failure; expected bullseye detail but was non-existent");
            return ImportResult.FAILURE;
        }

        String title = bullseyeDetail.getAttribute(DetailConstants.ATTR_TITLE, null);
        boolean pointingEdgeToCenter = Boolean.parseBoolean(bullseyeDetail.getAttribute(BullseyeExporter.ATTR_BULLSEYE_DIRECTION, "false"));
        int ringCount = ParseUtils.parseInt(bullseyeDetail.getAttribute(BullseyeExporter.ATTR_RING_COUNT, "-1"), -1);
        boolean ringsVisible = Boolean.parseBoolean(bullseyeDetail.getAttribute(BullseyeExporter.ATTR_RANGE_RINGS_VISIBLE, "false"));
        boolean isMils = Boolean.parseBoolean(bullseyeDetail.getAttribute(BullseyeExporter.ATTR_IS_MILS, "false"));
        double bullseyeRadius = ParseUtils.parseDouble(bullseyeDetail.getAttribute(BullseyeExporter.ATTR_BULLSEYE_RADIUS, "-1"), Double.NaN);
        double ringRadius = ParseUtils.parseDouble(bullseyeDetail.getAttribute(BullseyeExporter.ATTR_RING_RADIUS, "-1"), Double.NaN);
        NorthReference northReference = NorthReference.findFromAbbrev(bullseyeDetail.getAttribute(BullseyeExporter.ATTR_NORTH_REFERENCE, "T"));
        String bullseyeUid = bullseyeDetail.getAttribute(BullseyeExporter.ATTR_BULLSEYE_UID, null);

        if (ringCount == -1 || Double.isNaN(bullseyeRadius) || Double.isNaN(ringRadius) || bullseyeUid == null)
        {
            StringBuilder sb = new StringBuilder("Import failure: ");
            if (ringCount == -1) sb.append("Ring count must be >1. ");
            if (Double.isNaN(bullseyeRadius)) sb.append("Must include a valid Radius. ");
            if (Double.isNaN(ringRadius)) sb.append("Must include a valid Ring Radius. ");
            if (bullseyeUid == null) sb.append("Must include a valid UID.");
            logger.warn(sb.toString());
            return ImportResult.FAILURE;
        }

        IShapeBullseyeMapObject bullseyeMapObject = (IShapeBullseyeMapObject) existing;
        if (bullseyeMapObject == null)
        {
            final String uid = event.getUID();
            try
            {
                bullseyeMapObject = shapeService.createBullseyeMapObject(new Bullseye(uid, new GeoPoint(event.getPoint()), bullseyeRadius, Span.METER));
            } catch (ShapeAlreadyExistsException e)
            {
                logger.error("Failed to create bullseye, UID={}", uid, e);
                return ImportResult.FAILURE;
            }
        }

        bullseyeMapObject.setRadius(bullseyeRadius, Span.METER);
        bullseyeMapObject.setRingCount(ringCount);
        bullseyeMapObject.setRingRadius(ringRadius, Span.METER);
        bullseyeMapObject.setShowRangeRings(ringsVisible);
        bullseyeMapObject.setBearingUnits(isMils ? Angle.MIL : Angle.DEGREE);
        bullseyeMapObject.setNorthReference(northReference);
        bullseyeMapObject.setPointingEdgeToCenter(pointingEdgeToCenter);
        bullseyeMapObject.setMetaBoolean(StandardAttributeNames.RANGE_BEARING, true);

        return super.importMapItem(bullseyeMapObject, event);
    }
}
