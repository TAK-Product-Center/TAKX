package gov.tak.platform.cot.exporter;

import com.atakmap.coremap.maps.coords.NorthReference;
import gov.tak.api.cot.event.CotDetail;
import gov.tak.api.cot.event.CotEvent;
import gov.tak.api.cot.exporter.ACotEventExporter;
import gov.tak.api.util.StandardAttributeNames;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.dataservices.persist.MapObject;
import gov.takx.dataservices.persist.ShapeArrowMapObject;
import gov.takx.platform.preferences.UserPreferenceEntry;
import gov.takx.platform.utils.units.BearingUnits;
import gov.takx.platform.utils.units.RangeUnits;
import gov.takx.platform.utils.units.SpanConversionUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import static gov.tak.api.cot.detail.DetailConstants.ANCHOR_UID;
import static gov.tak.api.cot.detail.DetailConstants.ATTR_VALUE;
import static gov.tak.api.cot.detail.DetailConstants.BEARING;
import static gov.tak.api.cot.detail.DetailConstants.BEARING_UNITS;
import static gov.tak.api.cot.detail.DetailConstants.INCLINATION;
import static gov.tak.api.cot.detail.DetailConstants.NORTH_REF;
import static gov.tak.api.cot.detail.DetailConstants.RANGE;
import static gov.tak.api.cot.detail.DetailConstants.RANGE_UID;
import static gov.tak.api.cot.detail.DetailConstants.RANGE_UNITS;

/**
 * CoT Exporter handling {@link gov.takx.api.mapobject.IShapeArrowMapObject} objects.
 *
 * @since 3.2
 */
public class ArrowExporter extends ACotEventExporter
{
    @Override
    public Class<? extends MapObject> getType()
    {
        return ShapeArrowMapObject.class;
    }

    @Nullable
    @Override
    public CotEvent export(@Nonnull IMapObject mapObject)
    {
        final ShapeArrowMapObject arrowMO = (ShapeArrowMapObject) mapObject;

        final CotEvent event = createBasicEvent(mapObject, ONE_DAY_SECONDS);
        final CotDetail detail = event.getDetail();

        detail.addChild(new CotDetail(RANGE, ATTR_VALUE, Double.toString(arrowMO.getRange())));
        detail.addChild(new CotDetail(BEARING, ATTR_VALUE, Double.toString(arrowMO.getBearing())));

        final double inclination = arrowMO.getMetaDouble(StandardAttributeNames.INCLINATION, Double.NaN);
        if (!Double.isNaN(inclination)) detail.addChild(new CotDetail(INCLINATION, ATTR_VALUE, Double.toString(inclination)));

        final IMapObject startAnchor = arrowMO.getStartAnchor();
        if (startAnchor != null) detail.addChild(new CotDetail(ANCHOR_UID, ATTR_VALUE, startAnchor.getUid()));

        final IMapObject rangeAnchor = arrowMO.getRangeAnchor();
        if (rangeAnchor != null) detail.addChild(new CotDetail(RANGE_UID, ATTR_VALUE, rangeAnchor.getUid()));

        final int spanSystem = SpanConversionUtils.toSpanSystem(UserPreferenceEntry.DISPLAY_RANGE_UNITS.getValueAs(RangeUnits.class));
        detail.addChild(new CotDetail(RANGE_UNITS, ATTR_VALUE, Integer.toString(spanSystem)));
        detail.addChild(new CotDetail(BEARING_UNITS, ATTR_VALUE, Integer.toString(UserPreferenceEntry.DISPLAY_BEARING_UNIT.getValueAs(BearingUnits.class).getAngle().ordinal())));
        detail.addChild(new CotDetail(NORTH_REF, ATTR_VALUE, Integer.toString(UserPreferenceEntry.DISPLAY_NORTH_REFERENCE.getValueAs(NorthReference.class).ordinal())));

        return event;
    }
}
