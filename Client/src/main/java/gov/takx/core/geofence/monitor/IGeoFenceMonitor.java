package gov.takx.core.geofence.monitor;

import gov.tak.api.maps.PointMapItem;
import gov.takx.api.geofence.GeoFence;
import jakarta.annotation.Nonnull;

/**
 * Contract for a {@link GeoFence} monitor, responsible for determining GeoFence boundary crossings for
 * relevant {@link PointMapItem map items}.
 * 
 * @since 5.3
 */
public interface IGeoFenceMonitor
{
    /**
     * @return The UID of the shape defining the GeoFence boundary
     */
    @Nonnull
    String getShapeUid();

    /**
     * @return The extent (bounding circle information) for the GeoFence's boundary
     */
    @Nonnull
    GeoFenceExtent getExtent();

    /**
     * Set the current containment state of the given {@code pointMapItem}.  No alerts will be posted.
     *
     * @param pointMapItem Map item of interest
     */
    void setMapItemState(@Nonnull PointMapItem pointMapItem);

    /**
     * @param pointMapItem Map item of interest
     * @return {@code true} if the type(s) monitored by the {@link GeoFence} match the given {@code pointMapItem}
     * @see GeoFence#monitoredType()
     */
    boolean matchesMonitoredType(@Nonnull PointMapItem pointMapItem);

    /**
     * Process movement of a map item, report any boundary crossings as configured in the {@link GeoFence}.
     *
     * @param pointMapItem Map item of interest
     */
    void mapItemMoved(@Nonnull PointMapItem pointMapItem);

    /**
     * Stop operation, release resources
     */
    void stop();
}
