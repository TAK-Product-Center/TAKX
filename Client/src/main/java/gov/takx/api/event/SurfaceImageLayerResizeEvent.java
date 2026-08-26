package gov.takx.api.event;

import gov.nasa.worldwind.geom.LatLon;
import gov.takx.core.globe.layers.SurfaceImageController;

import java.util.List;

/**
 * Provides event notification that a surface image layer is either about to be resized or has been resized.
 *
 * @since 1.20
 */

public class SurfaceImageLayerResizeEvent
{
    /**
     * Identifies that resize stage that the layer has transitioned to.
     */
    public enum EventType
    {
        Armed,
        Disarmed,
        Resized
    }

    private SurfaceImageController controller;
    private EventType eventType;
    private List<LatLon> corners;

    /**
     * Create an immutable surface image layer resize event.
     *
     * @param controller Controller of surface image being resized
     * @param eventType  Type of event
     * @param corners    Either the original corners (eventType == Armed) or resized corners otherwise
     */
    public SurfaceImageLayerResizeEvent(SurfaceImageController controller, EventType eventType, List<LatLon> corners)
    {
        this.controller = controller;
        this.eventType = eventType;
        this.corners = corners;
    }

    public SurfaceImageController getController()
    {
        return controller;
    }

    public EventType getEventType()
    {
        return eventType;
    }

    public List<LatLon> getCorners()
    {
        return corners;
    }

    @Override
    public String toString()
    {
        return "SurfaceImageLayerResizeEvent{" +
                "controller=" + controller +
                ", eventType=" + eventType +
                '}';
    }
}
