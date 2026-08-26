package gov.takx.core.kml.event;

import de.micromata.opengis.kml.v_2_2_0.NetworkLink;
import gov.takx.core.kml.KmlModel;
import jakarta.annotation.Nonnull;

/**
 * Event fired when a {@link NetworkLink} has been updated with new content that has been downloaded and parsed.
 *
 * @since 5.5
 */
public record NetworkLinkUpdated(@Nonnull KmlModel kmlModel, @Nonnull NetworkLink networkLink)
{
}
