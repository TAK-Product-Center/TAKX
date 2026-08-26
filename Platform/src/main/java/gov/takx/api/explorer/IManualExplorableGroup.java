package gov.takx.api.explorer;

import gov.tak.api.annotation.IncubatingApi;
import gov.takx.api.annotation.NoPluginImplementations;

/**
 * Extension of {@link IUserExplorableGroup} for groups whose children are manually added and removed by the user.
 * 
 * @since 5.3
 */
@IncubatingApi(since = "5.3")
@NoPluginImplementations
public interface IManualExplorableGroup extends IUserExplorableGroup
{
}