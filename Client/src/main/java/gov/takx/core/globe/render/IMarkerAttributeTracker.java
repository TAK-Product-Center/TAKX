package gov.takx.core.globe.render;

import gov.takx.api.platform.IRaptorShutdownListener;
import gov.takx.api.tracks.ITrackStyleDefinition;
import gov.takx.core.mapdata.MapPoint;
import gov.takx.core.mapdata.MapPointGroup;

/**
 * Contract for MarkerAttributeTracker
 */
public interface IMarkerAttributeTracker extends IRaptorShutdownListener
{
    /**
     * Get the GlobeMarkerAttributes object that manages the rendering attributes for the given map point.
     *
     * @param mapPoint MapPoint to handle
     * @return rendering attributes for the map point
     */
    GlobeMarkerAttributes getMarkerAttributesForMapPoint(MapPoint mapPoint);

    /**
     * Get the GlobeMarkerAttributes object that manages the rendering attributes for the given map point.  The
     * given TSD ids are assumed to be current and allows us to bypass a database query to get them.
     *
     * @param mapPoint      MapPoint to handle
     * @param tsdId         Currently assigned TSD Id
     * @param overrideTsdId Currently assigned override TSD Id
     * @return rendering attributes for the map point
     */
    GlobeMarkerAttributes getMarkerAttributesForMapPoint(MapPoint mapPoint, Integer tsdId, Integer overrideTsdId);

    /**
     * Get the GlobeMarkerAttributes object that manages the rendering attributes for the given map point group.
     *
     * @param mapPointGroup MapPointGroup to handle
     * @param rdmTsd        RDM level TSD if this group represents a set of map points that have an RDM level TSD
     * @return rendering attributes for the map point group
     */
    GlobeMarkerAttributes getMarkerAttributesForMapPointGroup(MapPointGroup mapPointGroup, ITrackStyleDefinition rdmTsd);
}
