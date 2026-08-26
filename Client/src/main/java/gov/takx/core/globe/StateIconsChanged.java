package gov.takx.core.globe;

import gov.takx.api.globe.IGlobeMarkerMapObject;

/**
 * Event fired when some aspect of the state icons for a Map Object has changed.  This includes addition/removal
 * of state icons or properties of any state icon associated with a Map Object.
 * <p/>
 * Note that this event is fired against the {@link IGlobeMarkerMapObject} whose state icons have changed because
 * there are two state icon managers per marker - one for live, and one for playback.
 *
 * @param globeMarkerMapObject Globe Map Object whose state icons have changed
 * @since 3.5 (signature change in 5.7)
 */
public record StateIconsChanged(IGlobeMarkerMapObject globeMarkerMapObject)
{
}
