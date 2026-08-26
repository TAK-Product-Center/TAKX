package gov.takx.core.explorer.common;

import gov.takx.api.explorer.IExplorable;
import gov.takx.api.explorer.IUserExplorableGroup;

/**
 * Interface to indicate that an {@link IExplorable} class cannot be added to a {@link IUserExplorableGroup}. e.g. to exclude alarms from
 * showing up in the list of available explorables when creating manual and rule-based groups.
 *
 * @since 5.3
 */
public interface INonGroupableExplorable extends IExplorable
{
}