package gov.takx.api.explorer;

import gov.tak.api.annotation.IncubatingApi;
import gov.takx.api.annotation.NoPluginImplementations;
import jakarta.annotation.Nonnull;

/**
 * Extension of {@link IExplorableGroup} that represents user created groups. This interface is currently
 * necessary to ensure these explorables are grouped together in the explorer tree.
 *
 * @since 5.3
 */
@SuppressWarnings("deprecation")
@IncubatingApi(since = "5.3")
@NoPluginImplementations
public interface IUserExplorableGroup extends IExplorableGroup
{
    /**
     * @param displayName the display name for this explorable group
     * @since 5.6
     */
    void setDisplayName(@Nonnull String displayName);
}