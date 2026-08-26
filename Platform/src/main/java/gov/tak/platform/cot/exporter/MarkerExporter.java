package gov.tak.platform.cot.exporter;

import gov.tak.api.cot.detail.DetailConstants;
import gov.tak.api.cot.event.CotDetail;
import gov.tak.api.cot.event.CotEvent;
import gov.tak.api.cot.exporter.ACotEventExporter;
import gov.tak.api.maps.Marker;
import gov.tak.api.util.StandardAttributeNames;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IRouteMapObject;
import gov.takx.dataservices.persist.MapObject;
import gov.takx.dataservices.persist.MarkerMapObject;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.concurrent.TimeUnit;

/**
 * CoT Exporter handling {@link MarkerMapObject} objects.
 *
 * @since 3.0
 */
public class MarkerExporter extends ACotEventExporter
{
    // per Josh Sterling as part of the COVID 19 response bump the stale time from 5 minutes to 1 year
    public static final int DEFAULT_STALE_TIME_SECONDS = (int) TimeUnit.DAYS.toSeconds(365);

    @Override
    public Class<? extends MapObject> getType()
    {
        return MarkerMapObject.class;
    }

    @Nullable
    @Override
    public CotEvent export(@Nonnull IMapObject mapObject)
    {
        // See if this marker is part of a Route compound MO, and export as Route if so
        final IMapObject owner = mapObject.getOwner();

        if (owner instanceof IRouteMapObject) return new RouteExporter().export(owner);

        // If this is the "self" location marker, then we use the last SA message generated
        if (mapObject.isSelfMarker()) return mapObject.getLatestCotEvent();

        // Nope, ordinary marker
        final Marker marker = (Marker) mapObject;

        final int overrideStaleSeconds = marker.getMetaInteger(StandardAttributeNames.COT_DEFAULT_STALE_SECONDS, 0);
        final int staleSeconds = overrideStaleSeconds > 0
                ? overrideStaleSeconds
                : DEFAULT_STALE_TIME_SECONDS;

        final CotEvent event = createBasicEvent(mapObject, staleSeconds);

        if (marker.hasMetaValue(StandardAttributeNames.OPEX)) event.setOpex(marker.getMetaString(StandardAttributeNames.OPEX, ""));
        if (marker.hasMetaValue(StandardAttributeNames.QOS)) event.setQos(marker.getMetaString(StandardAttributeNames.QOS, ""));
        if (marker.hasMetaValue(StandardAttributeNames.ACCESS)) event.setAccess(marker.getMetaString(StandardAttributeNames.ACCESS, ""));

        // Now the Marker-specific details

        final CotDetail detail = event.getDetail();

        if (marker.hasMetaValue(StandardAttributeNames.INITIAL_BEARING))
        {
            final String bearing = Double.toString(marker.getMetaDouble(StandardAttributeNames.INITIAL_BEARING, 0.0d));
            detail.addChild(new CotDetail(DetailConstants.INITIAL_BEARING, DetailConstants.ATTR_VALUE, bearing));
        }

        if (marker.hasMetaValue(StandardAttributeNames.READINESS))
        {
            final String readiness = String.valueOf(marker.getMetaBoolean(StandardAttributeNames.READINESS, false));
            detail.addChild(new CotDetail(DetailConstants.STATUS, DetailConstants.ATTR_READINESS, readiness));
        }

        return event;
    }
}
