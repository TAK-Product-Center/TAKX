package gov.takx.api.event.pointdropper;

import gov.takx.api.event.PlatformEvent;

/**
 * An event reporting changes in the Point Dropper's state.
 *
 * @param active {@code true} if the point dropper is active (it's listeners are armed), {@code false} otherwise
 *
 * @since 3.0.1 (moved here and renamed in 5.1)
 */
public record PointDropperActivation(boolean active) implements PlatformEvent
{
}
