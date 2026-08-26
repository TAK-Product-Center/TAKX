package gov.takx.api.event.tak;

import gov.nasa.worldwind.View;

/**
 * Event signaling that the WW View object is valid and synchronized with the TAK scene.
 *
 * @since 3.4
 */
public record WWViewInitialized(View view)
{
}
