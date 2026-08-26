package gov.tak.platform.cot.exporter;

import com.atakmap.coremap.conversions.Angle;
import gov.tak.api.cot.detail.DetailConstants;
import gov.tak.api.cot.event.CotDetail;
import gov.tak.api.cot.event.CotEvent;
import gov.tak.api.cot.exporter.AShapeExporter;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.dataservices.persist.MapObject;
import gov.takx.dataservices.persist.ShapeBullseyeMapObject;
import jakarta.annotation.Nonnull;
import org.jetbrains.annotations.Nullable;

/**
 * CoT Exporter handling {@link gov.takx.api.mapobject.IShapeBullseyeMapObject} objects.
 * @since 5.5
 */
public class BullseyeExporter extends AShapeExporter
{
    public static final String BULLSEYE = "bullseye";
    public static final String ATTR_BULLSEYE_DIRECTION = "edgeToCenter";
    public static final String ATTR_RING_COUNT = "ringNum";
    public static final String ATTR_RING_RADIUS = "ringDist";
    public static final String ATTR_HAS_RANGE_RINGS = "hasRangeRings";
    public static final String ATTR_RANGE_RINGS_VISIBLE = "rangeRingVisible";
    public static final String ATTR_IS_MILS = "mils";
    public static final String ATTR_BULLSEYE_RADIUS = "distance";
    public static final String ATTR_BULLSEYE_UID = "bullseyeUID";
    public static final String ATTR_NORTH_REFERENCE = "bearingRef";

    @Override
    public Class<? extends MapObject> getType()
    {
        return ShapeBullseyeMapObject.class;
    }

    @Nullable
    @Override
    public CotEvent export(@Nonnull IMapObject mapObject)
    {
        final ShapeBullseyeMapObject bullseye = (ShapeBullseyeMapObject) mapObject;
        final CotEvent event = createBasicEvent(mapObject, ONE_DAY_SECONDS);
        final CotDetail bullseyeDetail = event.getDetail().addChild(new CotDetail(BULLSEYE));

        bullseyeDetail.setAttribute(DetailConstants.ATTR_TITLE, bullseye.getName());
        bullseyeDetail.setAttribute(ATTR_BULLSEYE_DIRECTION, Boolean.toString(bullseye.isPointingEdgeToCenter()));
        bullseyeDetail.setAttribute(ATTR_HAS_RANGE_RINGS, "true"); // always true: bullseye.getRingCount() > 0
        bullseyeDetail.setAttribute(ATTR_RING_COUNT, Integer.toString(bullseye.getRingCount()));
        bullseyeDetail.setAttribute(ATTR_RANGE_RINGS_VISIBLE, Boolean.toString(bullseye.isShowRangeRings()));
        bullseyeDetail.setAttribute(ATTR_IS_MILS, Boolean.toString(bullseye.getBearingUnits() == Angle.MIL));
        bullseyeDetail.setAttribute(ATTR_BULLSEYE_RADIUS, Double.toString(bullseye.getRadius()));
        bullseyeDetail.setAttribute(ATTR_RING_RADIUS, Double.toString(bullseye.getRingRadius()));
        bullseyeDetail.setAttribute(ATTR_NORTH_REFERENCE, bullseye.getNorthReference().getAbbrev());
        bullseyeDetail.setAttribute(ATTR_BULLSEYE_UID, bullseye.getUid());

        return event;
    }
}
