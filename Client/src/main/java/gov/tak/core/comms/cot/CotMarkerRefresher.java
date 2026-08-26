package gov.tak.core.comms.cot;

import com.google.common.annotations.VisibleForTesting;
import gov.tak.api.cot.CoordinatedTime;
import gov.tak.api.cot.CotTypes;
import gov.tak.api.icons.TakColor;
import gov.tak.api.maps.Marker;
import gov.tak.api.util.AttributeSet;
import gov.tak.api.util.StandardAttributeNames;
import gov.tak.core.icons.MarkerIconAdapter;
import gov.takx.api.annotation.PlatformUnderTest;
import gov.takx.api.event.StopProjectServices;
import gov.takx.api.event.mapobject.MapObjectAdded;
import gov.takx.api.event.mapobject.MapObjectRemoved;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.api.mapobject.IMarkerMapObject;
import gov.takx.api.platform.ICoreExecutorService;
import gov.takx.commons.swing.GraphicsUtils;
import gov.takx.platform.preferences.PreferenceEntry;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static gov.takx.core.project.ServiceOrdering.STOP_GenericCoreService;

/**
 * Manages the stale state of {@link IMapObject} markers.
 *
 * @since 5.1
 */
@ApplicationScoped
public class CotMarkerRefresher
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final long TEAM_MEMBER_EXTRA_STALE_TIME_MS = TimeUnit.SECONDS.toMillis(10);

    private final IMapObjectManager mapObjectManager;
    private final ICoreExecutorService coreExecutorService;
    private final MarkerIconAdapter markerIconAdapter;
    private final boolean underTest;
    private final Map<String, IMapObject> markers = new ConcurrentHashMap<>();
    private ScheduledFuture<?> staleCheckerFuture;

    @Inject
    CotMarkerRefresher(IMapObjectManager mapObjectManager, ICoreExecutorService coreExecutorService,
                       MarkerIconAdapter markerIconAdapter, @PlatformUnderTest boolean underTest)
    {
        this.mapObjectManager = mapObjectManager;
        this.coreExecutorService = coreExecutorService;
        this.markerIconAdapter = markerIconAdapter;
        this.underTest = underTest;
    }

    @SuppressWarnings("unused")
    private void stopService(@Observes @Priority(STOP_GenericCoreService) StopProjectServices event)
    {
        markers.clear();
    }

    @VisibleForTesting
    void onClientMapObjectAdded(@Observes MapObjectAdded event)
    {
        final IMapObject mapObject = event.getMapObject();
        logger.debug("MO added: {}", mapObject);

        // Only add markers, and no stale handling on markers tagged 'archive'
        if (mapObject.getFamily().equals(IMarkerMapObject.MARKER_FAMILY)
                && mapObject.getType().equals(IMarkerMapObject.MARKER_TYPE)
                && !CotTypes.EMPTY.equals(mapObject.getCotType())
                && !mapObject.hasMetaValue(StandardAttributeNames.ARCHIVE))
        {
            markers.put(mapObject.getUid(), mapObject);

            if (markers.size() == 1) startChecker();
        }
    }

    @VisibleForTesting
    void onClientMapObjectRemoved(@Observes MapObjectRemoved event)
    {
        markers.remove(event.getMapObject().getUid());

        if (markers.isEmpty()) stopChecker();
    }

    /**
     * Checks all tracked markers to see if they should be marked stale or deleted.
     */
    @VisibleForTesting
    void checkMarkers()
    {
        if (!underTest) Thread.currentThread().setName("CotMarkerRefresher");

        // These values are used for all markers
        final long now = CoordinatedTime.currentTimeMillis(); // CoT importers use CoordinatedTime
        final boolean expireEverything = PreferenceEntry.STALE_TIME_EXPIRE_EVERYTHING.getBooleanValue();
        final boolean expireUnknowns = PreferenceEntry.STALE_TIME_EXPIRE_UNKNOWNS.getBooleanValue();

        // Track the markers that have been stale longer than the configured deletion time period
        final List<IMapObject> deleteList = new ArrayList<>();

        // Check each marker and update it as needed, trapping any exceptions
        markers.values().forEach(marker -> checkMarker(marker, deleteList, now, expireEverything, expireUnknowns));

        // Delete markers that have been stale too long
        if (!underTest) for (IMapObject marker : deleteList) mapObjectManager.deleteMapObject(marker);
    }

    /**
     * @param marker The marker to check
     * @param deleteList The list of markers to delete
     * @param now The current time in milliseconds
     * @param expireEverything Flag indicating if all markers are subject to expiration/deletion
     * @param expireUnknowns Flag indicating if unknown markers ({@link CotTypes#PREFIX_UNKNOWN}) should be expired/deleted
     */
    private void checkMarker(@Nonnull IMapObject marker, @Nonnull List<IMapObject> deleteList, long now,
                             boolean expireEverything, boolean expireUnknowns)
    {
        logger.debug("checkMarker: {}", marker);

        try
        {
            final AttributeSet attributeSet = marker.getAttributeSet();

            // If the MO is currently being imported, then we don't want to perform the stale check yet.
            if (attributeSet.getBooleanAttribute(StandardAttributeNames.IMPORTING, false))
            {
                logger.debug("skip check while importing");
                return;
            }

            final boolean teamMember = attributeSet.containsAttribute(StandardAttributeNames.TEAM);
            final boolean stale = attributeSet.getBooleanAttribute(StandardAttributeNames.STALE, false);
            final boolean forceStale = attributeSet.getBooleanAttribute(StandardAttributeNames.FORCE_STALE, false);

            // Use the last update time as the stale time starting point
            long staleTimeMs = attributeSet.getLongAttribute(StandardAttributeNames.LAST_UPDATE_TIME, 0L);

            logger.debug("teamMember {}, stale {}, forceStale {}, lastUpdateTime {} for {}", teamMember, stale, forceStale, staleTimeMs, marker);

            // Add the auto stale duration or a default fixed time period to the last update time to get the stale time
            if (attributeSet.containsAttribute(StandardAttributeNames.AUTO_STALE_DURATION_MS))
            {
                staleTimeMs += attributeSet.getLongAttribute(StandardAttributeNames.AUTO_STALE_DURATION_MS, 0);
            } else
            {
                // From ATAK: preserving legacy behavior for non-team members without autoStaleDuration
                if (!teamMember) return;

                staleTimeMs += TEAM_MEMBER_EXTRA_STALE_TIME_MS;
            }

            // Marked stale, but no longer stale
            if (!forceStale && stale && staleTimeMs > now)
            {
                attributeSet.setAttribute(StandardAttributeNames.STALE, false);
                attributeSet.removeAttribute(StandardAttributeNames.STALE_TIME);

                // Update icon to be in color
                markerIconAdapter.adapt((Marker) marker);
            }
            // Still stale or newly stale
            else if (forceStale || stale || staleTimeMs <= now)
            {
                // Newly stale - update state and icon
                if (!stale)
                {
                    logger.debug("staleTime {} <> now {}", staleTimeMs, now);
                    attributeSet.setAttribute(StandardAttributeNames.STALE, true);
                    attributeSet.setAttribute(StandardAttributeNames.STALE_TIME, now);

                    colorStaleIcon(marker);

                    if (forceStale)
                    {
                        attributeSet.setAttribute(StandardAttributeNames.AUTO_STALE_DURATION_MS, 0L);
                        attributeSet.removeAttribute(StandardAttributeNames.FORCE_STALE);
                    }
                }

                // Check if item has been stale long enough to delete
                if (teamMember)
                {
                    if (expireEverything && isTimeToDelete(now, attributeSet)) deleteList.add(marker);
                } else
                {
                    final String cotType = marker.getCotType();

                    // Shouldn't happen, but verify
                    if (CotTypes.EMPTY.equals(cotType)) return;

                    // If the type isn't an atom, delete as soon as stale
                    if (!cotType.startsWith(CotTypes.PREFIX_ATOM)
                            // If it is an atom, delete if it's been stale long enough
                            || ((expireEverything || (cotType.startsWith(CotTypes.PREFIX_UNKNOWN) && expireUnknowns))
                                    && isTimeToDelete(now, attributeSet)))
                    {
                        deleteList.add(marker);
                    }
                } // end delete check
            } // end stale check
        } catch (Exception e)
        {
            // This may happen repeatedly if there's a bad AttributeSet value. The error will just prevent the marker
            // from staling out, so log at the debug level
            logger.debug("Error checking stale status for marker {}", marker, e);
        }
    }

    /**
     * Colors an icon to be stale by shading it with gray. Note: this does not do any checks on staleness, checks must
     * be done prior to calling.
     * @param marker The marker to shade as stale
     * @since 5.6
     */
    public static void colorStaleIcon(@Nonnull IMapObject marker)
    {
        // Update icon to be gray
        final var color = marker.hasMetaValue(StandardAttributeNames.TEAM)
                ? TakColor.GRAY.getColor()
                : TakColor.DARK_GRAY.getColor(); // mimics ATAK

        marker.setExternalIconImage(GraphicsUtils.colorImage(marker.getIconImage(), color));
        marker.setMetaBoolean(StandardAttributeNames.ADAPT_MARKER_ICON, true);
    }

    /**
     * Start the task that checks the markers at a fixed rate.
     */
    private void startChecker()
    {
        staleCheckerFuture = coreExecutorService.scheduleAtFixedRate(this::checkMarkers, 0,
                PreferenceEntry.STALE_TIME_CHECK_PERIOD_SEC.getIntegerValue(), TimeUnit.SECONDS);
    }

    /**
     * Stops the task that checks the markers at a fixed rate.
     */
    private void stopChecker()
    {
        if (staleCheckerFuture != null)
        {
            staleCheckerFuture.cancel(true);
            staleCheckerFuture = null;
        }
    }

    /**
     * Checks to see if it's time to delete a marker by calculating how long a marker has been stale and comparing that
     * value to the {@link PreferenceEntry#STALE_TIME_MINUTES_UNTIL_DELETE_STALE} value.
     *
     * @param now The current time
     * @param attributeSet The marker's attributes
     * @return {@code true} if it's time to delete the marker
     */
    private boolean isTimeToDelete(long now, @Nonnull AttributeSet attributeSet)
    {
        final long deleteAfterStaleMillis =
                TimeUnit.MINUTES.toMillis(PreferenceEntry.STALE_TIME_MINUTES_UNTIL_DELETE_STALE.getIntegerValue());

        return (now - attributeSet.getLongAttribute(StandardAttributeNames.STALE_TIME, 0) >= deleteAfterStaleMillis);
    }
}
