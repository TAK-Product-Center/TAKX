package gov.takx.core.explorer.common.events;

import gov.takx.api.explorer.IExplorable;

/**
 * Explorable lifecycle event for notifications on explorable addition or removal.
 *
 * @since 1.48 (converted to record in 5.3)
 */
public record ExplorableLifecycleEvent(Object source, IExplorable explorable, boolean added)
{
}
