package gov.takx.api.emergency.event;

import gov.takx.api.emergency.EmergencyAlert;
import gov.takx.api.event.PlatformEvent;

/**
 * CDI Event fired when an Emergency alert is added.  Ths occurs when an emergency alert CoT message is received.
 *
 * @param alert The alert that was added
 * @since 5.7
 */
public record EmergencyAlertAdded(EmergencyAlert alert) implements PlatformEvent
{
}
