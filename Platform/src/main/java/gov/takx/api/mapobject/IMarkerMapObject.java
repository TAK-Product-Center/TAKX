package gov.takx.api.mapobject;

import gov.tak.api.maps.Marker;

/**
 * Contract for Marker Map Objects.
 *
 * @since 3.0
 */
public interface IMarkerMapObject extends Marker, IPreviewProvider
{
    /**
     * MapObject Family for CoT markers.
     */
    String MARKER_FAMILY = "gov.tak.cot";

    /**
     * MapObject Type for CoT markers.
     */
    String MARKER_TYPE = "Marker";
}
