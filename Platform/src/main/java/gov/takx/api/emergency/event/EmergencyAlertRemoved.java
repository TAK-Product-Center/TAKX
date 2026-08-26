package gov.takx.api.emergency.event;

import gov.takx.api.emergency.EmergencyAlert;
import gov.takx.api.event.PlatformEvent;

/**
 * CDI Event fired when an Emergency alert is removed.  This happens when a user dismisses the alert, or when an
 * alert cancellation message is received.
 *
 * @param alert The alert that was removed
 * @since 5.7
 */
public record EmergencyAlertRemoved(EmergencyAlert alert) implements PlatformEvent
{
}
