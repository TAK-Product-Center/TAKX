package gov.takx.core.kml.event;

import gov.takx.core.kml.KmlModel;
import jakarta.annotation.Nonnull;

/**
 * Event fired when a KML model (representing an imported KML/KMZ file) is added.
 * @since 5.5
 */
public record KmlModelAdded(@Nonnull KmlModel kmlModel)
{
}
