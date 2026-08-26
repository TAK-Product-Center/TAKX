package gov.tak.platform.cot.detail;

import gov.tak.api.cot.detail.CotDetailHandler;
import gov.tak.api.cot.detail.DetailConstants;
import gov.tak.api.cot.event.CotDetail;
import gov.tak.api.cot.event.CotEvent;
import gov.tak.api.cot.importer.ImportResult;
import gov.tak.api.maps.MapItem;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.geofence.GeoFence;
import gov.takx.api.geofence.GeoFenceDetailConstants;
import gov.takx.api.geofence.IGeoFenceManager;
import gov.takx.api.geofence.MonitoredType;
import gov.takx.api.geofence.Trigger;
import gov.takx.api.mapobject.IShapeMapObject;
import gov.takx.commons.utils.ParseUtils;
import gov.takx.commons.utils.StringUtils;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.HashSet;
import java.util.Set;

/**
 * Handle the {@code <__geofence/>} detail element used for shapes.
 *
 * @since 3.5
 */
@PseudoBean
public class GeofenceDetailHandler extends CotDetailHandler
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Inject
    private IGeoFenceManager geoFenceManager;

    public GeofenceDetailHandler()
    {
        super(DetailConstants.GEOFENCE);
    }

    @Override
    public boolean isSupported(MapItem item, CotEvent event, CotDetail detail)
    {
        return item instanceof IShapeMapObject<?>;
    }

    @Override
    public ImportResult toItemMetadata(MapItem item, CotEvent event, CotDetail detail)
    {
        final var shape = (IShapeMapObject<?>) item;

        final boolean tracking = Boolean.parseBoolean(detail.getAttribute(GeoFenceDetailConstants.TRACKING, "true"));
        final boolean monitorElevation = Boolean.parseBoolean(detail.getAttribute(GeoFenceDetailConstants.ELEVATION_MONITORED, "false"));
        final var trigger = Trigger.forName(detail.getAttribute(GeoFenceDetailConstants.TRIGGER, Trigger.Entry.name()));
        final var monitoredType = MonitoredType.forName(detail.getAttribute(GeoFenceDetailConstants.MONITOR, MonitoredType.All.name()));

        final double minElevation = ParseUtils.parseDouble(detail.getAttribute(GeoFenceDetailConstants.ELEVATION_MIN), Double.NaN);
        final double maxElevation = ParseUtils.parseDouble(detail.getAttribute(GeoFenceDetailConstants.ELEVATION_MAX), Double.NaN);

        final Set<String> customUids = new HashSet<>();

        if (monitoredType == MonitoredType.Custom)
        {
            for (int i = 0, childCount = detail.childCount(); i < childCount; i++)
            {
                final var monitorDetail = detail.getChild(i);
                if (monitorDetail != null && GeoFenceDetailConstants.MONITOR.equals(monitorDetail.getElementName()))
                {
                    final String uid = monitorDetail.getAttribute(DetailConstants.ATTR_UID);
                    if (!StringUtils.isEmpty(uid)) customUids.add(uid);
                }
            }
        }

        final GeoFence geoFence = GeoFence.builder()
                .shapeUid(shape.getUid())
                .tracking(tracking)
                .trigger(trigger)
                .monitoredType(monitoredType)
                .monitorElevation(monitorElevation)
                .minElevation(minElevation)
                .maxElevation(maxElevation)
                .customUids(customUids)
                .build();

        try
        {
            geoFenceManager.addOrUpdateGeoFence(geoFence);
            shape.setPublished(true);
        } catch (Exception e)
        {
            logger.warn("Failed to apply geofence to shape '{}'", shape, e);
            return ImportResult.FAILURE;
        }

        logger.debug("Set geoFence on shape {}: {}", shape, geoFence);

        return ImportResult.SUCCESS;
    }

    @Override
    public boolean toCotDetail(MapItem item, CotEvent event, CotDetail root)
    {
        final var shape = (IShapeMapObject<?>) item;
        if (shape.hasGeoFence())
        {
            final var geoFence = shape.requireGeoFence();
            final var monitoredType = geoFence.monitoredType();
            final boolean monitorElevation = geoFence.monitorElevation();

            final var geoFenceDetail = new CotDetail(DetailConstants.GEOFENCE);
            geoFenceDetail.setAttribute(GeoFenceDetailConstants.TRIGGER, geoFence.trigger().name());
            geoFenceDetail.setAttribute(GeoFenceDetailConstants.MONITOR, monitoredType.name());
            geoFenceDetail.setAttribute(GeoFenceDetailConstants.TRACKING, Boolean.toString(geoFence.tracking()));
            geoFenceDetail.setAttribute(GeoFenceDetailConstants.ELEVATION_MONITORED, Boolean.toString(monitorElevation));

            if (monitorElevation)
            {
                geoFenceDetail.setAttribute(GeoFenceDetailConstants.ELEVATION_MIN, Double.toString(geoFence.minElevation()));
                geoFenceDetail.setAttribute(GeoFenceDetailConstants.ELEVATION_MAX, Double.toString(geoFence.maxElevation()));
            }

            if (monitoredType == MonitoredType.Custom)
            {
                geoFence.customUids().forEach(uid -> geoFenceDetail.addChild(new CotDetail(GeoFenceDetailConstants.MONITOR, DetailConstants.ATTR_UID, uid)));
            }

            root.addChild(geoFenceDetail);

            return true;
        }
        return false;
    }
}
