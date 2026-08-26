package gov.tak.platform.cot.importer;

import gov.tak.api.cot.CoordinatedTime;
import gov.tak.api.cot.CotTypes;
import gov.tak.api.cot.detail.DetailConstants;
import gov.tak.api.cot.event.CotEvent;
import gov.tak.api.cot.importer.ImportResult;
import gov.tak.api.cot.importer.MapItemImporter;
import gov.tak.api.maps.MapItem;
import gov.tak.api.maps.Marker;
import gov.tak.api.maps.coords.GeoPoint;
import gov.tak.api.time.SystemClock;
import gov.tak.api.util.AttributeSet;
import gov.tak.api.util.StandardAttributeNames;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.api.mapobject.IMarkerMapObject;
import gov.takx.api.mapobject.MapObjectOptions;
import gov.takx.api.mapobject.MapObjectSpec;
import gov.takx.commons.utils.ParseUtils;
import gov.takx.commons.utils.StringUtils;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * CoT Event importer that produces Markers.
 *
 * @since 3.0
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class MarkerImporter extends MapItemImporter
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final long ONE_SEC_IN_MS = TimeUnit.SECONDS.toMillis(1);

    @Inject
    private IMapObjectManager mapObjectManager;

    /**
     * Construct on a list of types.
     *
     * @param prefixOnly {@code true} if the type list is to be interpreted as prefixes of types, not discrete types.
     * @param types      List of type codes or type prefixes
     */
    public MarkerImporter(boolean prefixOnly, String... types)
    {
        super(prefixOnly, types);
    }

    @Override
    protected ImportResult importMapItem(MapItem existing, CotEvent event)
    {
        // If we have an existing item, and it isn't a Marker, then ignore it
        if (existing != null && !(existing instanceof Marker)) return ImportResult.IGNORE;

        final AttributeSet attrs = event.getAttributeSet();

        final boolean localImport = isLocalImport(attrs);

        GeoPoint pointBefore = null;
        Marker marker = (Marker) existing;

        if (marker == null)
        {
            marker = createMarker(event);
        } else
        {
            marker.setMetaBoolean(StandardAttributeNames.IMPORTING, true);
            pointBefore = marker.getPoint();

            // Special handling for cases where the __group detail is NOT included in a subsequent message for
            // an existing marker.  We need to remove the TEAM metadata tags in that case.  This will cause the
            // marker's icon to be re-adapted.
            if (event.getDetail().getFirstChildByName(DetailConstants.GROUP) == null)
            {
                marker.setMetaString(StandardAttributeNames.TEAM, null);
                marker.setMetaString(StandardAttributeNames.ATAK_ROLE_TYPE, null);
                marker.setMetaString(StandardAttributeNames.CUSTOM_ROLE, null);
                marker.setMetaString(StandardAttributeNames.CUSTOM_ROLE_ABBREVIATION, null);
            }
        }

        // if this came in on a streaming connection, lets tag the marker as such
        final String serverFromExtra = attrs.getStringAttribute("serverFrom", "");

        if (!localImport && !StringUtils.isEmpty(serverFromExtra))
        {
            marker.setMetaString("serverFrom", serverFromExtra);
        }

        final GeoPoint geoPoint = new GeoPoint(event.getPoint());

        // calculate instant speed, course, and distance based on points received from the network and store
        if (pointBefore != null) updateTrack(marker, pointBefore, geoPoint);

        marker.setPoint(geoPoint);
        marker.setMetaString(StandardAttributeNames.ACCESS, event.getAccess());
        marker.setMetaString(StandardAttributeNames.QOS, event.getQos());
        marker.setMetaString(StandardAttributeNames.OPEX, event.getOpex());

        final String markerType = marker.getCotType();
        final String eventType = event.getType();

        // Update marker type based on event type (ignore file transfer events)
        if (!eventType.startsWith(CotTypes.PREFIX_FILE_TRANSFER) && !eventType.equals(markerType))
        {
            marker.setCotType(eventType);
        }

        if (eventType.startsWith(CotTypes.PREFIX_DETECT) || eventType.startsWith(CotTypes.PREFIX_EMERGENCY))
        {
            marker.setStyle(marker.getStyle() | Marker.STYLE_ALERT_MASK);
        }

        final String how = event.getHow();
        final String oldHow = marker.getMetaString(StandardAttributeNames.HOW, "");

        if (!marker.hasMetaValue(StandardAttributeNames.HOW) || !Objects.equals(oldHow, how))
        {
            marker.setMetaString(StandardAttributeNames.HOW, how);
        }

        if (existing != null) cotDetailManager.processDetails(marker, event);

        // In previous versions of ATAK before 3.12, the uid was used as the title.
        // When improving this workflow dramatically in 3.12, Markers with just a
        // UID no longer have their title set.
        // There are still cases where UUID's are still sent without the callsign set.

        final String title = marker.getTitle();
        final String markerUid = marker.getUid();

        if (StringUtils.isEmpty(title) && !ParseUtils.isUuid(markerUid))
        {
            marker.setTitle(markerUid);
        }

        // Add the marker to the map if isn't already
        // addToGroup(marker); TODO - when we add MapGroup support

        // auto stale duration is 1.5x the diff between start and stale (from ATAK)
        final long autoStaleDuration = event.getStale().millisecondDiff(event.getStart()) * 3 / 2;
        marker.setMetaLong(StandardAttributeNames.AUTO_STALE_DURATION_MS, autoStaleDuration < 0 ? 0L : autoStaleDuration);

        marker.setLatestCotEvent(event);

        // We're done importing
        marker.setMetaBoolean(StandardAttributeNames.IMPORTING, false);
        marker.setMetaLong(StandardAttributeNames.LAST_UPDATE_TIME, CoordinatedTime.currentTimeMillis());

        return ImportResult.SUCCESS;
    }

    /**
     * Calculate instantaneous speed and bearing based on the two given points, and set Attributes:
     * <ul>
     *     <li>{@code est.speed} as a double (meters per second)</li>
     *     <li>{@code est.course} as a double (degrees from North)</li>
     *     <li>{@code est.dist} as a double (meters)</li>
     *     <li>{@code est.time} as a long (millis since epoch)</li>
     * </ul>
     * <p>
     * this is only useful for the instantaneous time between the last two points.
     *
     * @param marker      Marker to process
     * @param pointBefore Previous location
     * @param geoPoint    New location
     */
    private void updateTrack(Marker marker, GeoPoint pointBefore, GeoPoint geoPoint)
    {
        final long now = CoordinatedTime.currentTimeMillis();
        final long lastUpdateTime = marker.getMetaLong(StandardAttributeNames.LAST_UPDATE_TIME, -1);

        if (lastUpdateTime >= 0)
        {
            final double distance = pointBefore.distanceTo(geoPoint);
            final double speed = distance / ((now - lastUpdateTime) / (double) ONE_SEC_IN_MS);
            final double course = pointBefore.bearingTo(geoPoint);

            marker.setMetaLong(StandardAttributeNames.EST_TIME, now);
            marker.setMetaDouble(StandardAttributeNames.EST_SPEED, speed);
            marker.setMetaDouble(StandardAttributeNames.EST_COURSE, course);
            marker.setMetaDouble(StandardAttributeNames.EST_DISTANCE, distance);
        }
    }

    /**
     * Create a new marker given a CoT event.
     *
     * @param event CoT event
     * @return Newly created marker
     */
    private Marker createMarker(CotEvent event)
    {
        if (logger.isDebugEnabled()) logger.debug("creating a new marker for: {}", event.getUID());

        // If this event does NOT have the 'archive' detail, then we create a transient marker.
        final boolean isTransient = event.getDetail().getFirstChildByName(DetailConstants.ARCHIVE) == null;
        final var options = isTransient ? MapObjectOptions.TRANSIENT_MARKER_OPTIONS : MapObjectOptions.STANDARD_RESOURCE_OPTIONS;

        final MapObjectSpec spec = MapObjectSpec.builder(IMarkerMapObject.MARKER_FAMILY, IMarkerMapObject.MARKER_TYPE)
                .uid(event.getUID())
                .cotType(event.getType())
                .cotEvent(event)
                .mapObjectOptions(options)
                .location(new GeoPoint(event.getPoint()))
                .initializer(mo -> {
                    setInitialProperties(event, (IMarkerMapObject) mo);
                    cotDetailManager.processDetails(mo, event);
                })
                .build();

        return (Marker) mapObjectManager.getOrCreateMapObject(spec);
    }

    /**
     * Set the initial properties for a newly created marker so they are properly set when observers/listeners
     * are notified of the new Map Object.
     *
     * @param event The CoT event being processed
     * @param marker The marker to initialize
     * @since 5.5
     */
    private static void setInitialProperties(CotEvent event, IMarkerMapObject marker)
    {
        marker.setMetaBoolean(StandardAttributeNames.IMPORTING, true);

        marker.setMetaLong(StandardAttributeNames.LAST_UPDATE_TIME, CoordinatedTime.currentTimeMillis());

        marker.setStyle(marker.getStyle() | Marker.STYLE_MARQUEE_TITLE_MASK);
        marker.setMetaString(StandardAttributeNames.HOW, event.getHow());
        marker.setMetaString(StandardAttributeNames.ENTRY, "CoT");

        // Set not draggable on import, user can change in settings.
        marker.setAllowDrag(false);
        marker.setSnapIconToSurface(false);
        marker.setMetaBoolean(StandardAttributeNames.MOVABLE, false);
        marker.setMetaBoolean(StandardAttributeNames.REMOVABLE, true);
        marker.setMetaBoolean(StandardAttributeNames.EDITABLE, true);

        // flag a marker as interesting at this time.  this will be used by the offscreen
        // search mechanism to weed out offscreen markers that are close but may no longer be
        // interesting.   New markers are interesting....
        marker.setMetaLong(StandardAttributeNames.OFFSCREEN_INTEREST, SystemClock.elapsedRealtime());
    }
}
