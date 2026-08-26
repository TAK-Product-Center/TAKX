package gov.takx.api.ui.event;

import gov.takx.api.mapobject.IMapObjectController;

/**
 * Event fired when the tracking state of a map object changes (starts of stops) on a given globe.  This event allows
 * loose UI coupling among components that manage or show these configurations.
 *
 */
public class MapObjectTrackingEvent
{
    /**
     * Enum used to indicate what sort of tracking event this is.
     */
    public enum TrackingState
    {
        STARTED,
        STOPPED
    }

    /**
     * Enum used to indicate which tracking property has changed state.
     */
    public enum TrackingType
    {
        CENTER_ON,
        KEEP_IN_VIEW
    }

    private final int globeId;
    private final IMapObjectController moc;
    private final TrackingState trackingState;
    private final TrackingType trackingType;

    /**
     * Construct a tracking event.
     *
     * @param globeId       The ID of the globe whose configuration changed
     * @param moc           Controller for map object who's tracking state changed
     * @param trackingState Tracking state for map object
     * @param trackingType  Tracking type
     */
    public MapObjectTrackingEvent(int globeId, IMapObjectController moc, TrackingState trackingState, TrackingType trackingType)
    {
        this.globeId = globeId;
        this.moc = moc;
        this.trackingState = trackingState;
        this.trackingType = trackingType;
    }

    /**
     * @return The globe affected
     */
    public int getGlobeId()
    {
        return globeId;
    }

    /**
     * @return Tracking state of the map object
     */
    public TrackingState getTrackingState()
    {
        return trackingState;
    }

    /**
     * @return Tracking type
     */
    public TrackingType getTrackingType()
    {
        return trackingType;
    }

    /**
     * @return Controller for map object who's tracking state has changed
     */
    public IMapObjectController getMapObjectController()
    {
        return moc;
    }

    public String toString()
    {
        return "MapObjectTrackingEvent[globeId=" + globeId + ", trackingState=" + trackingState
                + ", trackingType=" + trackingType + ", mo=" + moc.getMapObject().getKey() + "]";
    }
}
