package gov.takx.api.event.tak;

import com.atakmap.map.MapRenderer3;
import gov.tak.api.annotation.DeprecatedApi;

/**
 * Event signaling that a {@link MapRenderer3} has been created and is now active. The {@code oldRenderer} field
 * is always null.
 *
 * @deprecated to be removed without replacement
 * @since 3.4.1
 */
@DeprecatedApi(since = "5.6", forRemoval = true, removeAt = "5.9")
@Deprecated(since = "5.6", forRemoval = true)
public record MapRendererInitialized(MapRenderer3 renderer, MapRenderer3 oldRenderer)
{
}
