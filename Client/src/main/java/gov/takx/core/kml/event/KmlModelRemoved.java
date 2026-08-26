package gov.takx.core.kml.event;

import gov.takx.core.kml.KmlModel;

/**
 * Event fired when a KML model (representing an imported KML/KMZ file) is removed.
 * @since 5.5
 */
public record KmlModelRemoved(KmlModel kmlModel)
{
}
