package gov.tak.api.cot.exporter;

import gov.tak.api.cot.CoordinatedTime;
import gov.tak.api.cot.CotConstants;
import gov.tak.api.cot.detail.ICotDetailManager;
import gov.tak.api.cot.event.CotDetail;
import gov.tak.api.cot.event.CotEvent;
import gov.tak.api.maps.PointMapItem;
import gov.tak.api.maps.coords.GeoPoint;
import gov.tak.api.util.StandardAttributeNames;
import gov.tak.platform.cot.util.AccessUtils;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.mapobject.IMapObject;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.TimeUnit;

/**
 * Abstract base class for CoT Event exporters.
 *
 * @since 3.0
 */
@PseudoBean
public abstract class ACotEventExporter implements CotEventExporter
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    // One day in seconds, the default stale time for many events (including shapes and routes)
    public static final int ONE_DAY_SECONDS = (int) TimeUnit.DAYS.toSeconds(1);

    @Inject
    protected ICotDetailManager detailManager;

    /**
     * Construct the basic CoT event, filling out the info from the given MapObject (like type, point, etc.).
     * The time/start will be now, and the given {@code staleSeconds} will be added to "now" to create the stale time.
     * <p>
     * "how" will be filled out from the MapObject's attributes, or defaulted to "h-e" if not present.
     * <p>
     * The detail handlers will be run, filling out all the standard sub-detail elements.
     *
     * @param mapObject    Map Object to process
     * @param staleSeconds Number of seconds to add to "now" to produce the stale time
     * @return Initialized CoT Event
     * @see #addDetails(IMapObject, CotEvent)
     */
    @Nonnull
    protected CotEvent createBasicEvent(@Nonnull IMapObject mapObject, int staleSeconds)
    {
        final CotEvent event = new CotEvent();
        AccessUtils.setCotAccess(event, mapObject);

        event.setUID(mapObject.getUid());
        event.setType(mapObject.getCotType());
        event.setPoint(new GeoPoint(mapObject.getLocation()));

        final CoordinatedTime time = new CoordinatedTime();
        event.setTime(time);
        event.setStart(time);
        event.setStale(time.addSeconds(staleSeconds));

        if (mapObject instanceof PointMapItem pointMapItem)
        {
            event.setHow(pointMapItem.getGeoPointMetaData().getMetaData()
                    .getStringAttribute(StandardAttributeNames.HOW, CotConstants.HOW_MACHINE_GENERATED_GARBAGE));
        } else if (mapObject.hasMetaValue(StandardAttributeNames.HOW))
        {
            event.setHow(mapObject.getMetaString(StandardAttributeNames.HOW, ""));
        } else
        {
            // if how is missing, then the conversion to a marker will cause an invalid CotEvent
            logger.debug("missing how type, filling in with h-e");
            event.setHow(CotConstants.HOW_HUMAN_ENTERED);
        }

        // Create the top-level detail element
        event.setDetail(new CotDetail());

        addDetails(mapObject, event);

        return event;
    }

    /**
     * Call the {@link ICotDetailManager} to add appropriate details to the event.
     *
     * @param mapObject The map object to process
     * @param event     The CoT event to add to
     */
    protected void addDetails(@Nonnull IMapObject mapObject, @Nonnull CotEvent event)
    {
        detailManager.addDetails(mapObject, event);
    }
}
